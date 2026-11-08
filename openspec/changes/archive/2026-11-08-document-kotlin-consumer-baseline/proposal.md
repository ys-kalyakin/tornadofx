# Proposal

## Why

The modernize-stack change (archived 2026-11-08) builds the framework with Kotlin 2.4.21 and sets no `apiVersion`/`languageVersion`, so the published artifact carries Kotlin metadata 2.4.0. Per Kotlin's binary compatibility rule (a compiler reads metadata up to one minor version ahead), consumers must build with Kotlin **2.3 or newer**. The consumer-facing documentation does not say this: README and CHANGELOG ask only for "Kotlin 2.x", so a consumer on Kotlin 2.0–2.2 who follows the docs verbatim hits `class ... was compiled with an incompatible version of Kotlin` at build time. The same README section contains a Gradle example still pinned to `jvmTarget = "1.8"`, contradicting the `jvmTarget 17` requirement stated one paragraph above it.

## What Changes

- README "Important version note": replace the loose "Kotlin 2.x" claim with the concrete consumer baseline — Kotlin 2.3+ — and add one sentence explaining why (the artifact ships metadata 2.4.0; Kotlin compilers read metadata one minor ahead).
- README Gradle example: fix `kotlinOptions.jvmTarget = "1.8"` → `"17"` to match the stated requirement.
- CHANGELOG breaking-changes "Kotlin 2.x (K2)" bullet: name the exact minimum (Kotlin 2.3+) and the reason, so release notes and README agree.
- Decision recorded (no code change): the consumer baseline is documented, not widened — no `-language-version 1.9` / `apiVersion` downgrade in the build. Downgrading would force reverting the K2-era fixes (e.g., the star-projected `Property<*>` assignment guard) and defeat the purpose of the K2 migration. Documenting the real baseline is the deliberate choice over pretending a wider window.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None — documentation-only change; no spec-level behavior changes (`skip_specs: true`). The `platform-compatibility` spec's "Modern platform baseline" requirement already states the framework is built with Kotlin 2.x; this change only makes the consumer-facing docs state the concrete consequence of that fact accurately.

## Impact

- Files: `README.md` (version note section, ~2 spots), `CHANGELOG.md` (one bullet).
- No code, build, or dependency changes; no re-verification of the test suite needed.
- Consumers on Kotlin 2.0–2.2 get an accurate error message path: the docs now tell them the minimum up front instead of failing mid-build.
