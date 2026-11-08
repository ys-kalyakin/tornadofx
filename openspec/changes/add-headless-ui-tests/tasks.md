# Tasks

## 1. Headless build configuration

- [x] 1.1 Add a `headless` Maven profile to `pom.xml` that sets surefire system properties `glass.platform=Monocle`, `monocle.platform=Headless`, `prism.order=sw`; verify `mvnw test -Pheadless` picks the properties up (one test logs/observes `Platform.getPlatform` or a smoke class runs toolkit headless)
- [x] 1.2 Run the full default suite headless on this machine with the display server unavailable (e.g. `env -u DISPLAY -u WAYLAND_DISPLAY mvnw test -Pheadless`) and record the baseline: which of the 23 toolkit-starting classes pass, fail, or error; commit the triage list into the change notes
- [x] 1.3 Confirm the displayed run is unaffected: plain `mvnw test` (no profile) is still green locally

## 2. Triage and framework fixes

- [x] 2.1 Triage each failure from 1.2 into fix-in-framework vs environment-dependent vs flaky, following design D4; record the classification next to the baseline list
- [x] 2.2 Fix deterministic framework-side failures (expected areas: custom skins, stylesheet loading/error handling, event delivery) so the affected tests pass headless; verify each affected test passes under `mvnw test -Pheadless -Dtest=<Class>`
- [x] 2.3 For tests that misbehave headless due to test-code assumptions (not framework defects), fix the test code; verify they pass headless without weakening what they assert
- [x] 2.4 Re-run the full headless suite; the remaining red set must contain only candidates for stabilization/quarantine from 2.2–2.3, and plain `mvnw test` stays green

## 3. Quarantine mechanism

- [x] 3.1 Set `excludedGroups=quarantine` in the main surefire configuration of `pom.xml`; verify a test tagged `@Tag("quarantine")` is skipped by the default `mvnw test` (use a temporary tagged test, then remove it)
- [x] 3.2 Add a `quarantine` Maven profile that switches surefire to `groups=quarantine` (exclusion off); verify `mvnw test -Pquarantine` runs exactly the tagged set from 3.1's check
- [x] 3.3 Tag environment-dependent and still-flaky tests from the 2.4 baseline with `@Tag("quarantine")` plus a comment stating the reason (first expected: `ClipboardTest`); verify the default suite is green headless and the tagged set runs under `-Pquarantine`

## 4. CI workflow

- [x] 4.1 Add `.github/workflows/ci.yml`: ubuntu runner, JDK 21 (Corretto) via `actions/setup-java`, Maven repo cache, blocking job `mvnw test -Pheadless`; verify the workflow syntax (`actionlint` or YAML parse) locally
- [x] 4.2 Add a non-blocking job (`continue-on-error`) running `mvnw test -Pheadless -Pquarantine` for drift visibility; verify the workflow file parses and both jobs reference existing profiles
- [x] 4.3 Push the branch and observe the CI run end to end: blocking job green on a clean tree, and a deliberately broken assertion (reverted afterwards) turns it red

## 5. Final integration check

- [x] 5.1 Full verification matrix on one commit: `mvnw test` green (displayed), `env -u DISPLAY -u WAYLAND_DISPLAY mvnw test -Pheadless` green (headless), `env -u DISPLAY -u WAYLAND_DISPLAY mvnw test -Pheadless -Pquarantine` runs the quarantined set; produced artifact (`mvnw package -DskipTests`) is byte-comparable to a pre-change build (same deps)
- [x] 5.2 Update `README.md` (or docs) with a short section: how to run tests headless and how quarantine works; verify the documented commands work as written

## Workflow follow-up

- Archive the change after the project's review requirements are satisfied.
- Verify the archived result.
