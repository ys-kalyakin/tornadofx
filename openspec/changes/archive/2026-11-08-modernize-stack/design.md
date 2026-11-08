# Design

## Context

TornadoFX 1.7.21-SNAPSHOT is a single-module Maven library (`no.tornado:tornadofx`, ~148 Kotlin files) built for the Java 8 / bundled-JavaFX-8 era. The code compiles only against Java 8-era internals: a `URLStreamHandler` living in package `sun.net.www.protocol.css`, `com.sun.glass`/`com.sun.javafx.tk` calls in `Async.kt`, `com.sun.javafx.scene.control.skin/behavior` in four custom skins, `PlatformImpl` reflection in the OSGi activator, and a stray `com.sun.xml.internal.bind` import in `ListMenu.kt` (shadowed by a local `FACTORY` — effectively dead). The REST client's public classes (`HttpClientEngine`, `HttpClientRequest`, `HttpClientResponse`) expose Apache HttpClient 4 types; ~28 test files use JUnit 4 + TestFX `FxToolkit`. The REST tests already use `com.sun.net.httpserver` (supported `jdk.httpserver` API — stays). `javax.json` is woven through the public `Json.kt` API. See proposal.md — Why for motivation.

## Goals / Non-Goals

**Goals:**
- Build and pass tests on JDK 21 with JavaFX 21 and Kotlin 2.x as the declared stack.
- Publish an artifact consumable from JDK 17+ runtimes (JavaFX 21's own minimum).
- Remove all JDK-internal (`sun.*`, `com.sun.glass`, `com.sun.xml.internal`) usage from main sources; move custom skins onto public JavaFX skin APIs.
- Keep framework feature parity: type-safe CSS, REST client, custom controls, JSON utilities behave as in 1.7.x (minus removed pieces).

**Non-Goals:**
- No `javax.json` replacement (public API surface too large; separate change if ever needed).
- No migration to JPMS (`module-info.java`); jars stay on the classpath story (`Automatic-Module-Name` may be added only if the bundle plugin setup already emits one — not a requirement).
- No Gradle migration; Maven stays.
- No headless/Monocle CI test infrastructure work; tests still require a display as today.
- No Android/GraalVM substrate targets.
- OSGi support is removed, not modernized (decision below).

## Decisions

### D1. Platform versions and bytecode target
- Build toolchain: JDK 21. Bytecode target: **17** (`maven.compiler.release=17`, Kotlin `jvmTarget=17`).
- Rationale: JavaFX 21 itself requires JDK 17+, so bytecode 17 excludes nobody who can actually run the framework, while still allowing the build/CI to standardize on JDK 21. Alternative considered: bytecode 21 (simpler story, needlessly excludes JDK 17/20 consumers running JavaFX 21). Exact patch pins (JavaFX 21.0.x latest, Kotlin latest stable 2.x, HttpClient 5.x, JUnit 5.x, TestFX 4.0.18, Felix removed, Dokka 2.x, surefire 3.5.x, maven-bundle-plugin dropped) are resolved against Maven Central at implementation time and recorded in `tasks.md`.

### D2. OpenJFX as Maven dependency
- Declare `org.openjfx:javafx-base/graphics/controls/fxml/media/web` at 21.0.x as `compile`-scoped (compile is needed because tornadofx's public API extends/reveals JavaFX types) with platform classifier resolution via Maven profiles activating on `os.name` setting the `javafx.platform` property (`linux`/`mac`/`win`) — the standard documented Maven pattern for OpenJFX. Module set completed during implementation (task 1.6 inventory): the framework's public API also exposes FXMLLoader (fxml), Media/MediaPlayer (media) and WebView/HTMLEditor (web) helpers.
- Alternative considered: `javafx-maven-plugin` — designed for running apps, wrong tool for a library; classifier-free "empty jar" trick — fragile across JavaFX patch releases.
- Risk: contributors on an unlisted OS (e.g., freebsd) need `-Djavafx.platform=linux` override; documented in README update task.

### D3. CSS handler relocation
- Move `sun/net/www/protocol/css/Handler.kt` → `tornadofx/css/Handler.kt` (package `tornadofx.css`), update the single import in `CSS.kt:473`. Registration is already an explicit `URL.setURLStreamHandlerFactory(...)` call, so no discovery mechanism depends on the package name. `HandlerFactory` stays as-is.
- Note: `URL.setURLStreamHandlerFactory` is JVM-once-per-process; existing behavior (guard against second registration) is preserved.
- Alternative considered: `java.protocol.handler.pkgs` system property — requires specific package layout, more fragile than the existing explicit registration.

### D4. Async.kt internal replacements
- `com.sun.glass.ui.Application.isEventThread()` → `javafx.application.Platform.isFxApplicationThread()` (equivalent semantics).
- `Toolkit.getToolkit().enterNestedEventLoop/exitNestedEventLoop` → public `Platform.enterNestedEventLoop/exitNestedEventLoop` (public since JavaFX 9; same key-based contract).
- `Toolkit.getToolkit().canStartNestedEventLoop()` (no public equivalent) → keep the "awaitUntil is not allowed during animation or layout processing" failure semantics by catching the `IllegalStateException` that `Platform.enterNestedEventLoop` throws in those states and rethrowing with the original message.
- Alternative considered: keeping `com.sun.javafx.tk.Toolkit` with `--add-exports` — violates the no-internal-APIs requirement and breaks non-flagged consumers.

### D5. Skins on public APIs
- Direct import swaps: `com.sun.javafx.scene.control.skin.*` → `javafx.scene.control.skin.*` (public since JavaFX 9): `TableRowSkin`, `TableColumnHeader` (`ItemControls.kt`, `Nodes.kt`), `CellSkinBase`, `VirtualContainerBase` (`DataGrid.kt`), `ComboBoxPopupControl` (`AutoCompleteComboBoxSkin.kt`).
- Behavior classes (`BehaviorBase`, `CellBehaviorBase`, `ComboBoxListViewBehavior`, `BehaviorSkinBase`) live in non-exported `com.sun.javafx.scene.control.behavior` and changed API in JavaFX 17 — they cannot be used. Replacements:
  - `DataGrid`: reimplement selection behavior (mouse press → select cell, keyboard focus) as event handlers inside `DataGridSkin`; drop `BehaviorBase`/`CellBehaviorBase` inheritance, extend `CellSkinBase`/`SkinBase` directly.
  - `SqueezeBox`: reimplement on `SkinBase` + pane-expanded property listeners (the 1.7.x behavior logic is simple toggle semantics); drop `BehaviorSkinBase`.
  - `AutoCompleteComboBoxSkin`: base on public `ComboBoxPopupControl`, implement popup show/hide and editor key handling itself (ControlsFX-style), dropping `ComboBoxListViewBehavior`.
- Alternative considered: shipping `--add-exports` documentation for `com.sun.javafx.scene.control.behavior` — rejected: breaks plain classpath consumers and the API is unstable across JavaFX releases.

### D6. REST client on HttpClient 5
- Port `Rest.kt` to Apache HttpClient 5.x classic API (`org.apache.hc.client5.http.*`, `org.apache.hc.core5.http.*`): `CloseableHttpClient` stays (new coordinates), `HttpClientContext` → `org.apache.hc.client5.protocol.HttpClientContext`, entities/config/auth move to client5 packages (`BasicScheme` construction and credentials handling change per client5 API — auth scope now uses `HttpHost`+realm).
- Public classes `HttpClientEngine`, `HttpClientRequest`, `HttpClientResponse` now expose client5 types (**BREAKING**, declared in proposal). The `Rest.Engine` abstraction and the `Rest` facade keep their signatures.
- The `provided` scope for httpclient stays `provided`-equivalent in Maven terms? No — client5 becomes a regular `compile` dependency? Decision: keep `provided` semantics 1.7.x had (Rest engine classes reference Apache types but the dependency is declared `provided` so consumers choose to have it). Concretely: declare `org.apache.httpcomponents.client5:httpclient5` with scope `provided` (same as today's httpclient 4). Existing `useApacheHttpClient()` opt-in behavior preserved.
- Tests: `RestClientTest` retargeted to client5 semantics (server side stays `com.sun.net.httpserver`).

### D7. Remove OSGi support
- Delete `src/main/java/tornadofx/osgi/**` (12 files incl. `CSSURLStreamHandlerService`), the Felix dependency and test dependency, `maven-bundle-plugin` and its `Bundle-Activator`/manifest wiring in `pom.xml`, and the `META-INF/MANIFEST.MF` bundle configuration.
- Rationale: JavaFX 21 artifacts are not OSGi bundles, making OSGi provisioning of the new stack a wrapping exercise for every consumer; the OSGi module was the largest blocker for internal-API cleanup (`PlatformImpl`, URL handler service); community activity concentrates on non-OSGi use.
- Consumers needing OSGi stay on 1.7.x (documented in README/CHANGELOG).
- Alternative considered: keep-and-upgrade Felix 7 — rejected by user decision; maintenance cost disproportional.

### D8. Test stack migration
- JUnit 4 → JUnit 5: `org.junit.Test` → `org.junit.jupiter.api.Test`, `Assert` → `Assertions`, `Before/AfterClass` → `BeforeAll/AfterAll`; `junit` + `kotlin-test-junit` replaced by `junit-jupiter` + `kotlin-test-junit5`; surefire upgraded to 3.x for platform auto-detection.
- TestFX 4.0.4-alpha → 4.0.18 (final): `FxToolkit`/`FxRobot` usage stays; no testfx-junit runner dependency (JUnit 5 needs none).
- Test files are migrated mechanically first, then behaviorally adjusted only where HttpClient 5 or skins require it.

### D9. Build infrastructure
- Remove jcenter `pluginRepository`; everything resolves from Maven Central.
- `kotlin-stdlib-jdk8` → `kotlin-stdlib` (merged artifact since Kotlin 1.8); Kotlin maven plugin version tracks Kotlin release.
- Dokka 0.10.0 → 2.x `dokka-maven-plugin` (site/javadoc phase wiring preserved or javadoc delegated to Dokka — decided at implementation based on the 2.x plugin's Maven lifecycle support).
- Plugin bumps: compiler, jar, source, javadoc, gpg, release to current stable versions; delete `maven.gpg`/release-era hacks only if unused.
- `ListMenu.kt` dead import `com.sun.xml.internal.bind.v2.runtime.unmarshaller.UnmarshallerImpl.FACTORY` deleted (local `FACTORY` shadows it).

## Risks / Trade-offs

- [Behavior regression in reimplemented skin behaviors (D5)] → Port the 1.7.x `Stylesheet`/controls tests and add selection/expand scenarios for DataGrid, SqueezeBox, AutoCompleteComboBox before deleting behavior-class usage; verify against existing `ControlsTest`, `TableViewTest` suites.
- [HttpClient 4→5 semantic drift (auth scheme sequencing, entity repeatable-ness, response entity closing)] → Port `RestClientTest` scenarios 1:1, add auth + timeout cases; keep `Rest` facade API stable so app-level code is unaffected.
- [Kotlin 2 (K2) compile errors from 1.3-era code] → Compile early with Kotlin 2 on the unmodified sources to inventory failures; fall back to `-language-version 1.9` only if failures are pervasive, and record the choice in tasks.
- [OpenJFX classifier profiles mis-resolve on uncommon OS/arch] → Default profile falls back to `linux` classifier with an explicit property override documented in README; CI matrix covers linux + mac + win.
- [`Platform.enterNestedEventLoop` restrictions differ subtly from `Toolkit` internals (D4)] → `awaitUntil` tests (`AsyncTest`) cover the nested-loop paths; error message preserved via rethrow.
- [JDK 17 bytecode vs libraries compiled against 1.7.x] → This is a major-version compatibility break by design; CHANGELOG documents migration notes for consumers (JavaFX dependency, Rest types, OSGi removal).

## Migration Plan

1. Land build-infrastructure changes first (D1, D2, D9) with source untouched except making it compile — establishes the new toolchain baseline.
2. Apply internal-API replacements (D3–D5) in one series, tests green at each step.
3. Port REST to HttpClient 5 (D6) with test parity.
4. Remove OSGi (D7) and migrate tests (D8).
5. Consumer rollback strategy: this is a deliberate major-version break; consumers pin the previous `1.7.20` release. Repo-side rollback = git revert of the change series.

## Open Questions

- Exact Dokka 2.x Maven lifecycle wiring (site vs javadoc goal) — resolved during implementation, does not affect specs.
- Whether any `com.sun.javafx.*` import beyond skins remains necessary after D5 (e.g., `PlatformImpl` in non-OSGi paths) — inventory is re-checked during implementation; any remainder must be confined per `platform-compatibility` spec.
