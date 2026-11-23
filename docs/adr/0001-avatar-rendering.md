# 0001: Avatar rendering and body model

- Status: Accepted
- Date: `2026-11-23`
- Deciders: repository owner

## Context

The first milestone is an on-screen 3D human the user can reshape. The app is
Android-only and built by one person part-time. A parametric body needs a mesh whose
vertices can be driven independently at runtime. Three global sliders (slim/tall/muscle)
were found inadequate: they couple independent body dimensions.

## Decision

- Render with Filament's `ModelViewer` (`com.google.android.filament:filament-utils-android`).
- Generate the base mesh with **MPFB 2** (MakeHuman's Blender generator). MPFB's bundled
  assets are **CC0**, so generated models can be used commercially without a license fee.
  MPFB itself is GPL and is a build-time tool only; no MPFB code ships in the app.
- The generator samples each macro dimension (gender, age, muscle, weight, proportions,
  height, cupsize, firmness) at both extremes, creating 16 named morph targets plus MPFB's
  own detail targets (24 total). The GLB stores target names in `mesh.extras.targetNames`.
- The app reads morph names and default weights from the GLB at runtime and generates one
  slider per target. Adding morphs to the model needs no app code change.

## Alternatives considered

| Alternative | Why not |
|---|---|
| SMPL / SMPL-X | Non-commercial research license; unsuitable for a commercial app |
| Photogrammetry from many photos | Static mesh; cannot be dressed or resized |
| Hosted avatar service | Cloud dependency, limited parametric control |
| 2D paper doll | User requires a realistic 3D human |
| Hand-modelled stylized mesh | Only 3 coupled morphs; not realistic |

## Consequences

- Positive: realistic human, 24 independently adjustable dimensions, CC0 licensing,
  extensible to measurements and photo-driven targets without replacing the renderer.
- Negative: 12 MB GLB, MPFB must be installed to regenerate it, Filament/glTF setup is
  a learning curve, and the current look is stylized-by-default (no skin texture yet).
- Follow-up: add MakeHuman system skin assets (CC0), measurement form mapped to morph
  weights, and photo-derived targets as separate milestones.
