"""Generate a stylized low-poly mannequin with three morph targets.

Run from the repository root:
    blender --background --python tools/make_avatar.py

Output: app/src/main/assets/models/avatar.glb
Edit this script, not the generated GLB.
"""

from pathlib import Path

import bpy


def add_part(name, location, rotation=(0.0, 0.0, 0.0), **kwargs):
    bpy.ops.mesh.primitive_cylinder_add(location=location, rotation=rotation, **kwargs)
    part = bpy.context.active_object
    part.name = name
    return part


def build_body(material):
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)

    parts = []

    bpy.ops.mesh.primitive_uv_sphere_add(segments=12, ring_count=8, radius=0.12, location=(0, 0, 1.64))
    parts.append(bpy.context.active_object)

    parts.append(add_part("Neck", (0, 0, 1.48), radius=0.05, depth=0.12, vertices=8))

    bpy.ops.mesh.primitive_cone_add(vertices=8, radius1=0.16, radius2=0.24, depth=0.55, location=(0, 0, 1.17))
    parts.append(bpy.context.active_object)

    parts.append(add_part("Pelvis", (0, 0, 0.85), radius=0.18, depth=0.20, vertices=8))

    for side in (-1, 1):
        parts.append(
            add_part(
                f"Arm_{side}",
                (side * 0.30, 0, 1.16),
                rotation=(0, side * 0.14, 0),
                radius=0.055,
                depth=0.60,
                vertices=8,
            )
        )
        parts.append(
            add_part(
                f"Leg_{side}",
                (side * 0.11, 0, 0.42),
                radius=0.085,
                depth=0.82,
                vertices=8,
            )
        )

    for part in parts:
        part.data.materials.append(material)

    bpy.ops.object.select_all(action="DESELECT")
    for part in parts:
        part.select_set(True)
    bpy.context.view_layer.objects.active = parts[0]
    bpy.ops.object.join()

    obj = bpy.context.active_object
    obj.name = "Avatar"
    for polygon in obj.data.polygons:
        polygon.use_smooth = False
    return obj


def add_shape_keys(obj):
    basis = obj.shape_key_add(name="Basis", from_mix=False)
    slim = obj.shape_key_add(name="Slim", from_mix=False)
    tall = obj.shape_key_add(name="Tall", from_mix=False)
    muscle = obj.shape_key_add(name="Muscular", from_mix=False)

    for index, vertex in enumerate(basis.data):
        x, y, z = vertex.co

        slim_factor = 0.80 if 0.8 < z < 1.5 else 0.88
        slim.data[index].co = (x * slim_factor, y * slim_factor, z)

        tall.data[index].co = (x, y, z * 1.12)

        muscle_factor = 1.18 if 1.0 < z < 1.48 else 1.0
        muscle.data[index].co = (x * muscle_factor, y * muscle_factor, z)


def main():
    material = bpy.data.materials.new("Mannequin")
    material.diffuse_color = (0.62, 0.66, 0.72, 1.0)
    material.use_nodes = True
    principled = material.node_tree.nodes.get("Principled BSDF")
    principled.inputs["Base Color"].default_value = (0.62, 0.66, 0.72, 1.0)
    principled.inputs["Roughness"].default_value = 0.75

    avatar = build_body(material)
    add_shape_keys(avatar)

    output = Path(__file__).resolve().parents[1] / "app/src/main/assets/models/avatar.glb"
    output.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.export_scene.gltf(
        filepath=str(output),
        export_format="GLB",
        export_morph=True,
        export_morph_normal=True,
        use_selection=False,
    )
    print(f"Wrote {output}")


if __name__ == "__main__":
    main()
