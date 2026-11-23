# AGENTS.md

> Short entrypoint for agents working in this repository. Depth lives in `README.md` and `docs/adr/`.

## 1. Project profile

A digital wardrobe app. The current milestone is an Android-only avatar creator: a
stylized 3D mannequin that the user reshapes with sliders.

- Toolchain: Android Studio, JDK 17, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21
- Languages: Kotlin, Python (Blender mesh generator)
- Entrypoints: `app/src/main/java/com/example/clothingsystem/MainActivity.kt`, `tools/make_avatar.py`
- Canonical docs: `README.md`, `docs/adr/0001-avatar-rendering.md`
- Owner / reviewer: the repository owner (solo project)

## 2. Instruction precedence

1. The user's explicit instruction for this task.
2. This file and the nearest `AGENTS.md`.
3. `README.md` and `docs/adr/`.
4. Skill defaults.

## 3. Non-negotiables

- Never commit or push to the protected default branch (`main`). Branch before the first edit.
- Never print, commit, or invent secrets.
- Never claim completion without the gate and its output. If a check cannot run in this
  environment, say so explicitly.
- Keep changes small and single-purpose. Read before overwriting. Never discard uncommitted work.
- Do not add dependencies, publish, deploy, delete data, or spend money without explicit approval.
- Treat generated and vendored files as read-only unless the generator produced the change.
- If a change crosses a boundary above, stop and propose the smallest safe step.

## 4. Before you start

1. Read this file and `git status --short --branch`.
2. Confirm the default branch (`origin/main`). If on it, create a branch before editing.
3. Discover commands from `README.md`, `build.gradle.kts`, `gradle/libs.versions.toml`. Do not guess.
4. Run the brainstorming process before writing code for a new feature or behavior change.
   If the skill is unavailable, do the design step inline and get approval before product code.

## 5. Workflow

| Task size | Path |
|---|---|
| Quick fix | Equivalent check, change, verify, commit |
| Small change | Short design, test, implementation, review, verify |
| Feature or project | Design -> plan -> execute -> review -> verify -> integrate |

Behavior changes start with a failing test; non-behavior changes use an equivalent check
(config validation, dry run, or manual evidence). Stop and ask on conflicting requirements,
a destructive action, or a gate failure outside the change.

## 6. Commands and gates

Source: `README.md`, `build.gradle.kts`, `gradle/libs.versions.toml`.

```bash
# install: open the project in Android Studio and let it download the SDK + wrapper

# fast loop
./gradlew :app:compileDebugKotlin

# full gate
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The agent environment has no JDK, Gradle, or Android SDK, so these commands cannot be run
here. Report them as "not run in this environment" rather than claiming a pass.
A Python syntax check of `tools/make_avatar.py` does run here.

- Narrowest check while iterating; full gate before push.
- Never bypass with skipped tests, disabled lint, or lowered thresholds.

## 7. Architecture boundaries

- See `docs/adr/0001-avatar-rendering.md` for the rendering decision.
- `ui/` holds Compose screens only. `avatar/` holds model state and the Filament bridge.
- `tools/make_avatar.py` is generated-asset source; edit the script, not the GLB.
- Prefer additive seams over rewrites: a new capability gets its own module beside the existing one.

## 8. Branching and authorization

- Branch prefixes: `feat/`, `fix/`, `docs/`, `infra/`, `refactor/`, `test/`, `perf/`, `revert/`.
- Push only when the user asks. Merge, tag, publish, deploy, force-push, and delete need explicit approval.

## 9. Commits

Start every subject with `[TYPE] area:`, imperative, at most 72 characters, no trailing period.
Types: `[FEATURE]`, `[FIX]`, `[SECURITY]`, `[DOCS]`, `[INFRA]`, `[REFACTOR]`, `[TEST]`, `[PERF]`,
`[REVERT]`, `[RELEASE]`. One logical change per commit. No `Co-authored-by:` trailers.

## 10. Handoff format

End every task with: what changed and why, files touched, commands run and their results,
what is not verified, and the next concrete step.
