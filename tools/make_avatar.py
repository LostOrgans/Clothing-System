"""Generate a realistic parametric human GLB using MPFB 2 (MakeHuman).

Run with Blender 4.2+ after installing MPFB 2 and the CC0 system assets pack:
    blender --background --python tools/make_avatar.py

Output: app/src/main/assets/models/avatar.glb
The GLB carries named morph targets; the app reads their names from mesh extras.
MPFB code is GPL (build-time tool only); MakeHuman assets are CC0 (commercial use OK).
"""

import importlib
import sys
from pathlib import Path

import bmesh
import bpy
import numpy as np


def dynamic_import(suffix, key):
    for module_name in sys.modules:
        if module_name.endswith(suffix):
            module = importlib.import_module(module_name)
            if hasattr(module, key):
                return getattr(module, key)
    raise ValueError(f"No module found ending in {suffix}")


MACROS = ("gender", "age", "muscle", "weight", "proportions", "height", "cupsize", "firmness")


def enable_mpfb():
    try:
        bpy.ops.preferences.addon_enable(module="bl_ext.user_default.mpfb")
    except RuntimeError as exc:
        if "already enabled" not in str(exc):
            raise
    for suffix in (
        "humanservice", "targetservice", "objectproperties", "assetservice",
    ):
        for module_name in list(sys.modules):
            if module_name.endswith(f"mpfb.services.{suffix}"):
                importlib.import_module(module_name)
    for module_name in list(sys.modules):
        if module_name.endswith("mpfb.entities.objectproperties"):
            importlib.import_module(module_name)


def force_opaque(materials):
    """MPFB wires texture alpha into the shader; glTF then renders eyes/mouth as see-through."""
    for material in materials:
        if not material or not material.use_nodes:
            continue
        nodes = material.node_tree.nodes
        principled = next((n for n in nodes if n.type == "BSDF_PRINCIPLED"), None)
        if principled is None:
            continue
        for link in list(material.node_tree.links):
            if link.to_node == principled and link.to_socket.name == "Alpha":
                material.node_tree.links.remove(link)
        principled.inputs["Alpha"].default_value = 1.0
        material.surface_render_method = "DITHERED"
        material.blend_method = "OPAQUE" if hasattr(material, "blend_method") else "OPAQUE"


def evaluated_positions(obj):
    depsgraph = bpy.context.evaluated_depsgraph_get()
    evaluated = obj.evaluated_get(depsgraph)
    mesh = evaluated.to_mesh()
    positions = [tuple(v.co) for v in mesh.vertices]
    evaluated.to_mesh_clear()
    return positions


def build_shape_keys(obj, human_properties, target_service):
    neutral = target_service.get_default_macro_info_dict()
    for name in MACROS:
        human_properties.set_value(name, neutral[name], entity_reference=obj)
    target_service.reapply_macro_details(obj)
    basis = evaluated_positions(obj)

    for name in MACROS:
        for extreme, value in (("min", 0.0), ("max", 1.0)):
            human_properties.set_value(name, value, entity_reference=obj)
            target_service.reapply_macro_details(obj)
            positions = evaluated_positions(obj)
            key = obj.shape_key_add(name=f"{name}_{extreme}", from_mix=False)
            for index, (bx, by, bz) in enumerate(basis):
                tx, ty, tz = positions[index]
                key.data[index].co = (bx + (tx - bx), by + (ty - by), bz + (tz - bz))
            key.value = 0.0
            human_properties.set_value(name, neutral[name], entity_reference=obj)
            target_service.reapply_macro_details(obj)


def strip_helpers(obj):
    """Remove MPFB's fitting helpers; the UVs and morphs stay intact with bmesh."""
    body = obj.vertex_groups.get("body")
    if body is None:
        raise RuntimeError("MPFB basemesh has no 'body' vertex group")
    keep = {v.index for v in obj.data.vertices if any(g.group == body.index for g in v.groups)}
    mesh = bmesh.new()
    mesh.from_mesh(obj.data)
    mesh.verts.ensure_lookup_table()
    bmesh.ops.delete(mesh, geom=[v for v in mesh.verts if v.index not in keep], context="VERTS")
    mesh.to_mesh(obj.data)
    mesh.free()
    obj.data.update()
    return len(keep)


def add_measurement_morphs(obj):
    """Explicit region morphs — MPFB's weight/muscle macros barely move the mesh."""
    basis = [tuple(v.co) for v in obj.data.vertices]

    def smoothstep(edge0, edge1, x):
        t = max(0.0, min(1.0, (x - edge0) / (edge1 - edge0)))
        return t * t * (3 - 2 * t)

    regions = (
        ("chest", 1.17, 0.16, -1.0, 1.0),
        ("waist", 1.03, 0.11, -1.0, 1.0),
        ("hips", 0.85, 0.13, -1.0, 1.0),
        ("shoulders", 1.45, 0.12, 0.12, 1.0),
        ("thighs", 0.55, 0.25, -1.0, 1.0),
    )
    for name, center, half_height, min_abs_x, max_abs_x in regions:
        for direction, factor in (("min", -0.16), ("max", 0.16)):
            key = obj.shape_key_add(name=f"{name}_{direction}", from_mix=False)
            for index, (x, y, z) in enumerate(basis):
                falloff = (1.0 - smoothstep(half_height * 0.6, half_height, abs(z - center)))
                abs_x = abs(x)
                side = smoothstep(min_abs_x, min_abs_x + 0.04, abs_x)
                side *= 1.0 - smoothstep(max_abs_x - 0.04, max_abs_x, abs_x)
                amount = factor * falloff * side
                key.data[index].co = (x + x * amount, y + y * amount, z)
            key.value = 0.0


def add_face_assets(human_service, asset_service, human):
    for subdir, filename, kind in (
        ("eyes", "low-poly.mhclo", "Eyes"),
        ("eyebrows", "eyebrow001.mhclo", "Eyebrows"),
        ("eyelashes", "eyelashes01.mhclo", "Eyelashes"),
        ("hair", "long01.mhclo", "Hair"),
    ):
        path = asset_service.find_asset_absolute_path(filename, asset_subdir=subdir)
        if path is None:
            continue
        obj = human_service.add_mhclo_asset(path, human, asset_type=kind, material_type="GAMEENGINE")
        if obj is not None:
            obj.name = f"Face_{kind}"


def add_garment_hide_morphs(human, mapping):
    """Collapse body vertices marked Delete.<asset> so skin does not poke through clothes."""
    for morph_name, group_name in mapping.items():
        group = human.vertex_groups.get(group_name)
        if group is None:
            continue
        key = human.shape_key_add(name=morph_name, from_mix=False)
        collapsed = set()
        strip = []
        kept = []
        for vertex in human.data.vertices:
            x, y, z = vertex.co
            weight = next(
                (g.weight for g in vertex.groups if g.group == group.index),
                0.0,
            )
            if weight > 0.5:
                collapsed.add(vertex.index)
                key.data[vertex.index].co = (0.0, 0.0, 1.0)
            elif abs(x) < 0.10 and 0.02 < z < 0.95:
                strip.append(vertex.index)
            else:
                kept.append(vertex.index)

        # The delete groups miss the inner-thigh gap; snap those vertices onto nearby
        # kept vertices so the triangles stay on the body surface under the clothes.
        kept_basis = np.array([tuple(human.data.vertices[i].co) for i in kept], dtype=np.float32)
        for index in strip:
            point = np.array(human.data.vertices[index].co, dtype=np.float32)
            distance = np.square(kept_basis - point).sum(axis=1)
            nearest = kept_basis[distance.argmin()]
            key.data[index].co = (float(nearest[0]), float(nearest[1]), float(nearest[2]))
        for index in kept:
            x, y, z = human.data.vertices[index].co
            key.data[index].co = (x, y, z)
        key.value = 0.0


def add_clothes(human_service, asset_service, human):
    """CC0 MHCLO garments fitted to the body; the app shows any combination of them."""
    for filename, name in (
        ("female_casualsuit01.mhclo", "Garment_Casual"),
        ("female_elegantsuit01.mhclo", "Garment_Elegant"),
        ("shoes01.mhclo", "Garment_Shoes"),
    ):
        path = asset_service.find_asset_absolute_path(filename, asset_subdir="clothes")
        if path is None:
            continue
        obj = human_service.add_mhclo_asset(
            path, human, asset_type="clothes", material_type="GAMEENGINE"
        )
        if obj is not None:
            obj.name = name


def main():
    enable_mpfb()
    human_service = dynamic_import("mpfb.services.humanservice", "HumanService")
    target_service = dynamic_import("mpfb.services.targetservice", "TargetService")
    human_properties = dynamic_import("mpfb.entities.objectproperties", "HumanObjectProperties")
    asset_service = dynamic_import("mpfb.services.assetservice", "AssetService")

    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)

    human = human_service.create_human()
    human.name = "Avatar"

    skin_path = asset_service.find_asset_absolute_path(
        "young_caucasian_female.mhmat", asset_subdir="skins"
    )
    if skin_path is None:
        raise RuntimeError("Install the CC0 makehuman_system_assets pack first")
    human_service.set_character_skin(skin_path, human, skin_type="GAMEENGINE")
    force_opaque(human.data.materials)
    add_face_assets(human_service, asset_service, human)
    add_clothes(human_service, asset_service, human)

    build_shape_keys(human, human_properties, target_service)
    kept = strip_helpers(human)
    add_measurement_morphs(human)
    add_garment_hide_morphs(
        human,
        {
            "hide_Casual": "Delete.female_casualsuit01",
            "hide_Elegant": "Delete.female_elegantsuit01",
            "hide_Shoes": "Delete.shoes01",
        },
    )
    print(f"Kept {kept} body vertices")

    # All morphs start at zero; the app drives them from measurements.
    for key in human.data.shape_keys.key_blocks:
        key.value = 0.0

    output = Path(__file__).resolve().parents[1] / "app/src/main/assets/models/avatar.glb"
    output.parent.mkdir(parents=True, exist_ok=True)
    # Downscale textures: 2048px skins are overkill on a phone.
    for image in bpy.data.images:
        if image.source == "FILE" and max(image.size) > 1024:
            image.scale(1024, 1024)

    bpy.ops.export_scene.gltf(
        filepath=str(output),
        export_format="GLB",
        export_morph=True,
        export_morph_normal=False,
        use_selection=False,
    )
    names = [k.name for k in human.data.shape_keys.key_blocks]
    print(f"Wrote {output} with {len(names) - 1} morph targets")


if __name__ == "__main__":
    main()
