# Design

## Context

The suite is green locally (385 tests) on a machine with a display, but 23 test classes start the real JavaFX toolkit via `FxToolkit`/`Platform.startup`; the rest build node trees without the toolkit and are display-independent. There is no CI configuration in the repository. TestFX core 4.0.18 is already a test dependency. The Monocle glass platform is **not** bundled in the OpenJFX Maven artifacts (verified: `javafx-graphics-21.0.13-linux.jar` contains only the GTK glass platform); it is provided by the maintained test-scoped artifact `org.testfx:openjfx-monocle:21.0.2` (revived, tracks JavaFX versions: 17.0.10, 21.0.2).

## Goals / Non-Goals

**Goals:**
- One command runs the full default suite on a monitorless machine.
- Flaky/environment-dependent tests are out of the default run but runnable on demand.
- CI (GitHub Actions) runs the default suite headless as a blocking check.
- JavaFX 21 regressions surfaced by headless triage get fixed in framework code.

**Non-Goals:**
- Writing new UI tests or TestFX coverage beyond triage fixes.
- Upgrading or replacing TestFX.
- Making the local (displayed) developer workflow use Monocle.
- Pixel-perfect rendering verification.

## Decisions

### D1: Monocle via a Maven `headless` profile + test-scoped artifact, not always-on
A `headless` Maven profile adds the system properties to surefire (`glass.platform=Monocle`, `monocle.platform=Headless`, `prism.order=sw`), and `org.testfx:openjfx-monocle:21.0.2` supplies the glass platform itself as a test-scoped dependency (it is absent from the OpenJFX Maven artifacts). CI builds with `-Pheadless`, local default runs keep the real glass platform.
*Why not always-on:* uniformity is nice but it would change clipboard/timing behavior and rendering speed for every contributor, and the "displayed workstation unaffected" guarantee is then lost.
*Why not Xvfb on CI:* needs an OS-level X server package and is slower; the Monocle artifact needs no OS support at all — important because this machine has no sudo and cannot install Xvfb for local headless verification.

### D2: Explicit glass properties, not TestFX's `testfx.headless`
Tests here call `FxToolkit` directly (no TestFX runner extensions), so the reliable path is setting the three glass/prism properties ourselves via surefire `systemPropertyVariables`. TestFX's built-in headless switch (`testfx.headless=true`, 4.0.16+) would do the same indirectly but only inside TestFX-managed setup.

### D3: Quarantine = JUnit tag + surefire groups
`@Tag("quarantine")` on tests; main surefire config sets `excludedGroups=quarantine` (applies everywhere by default, local and CI); a `quarantine` Maven profile overrides it with `groups=quarantine` (and empty exclusion) so `mvn test -Pquarantine` runs exactly the quarantined set. No separate source set, no extra plugins.
First quarantined candidate: `ClipboardTest` (system clipboard is an OS facility that headless does not provide). Async timing suspects (`AsyncTest`, `MultipleLifecycleAsyncAppTest`, `ViewDockingTests`) get one stabilization attempt before tagging.

### D4: Triage rule — fix or quarantine, nothing else
Deterministic headless failure caused by framework code → fix the framework (custom skins, stylesheet handling, event delivery are the expected areas; this is the point of the exercise). Environment-dependent failure → quarantine immediately. Intermittent failure → one bounded stabilization attempt (timeouts/polling), then quarantine. Every quarantine tag gets a code comment stating the reason.

### D5: CI — one blocking job, quarantine run non-blocking
GitHub Actions, `ubuntu-latest`, JDK 21 via `actions/setup-java` (Corretto, matching the local toolchain), `./mvnw test -Pheadless` with the Maven local repository cached. A second, non-blocking job (`continue-on-error`) runs `mvnw test -Pheadless -Pquarantine` so flaky drift stays visible without breaking CI.

## Risks / Trade-offs

- [TestFX 4.0.18 predates JavaFX 21; `FxToolkit` internals may hit module/access issues] → Properties-based startup is independent of TestFX version; if `FxToolkit` itself breaks, replace its use with a small `Platform.startup`-based helper in test code before considering a TestFX upgrade.
- [Software renderer (prism sw) differs from GPU rendering (text AA, snapshot pixels)] → No pixel-identical assertions in tests; snapshot-based checks compare structure/properties, not bitmaps.
- [Monocle headless has no real OS integrations beyond clipboard (e.g., drag-and-drop)] → Same triage rule: environment-dependent → quarantine, with reason recorded.
- [One JVM-wide toolkit: class execution order and fork settings can interact with `FxToolkit` caching] → Keep surefire defaults (single fork, reuse) initially; adjust `forkCount` only if triage shows cross-class contamination.
- [Two-core CI runners make timing-sensitive tests flakier than local] → Generous timeouts during stabilization; the quarantine tag is the safety valve, never ignored failures.
- [Triage uncovers more framework breakage than estimated] → Fixes are behavior-preserving and in scope; remaining items surface in the suite and can be split into a follow-up change rather than quarantined.

## Migration Plan

Additive only: new profile(s), new tags, new workflow file. Rollback = revert the pom surefire changes and delete `.github/workflows/ci.yml`; no product migration, no consumer impact.

## Open Questions

None blocking. The quarantine job's non-blocking status is a deliberate default; making it blocking later is a one-line workflow edit once its noise level is known.
