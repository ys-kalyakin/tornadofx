# Tasks

## 1. README — consumer baseline

- [x] 1.1 In README's "Important version note", replace "TornadoFX requires Kotlin 2.x and jvmTarget 17" with the concrete baseline: Kotlin **2.3 or newer** (reason, one sentence: the artifact ships Kotlin metadata 2.4.0, and a Kotlin compiler reads metadata up to one minor version ahead). Verify: the section states 2.3+ and the wording agrees with the CHANGELOG bullet.
- [x] 1.2 In the same section, fix the Gradle example: `kotlinOptions.jvmTarget = "1.8"` → `"17"`. Verify: the Gradle snippet matches the Maven example (`<jvmTarget>17</jvmTarget>`) directly above it.

## 2. CHANGELOG — breaking-change precision

- [x] 2.1 In CHANGELOG's `[1.7.21-SNAPSHOT]` → "Breaking changes" → "Kotlin 2.x (K2)" bullet, state the exact consumer minimum (Kotlin 2.3+) and why (tornadofx ships metadata 2.4.0; Kotlin compilers read metadata one minor ahead). Verify: the bullet and README name the same minimum.

## 3. Consistency check

- [x] 3.1 Search README and CHANGELOG for remaining unqualified "Kotlin 2.x" consumer-facing claims; qualify or correct each to the 2.3+ baseline. Verify: no consumer-facing doc instructs a Kotlin version below 2.3.

## Workflow follow-up

- Archive the change after review (`/opsx:archive`); docs-only, no build or test run needed.
