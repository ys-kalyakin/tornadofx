# Proposal

## Why

The modernized stack (JDK 21 / JavaFX 21 / Kotlin 2.4.21) has a green local test run (385 tests), but 23 test classes start the real JavaFX toolkit via `FxToolkit`/`Platform.startup` and therefore require a display. The repository has no CI at all, so the suite cannot run on a monitorless CI machine: toolkit startup fails without a windowing system. Headless execution is also expected to surface real JavaFX 21 regressions (skins, CSS parser, event delivery) that the display-based local run does not catch, and there is no mechanism to keep flaky tests from breaking CI.

## What Changes

- Add a headless test configuration to the Maven build that runs the JavaFX toolkit on the Monocle glass platform bundled with JavaFX 21 (`glass.platform=Monocle`, `monocle.platform=Headless`, `prism.order=sw`), no extra dependency.
- Run the existing UI test suite headless, triage every failure, and fix real JavaFX 21 regressions in framework code (custom skins, stylesheet handling, event delivery) where the framework is at fault.
- Introduce a quarantine mechanism: tests that are flaky or environment-dependent get `@Tag("quarantine")` and are excluded from the default test run; a dedicated Maven invocation runs quarantine tests for triage.
- Add a CI workflow (GitHub Actions, ubuntu, JDK 21) that runs the default test suite headless on every push and pull request, so regressions fail the build.
- No product API changes; all changes are test/build infrastructure plus framework fixes that preserve existing behavior.

## Capabilities

### New Capabilities

- `headless-ui-testing`: The UI test suite runs without a display on the bundled Monocle headless platform, flaky or environment-dependent tests are quarantined out of the default run but remain runnable for triage, and CI verifies the suite headless on every change.

### Modified Capabilities

- (none — framework fixes made under this change preserve the requirements already specified in `ui-controls` and `css-stylesheet-loading`; no requirement text changes)

## Impact

- `pom.xml`: surefire configuration (headless system properties, quarantine exclusion/inclusion), possibly a dedicated Maven profile.
- `src/test/**`: tags on quarantined tests; fixes to tests that misbehave headless.
- `src/main/kotlin/tornadofx/**`: only where headless triage reveals JavaFX 21 regressions in skins/CSS/events — behavior-preserving fixes.
- New `.github/workflows/ci.yml`: no existing CI is replaced (there is none).
- Consumers: no impact — no shipped API or dependency changes (Monocle properties are test-scoped).
