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

    build_shape_keys(human, human_properties, target_service)
    kept = strip_helpers(human)
    print(f"Kept {kept} body vertices")

    # All morphs start at zero; the app drives them from measurements.
    for key in human.data.shape_keys.key_blocks:
        key.value = 0.0

    output = Path(__file__).resolve().parents[1] / "app/src/main/assets/models/avatar.glb"
    output.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.export_scene.gltf(
        filepath=str(output),
        export_format="GLB",
        export_morph=True,
        export_morph_normal=True,
        use_selection=False,
    )
    names = [k.name for k in human.data.shape_keys.key_blocks]
    print(f"Wrote {output} with {len(names) - 1} morph targets")


if __name__ == "__main__":
    main()
