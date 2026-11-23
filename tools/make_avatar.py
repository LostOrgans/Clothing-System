"""Generate a realistic parametric human GLB using MPFB 2 (MakeHuman).

Run with Blender 4.2+ after installing MPFB 2 as an extension:
    blender --background --python tools/make_avatar.py

Output: app/src/main/assets/models/avatar.glb
The GLB carries named morph targets; the app generates one slider per target at runtime.
MakeHuman assets are CC0 (commercial use OK). MPFB code is GPL but is not shipped.
"""

import importlib
import sys
from pathlib import Path

import bmesh
import bpy
import numpy as np
from mathutils import Vector


def dynamic_import(absolute_package_str, key):
    """Blender extensions land under an unknown package prefix; find it by suffix."""
    for module_name in sys.modules:
        if module_name.endswith(absolute_package_str):
            module = importlib.import_module(module_name)
            if hasattr(module, key):
                return getattr(module, key)
    raise ValueError(f"No module found ending in {absolute_package_str}")


MACROS = ("gender", "age", "muscle", "weight", "proportions", "height", "cupsize", "firmness")


def enable_mpfb():
    try:
        bpy.ops.preferences.addon_enable(module="bl_ext.user_default.mpfb")
    except RuntimeError as exc:
        if "already enabled" not in str(exc):
            raise
    # MPFB must be registered in sys.modules before dynamic_import can find it.
    if not any(m.endswith("mpfb.services.humanservice") for m in sys.modules):
        import bl_ext.user_default.mpfb.services.humanservice  # noqa: F401
    if not any(m.endswith("mpfb.services.targetservice") for m in sys.modules):
        import bl_ext.user_default.mpfb.services.targetservice  # noqa: F401
    if not any(m.endswith("mpfb.entities.objectproperties") for m in sys.modules):
        import bl_ext.user_default.mpfb.entities.objectproperties  # noqa: F401


def get_services():
    return (
        dynamic_import("mpfb.services.humanservice", "HumanService"),
        dynamic_import("mpfb.services.targetservice", "TargetService"),
        dynamic_import("mpfb.entities.objectproperties", "HumanObjectProperties"),
    )


def evaluated_positions(obj):
    """Read the final deformed vertex positions from the depsgraph."""
    depsgraph = bpy.context.evaluated_depsgraph_get()
    evaluated = obj.evaluated_get(depsgraph)
    mesh = evaluated.to_mesh()
    positions = [tuple(v.co) for v in mesh.vertices]
    evaluated.to_mesh_clear()
    return positions


def set_macro(obj, name, value):
    HumanObjectProperties.set_value(name, value, entity_reference=obj)
    TargetService.reapply_macro_details(obj)


def add_delta_shape_key(obj, name, basis, target_positions):
    """Create a shape key whose full weight produces target_positions."""
    key = obj.shape_key_add(name=name, from_mix=False)
    for index, (bx, by, bz) in enumerate(basis):
        tx, ty, tz = target_positions[index]
        key.data[index].co = (bx + (tx - bx), by + (ty - by), bz + (tz - bz))
    key.value = 0.0
    return key


def build_shape_keys(obj):
    neutral = TargetService.get_default_macro_info_dict()
    for name in MACROS:
        HumanObjectProperties.set_value(name, neutral[name], entity_reference=obj)
    TargetService.reapply_macro_details(obj)
    basis = evaluated_positions(obj)

    for name in MACROS:
        for extreme, value in (("min", 0.0), ("max", 1.0)):
            set_macro(obj, name, value)
            positions = evaluated_positions(obj)
            add_delta_shape_key(obj, f"{name}_{extreme}", basis, positions)
            set_macro(obj, name, neutral[name])

    TargetService.reapply_macro_details(obj)


def strip_helper_geometry(obj):
    """MPFB's basemesh includes fitting helpers (hair, skirt, tights, ...). Keep only the body."""
    body = obj.vertex_groups.get("body")
    if body is None:
        raise RuntimeError("MPFB basemesh has no 'body' vertex group")
    keep = set()
    for vertex in obj.data.vertices:
        if any(group.group == body.index for group in vertex.groups):
            keep.add(vertex.index)

    mesh = bmesh.new()
    mesh.from_mesh(obj.data)
    mesh.verts.ensure_lookup_table()
    remove = [vertex for vertex in mesh.verts if vertex.index not in keep]
    bmesh.ops.delete(mesh, geom=remove, context="VERTS")
    mesh.to_mesh(obj.data)
    mesh.free()
    obj.data.update()
    return len(keep)


REGION_RANGES = {
    "Hair": (1.45, 2.0),
    "Eye_L": (1.5, 2.0),
    "Eye_R": (1.5, 2.0),
    "Nipple_L": (1.15, 1.5),
    "Nipple_R": (1.15, 1.5),
}


def propagate_shape_keys(body, objects):
    """Give every accessory the body's morph targets, using only its own body region."""
    key_blocks = body.data.shape_keys.key_blocks
    basis = np.array([tuple(v.co) for v in key_blocks["Basis"].data], dtype=np.float32)
    deltas = {
        key.name: np.array([tuple(v.co) for v in key.data], dtype=np.float32) - basis
        for key in key_blocks
        if key.name != "Basis"
    }

    for obj in objects:
        if not obj.data.vertices:
            continue
        low, high = REGION_RANGES.get(obj.name, (-10.0, 10.0))
        region = (basis[:, 2] >= low) & (basis[:, 2] <= high)
        region_basis = basis[region]
        if len(region_basis) == 0:
            continue

        obj_basis = np.array([tuple(v.co) for v in obj.data.vertices], dtype=np.float32)

        for name, delta in deltas.items():
            # A single rigid offset keeps small accessories intact while following the body.
            offset = delta[region].mean(axis=0)
            key = obj.shape_key_add(name=name, from_mix=False)
            for index, vertex in enumerate(key.data):
                vertex.co = Vector(obj_basis[index] + offset)
            key.value = 0.0


def make_simple_material(name, color):
    material = bpy.data.materials.new(name)
    material.use_nodes = True
    principled = material.node_tree.nodes.get("Principled BSDF")
    principled.inputs["Base Color"].default_value = (*color, 1.0)
    principled.inputs["Roughness"].default_value = 0.7
    return material


def extract_group_mesh(body, group_name, name, color, min_z=None):
    """Turn an MPFB fitting helper into a real mesh that fits the body exactly."""
    group = body.vertex_groups.get(group_name)
    if group is None:
        return None
    keep = {v.index for v in body.data.vertices if any(g.group == group.index for g in v.groups)}
    if not keep:
        return None

    mesh_data = bmesh.new()
    mesh_data.from_mesh(body.data)
    mesh_data.faces.ensure_lookup_table()
    remove = [
        face for face in mesh_data.faces
        if not all(v.index in keep for v in face.verts)
        or (min_z is not None and face.calc_center_median().z < min_z)
    ]
    bmesh.ops.delete(mesh_data, geom=remove, context="FACES")
    mesh = bpy.data.meshes.new(name)
    mesh_data.to_mesh(mesh)
    mesh_data.free()
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.collection.objects.link(obj)
    mesh.materials.append(make_simple_material(name, color))
    return obj


def add_appearance_parts(body):
    """Small separately-named meshes whose material colour is controlled by the app."""
    parts = []
    for group, name, color, min_z in (
        ("helper-hair", "Hair", (0.10, 0.07, 0.05), 1.48),
        ("helper-l-eye", "Eye_L", (0.25, 0.45, 0.65), None),
        ("helper-r-eye", "Eye_R", (0.25, 0.45, 0.65), None),
    ):
        obj = extract_group_mesh(body, group, name, color, min_z)
        if obj is not None:
            parts.append(obj)

    nipple = make_simple_material("Nipple", (0.55, 0.35, 0.30))
    for side in (-1, 1):
        bpy.ops.mesh.primitive_uv_sphere_add(segments=12, ring_count=8, radius=0.017, location=(side * 0.085, -0.118, 1.31))
        obj = bpy.context.object
        obj.name = f"Nipple_{'L' if side < 0 else 'R'}"
        obj.scale = (1.0, 0.5, 1.0)
        obj.data.materials.append(nipple)
        parts.append(obj)

    return parts


def garment_from_region(body, name, z_min, z_max, color, offset):
    """Duplicate a height band of the body and push it outward to make a well-fitting garment."""
    mesh_data = bmesh.new()
    mesh_data.from_mesh(body.data)
    mesh_data.faces.ensure_lookup_table()
    outside = [
        face for face in mesh_data.faces
        if not (z_min <= face.calc_center_median().z <= z_max)
    ]
    bmesh.ops.delete(mesh_data, geom=outside, context="FACES")
    bmesh.ops.recalc_face_normals(mesh_data, faces=mesh_data.faces)
    for vertex in mesh_data.verts:
        vertex.co += vertex.normal * offset

    mesh = bpy.data.meshes.new(name)
    mesh_data.to_mesh(mesh)
    mesh_data.free()
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.collection.objects.link(obj)
    mesh.materials.append(make_simple_material(name, color))
    return obj


def add_garments(body):
    """Garments derived from the body mesh so they fit and follow every morph."""
    return [
        garment_from_region(body, "Garment_Tops", 0.95, 1.48, (0.20, 0.45, 0.75), 0.014),
        garment_from_region(body, "Garment_Bottoms", 0.09, 0.95, (0.12, 0.16, 0.25), 0.012),
        garment_from_region(body, "Garment_Shoes", 0.0, 0.09, (0.08, 0.08, 0.08), 0.016),
    ]


def main():
    global HumanService, TargetService, HumanObjectProperties
    enable_mpfb()
    HumanService, TargetService, HumanObjectProperties = get_services()
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)

    human = HumanService.create_human()
    human.name = "Avatar"

    skin = bpy.data.materials.new("Skin")
    skin.use_nodes = True
    principled = skin.node_tree.nodes.get("Principled BSDF")
    principled.inputs["Base Color"].default_value = (0.76, 0.59, 0.48, 1.0)
    principled.inputs["Roughness"].default_value = 0.82
    principled.inputs["Metallic"].default_value = 0.0
    human.data.materials.clear()
    human.data.materials.append(skin)

    build_shape_keys(human)
    extras = add_appearance_parts(human)
    for obj in extras:
        bpy.ops.object.select_all(action="DESELECT")
        obj.select_set(True)
        bpy.context.view_layer.objects.active = obj
        bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)
    propagate_shape_keys(human, extras)
    kept = strip_helper_geometry(human)
    print(f"Kept {kept} body vertices")

    output = Path(__file__).resolve().parents[1] / "app/src/main/assets/models/avatar.glb"
    output.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.export_scene.gltf(
        filepath=str(output),
        export_format="GLB",
        export_morph=True,
        export_morph_normal=True,
        use_selection=False,
    )
    names = [k.name for k in human.data.shape_keys.key_blocks] if human.data.shape_keys else []
    print(f"Wrote {output} with {len(names)} shape keys: {names}")


if __name__ == "__main__":
    main()
