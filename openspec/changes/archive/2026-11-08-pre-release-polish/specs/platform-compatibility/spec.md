# Spec Delta: platform-compatibility

## ADDED Requirements

### Requirement: Warning-clean main sources
Main sources SHALL compile with zero compiler warnings under the pinned JDK 21
/ Kotlin 2.x toolchain. Main sources MUST NOT contain constructs that the Kotlin
language committee has scheduled to become compile errors (progression
warnings), so that upgrading the Kotlin compiler does not break the build.
Fixing warnings MUST NOT change the public API or observable runtime behavior.

#### Scenario: Zero-warning build
- **WHEN** the main sources are compiled with the pinned toolchain using the project's standard build
- **THEN** the compiler emits no warnings for the main source set

#### Scenario: Ready for the next Kotlin language version
- **WHEN** the main sources are compiled with Kotlin progressive mode enabled, which turns scheduled language changes into errors
- **THEN** compilation succeeds
