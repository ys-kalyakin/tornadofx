# Implementation Notes — modernize-stack

## Task 1.1 — Resolved versions (Maven Central, repo1 metadata, 2026-11-08)

| Artifact | Version | Note |
|---|---|---|
| `org.openjfx:javafx-base/graphics/controls` | **21.0.13** | latest stable 21.0.x LTS patch |
| `org.jetbrains.kotlin:kotlin-stdlib/reflect/test/maven-plugin` | **2.4.21** | latest stable 2.x |
| `org.apache.httpcomponents.client5:httpclient5` | **5.6.5** | latest stable 5.x |
| `org.junit.jupiter:junit-jupiter` (+ platform) | **5.14.4** | latest stable 5.x per design D8; 6.x exists but is out of the approved scope |
| `org.testfx:testfx-core` | **4.0.18** | final; `testfx-junit` runner dropped |
| `org.jetbrains.dokka:dokka-maven-plugin` | **2.3.0** | latest stable 2.x |
| `maven-surefire-plugin` | **3.6.1** | JUnit platform auto-detect |
| `maven-compiler-plugin` | **3.16.0** | latest stable 3.x |
| `maven-jar-plugin` | **3.5.1** | latest stable 3.x |
| `maven-source-plugin` | **3.4.0** | latest stable 3.x |
| `maven-javadoc-plugin` | **3.12.0** | |
| `maven-gpg-plugin` | **3.2.8** | |
| `maven-release-plugin` | **3.3.1** | |
| `de.jensd:fontawesomefx` | 8.9 (unchanged) | test scope only, out of change scope |
| `org.glassfish:javax.json` | 1.1.2 (unchanged) | public JSON API, out of change scope |

Environment: local JDK is 25.0.4 only; sudo unavailable. Temurin JDK 21 (adoptium tarball) installed under `~/tools/jdk21` to satisfy the JDK 21 toolchain requirement; build runs with `JAVA_HOME=~/tools/jdk21`. Maven provided via the Maven Wrapper (`mvnw`, wrapper distribution 3.3.2, Maven 3.9.11) at repo root, per user request.

## Task 1.6 — Compile inventory (K2 + internal APIs, JDK 21 / JavaFX 21)

`mvn compile` on unmodified sources: **922 errors** (Kotlin 2.4.21 K2). Root causes (per file error counts):

| Root cause | Files hit (errors) | Fix task |
|---|---|---|
| `com.sun.javafx.scene.control.behavior/skin` internals gone (behavior classes not public since JavaFX 17 refactor) | DataGrid.kt (286), ItemControls.kt (106), SqueezeBox.kt (66), AutoCompleteComboBoxSkin.kt (44), Nodes.kt (4) | 2.3–2.6 |
| `javafx.fxml` / `javafx.media` / `javafx.web` used but not declared as deps | Component.kt (50: FXMLLoader, Media), Controls.kt (18: WebView, HTMLEditor) | pom deps added (see D2 addendum below) |
| JDK-internal APIs: `com.sun.glass.ui.Application`, `com.sun.javafx.tk.Toolkit` | Async.kt | 2.2 |
| `com.sun.javafx.application.PlatformImpl` | osgi/impl/ApplicationListener.kt | 5.1 (OSGi removed) |
| `sun.net.www.protocol.css.Handler` package — **note: kotlinc does NOT fail on the split package** (it compiled); removal is still required (jar/module-path correctness, spec) | Handler.kt | 2.1 |
| K2 strictness / overload resolution (`plus`, `get`, `mapEach`, `contains` candidates inapplicable; nullability `Boolean?` vs `Nothing!`; "Too many arguments for toString()") | Binding.kt, ViewModel.kt, Lib.kt, Json.kt, FX.kt, ErrorHandler.kt, Validation.kt, Forms.kt, CSS.kt, ListMenu.kt | 2.7 (post-cascade cleanup) |
| deprecated-for-removal Kotlin API is a hard error under K2 language 2.x (`String.toLowerCase()`, `Char.toLowerCase()`) | CSS.kt (512, 1244) | 2.7 |
| OSGi console | osgi/OSGIConsole.kt (2) | 5.1 |

### D2 addendum (implementation discovery)

OpenJFX module set is wider than designed: the framework compiles against **javafx-base, javafx-graphics, javafx-controls, javafx-fxml, javafx-media, javafx-web**. The spec's platform-compatibility requirement listed only (base, graphics, controls); spec delta and design D2 updated accordingly. All six are declared at 21.0.13 with platform classifiers.

Build environment: Corretto JDK 21 at `~/tools/amazon-corretto-21*` (`JAVA_HOME` per invocation), Maven via `./mvnw` (wrapper, Maven 3.9.11).

## Task-order note

Task 5.1 (OSGi removal) was executed BEFORE the Group 2 test verification: the Felix
`provided` dependency put `org.osgi.framework.FrameworkUtil` on the test classpath,
which made `FX.osgiAvailable` true and suppressed the css:// handler registration
(1.7 relied on the JVM's automatic lookup of `sun.net.www.protocol.css.Handler`,
which the relocation eliminates). Removing OSGi unblocked the css:// scenarios.

## Implementation findings (tasks 2.x, 3.x, 4.x)

- **SkinBase API change (JavaFX 21)**: `registerChangeListener(ObservableValue, String)`
  and `handleControlPropertyChanged(String)` are gone; skins were ported to the
  Consumer-based `registerChangeListener` API. `VirtualContainerBase` lost the
  8-era abstract `createCell` and the `flow` field is accessed via
  `getVirtualFlow()`; the protected `VirtualFlow.recreate/rebuild/reconfigure` are
  exposed through a small `DataGridFlow` subclass.
- **CellBehaviorBase replacement**: DataGrid selection is implemented as a
  `MOUSE_PRESSED` event filter on `DataGridCell` (primary button, shift-range and
  shortcut-toggle in multi-select mode). Spec scenarios live in
  `CustomControlsTest`.
- **AutoCompleteComboBoxSkin** now extends the public `ComboBoxListViewSkin`
  (popup chrome, editor `FakeFocusTextField` and display sync are inherited); it
  overrides only `getPopupContent` and the filter handling. The base
  `ComboBoxPopupControl` could not be used directly: its constructor casts
  `getEditor()` to the non-exported `FakeFocusTextField`.
- **HttpClient 5.6 preemptive auth**: the documented `authCache + BasicScheme.initPreemptive`
  recipe did not produce the Authorization header against a plain HttpServer
  (verified with a standalone probe and client5 DEBUG logs). `setBasicAuth`
  therefore applies the header through an `AuthContext` (`HttpURLBasicAuthContext`),
  mirroring `HttpURLEngine`; `HttpClientRequest.execute` now invokes
  `authContext.interceptRequest/interceptResponse`, which also makes
  `DigestAuthContext` functional for the Apache engine (it was dead code for it
  in 1.7).
- **JUnit 5**: message-first `Assert.xxx("msg", value)` overloads swapped to
  Jupiter's value-first signature across tests; `@Test(expected=…)` converted to
  `assertThrows`. surefire 3.6.1 needs an explicit `junit-platform-launcher`
  1.14.4 test dependency to match the Jupiter engine version.
- **Kotlin 2 inference fixes**: `ViewModel.bind` facade selection gained an
  `ObjectProperty` branch (K2 infers `PropertyType=ObjectProperty<String>` for
  producers returning `nameProperty()`; K1 picked the loosest type); tests
  adapted where they asserted K1's loose types (`inline_viewmodel`,
  `propertyFromMapKey`). `ClipboardTest` adapted to JavaFX 21 returning
  custom-format clipboard data as a serialized ByteBuffer.
- **HttpClient 5 API notes**: `HttpHost(hostname, scheme, port)` argument order;
  `InputStreamEntity(in, -1, null)`; no `consumeQuietly` (null-safe consume
  implemented); `HttpResponse.code/reasonPhrase` replace `StatusLine`.

## Verification summary

- Full suite after all changes: **385 tests, 0 failures, 0 errors** (JUnit 5 provider, surefire 3.6.1, Corretto JDK 21, JavaFX 21.0.13 linux classifier).
- Artifact checks: jar contains no `sun/` classes, no `tornadofx/osgi`, no `Bundle-*` manifest headers; manifest reports `Java-Version: 17`, `Build-Jdk-Spec: 21`.
- The pom artifact version was intentionally left at `1.7.21-SNAPSHOT` (a version
  bump was not part of the task list; flagged for the maintainer to decide at
  release time).

