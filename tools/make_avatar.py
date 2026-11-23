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
