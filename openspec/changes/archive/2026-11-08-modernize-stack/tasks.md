# Tasks

## 1. Build infrastructure & toolchain (design D1, D2, D9)

- [x] 1.1 Resolve exact current stable versions from Maven Central and record them in this change's notes: JavaFX 21.0.x (latest patch), Kotlin latest stable 2.x, HttpClient5 latest 5.x, JUnit Jupiter latest 5.x, TestFX 4.0.18, Dokka 2.x, surefire 3.x, and remaining Maven plugins (compiler, jar, source, javadoc, gpg, release). Verification: version list committed to the change notes.
- [x] 1.2 Update `pom.xml`: remove jcenter `pluginRepository`, bump Maven plugins and surefire, set `maven.compiler.release=17` and Kotlin `jvmTarget=17` (build runs on JDK 21). Verification: `mvn -q validate` succeeds and `mvn help:effective-pom` shows no jcenter reference and release=17.
- [x] 1.3 Swap `kotlin-stdlib-jdk8` → `kotlin-stdlib` and bump `kotlin-maven-plugin`/`kotlin-reflect`/`kotlin-test-junit` to the chosen Kotlin 2.x version. Verification: `mvn -q dependency:resolve` succeeds with only Maven Central configured.
- [x] 1.4 Add OpenJFX 21.0.x dependencies (`org.openjfx:javafx-base/graphics/controls`) with platform classifier resolution via `javafx.platform` property and OS-activated Maven profiles (linux/mac/win, default linux) per design D2. Verification: `mvn -q dependency:resolve -Djavafx.platform=linux` (and spot-check `mac`/`win`) pull javafx jars.
- [x] 1.5 Bump Dokka to 2.x keeping the javadoc/site goal wired to the build lifecycle. Verification: `mvn -q dokka:dokka` (or the lifecycle goal chosen) generates docs without errors.
- [x] 1.6 Compile inventory: run `mvn compile` with the new toolchain on unmodified sources and record every K2 and internal-API failure (expected: sun.* split package, com.sun.javafx skin/behavior, glass/Toolkit, PlatformImpl). Verification: failure inventory written into the change notes.

## 2. Main sources compile on the new stack (design D3, D4, D5)

- [x] 2.1 Relocate `sun/net/www/protocol/css/Handler.kt` to package `tornadofx.css` (`src/main/java/tornadofx/css/Handler.kt`) and update the registration import in `CSS.kt:473`; delete the `src/main/java/sun` tree. Verification: `mvn compile` no longer reports the split-package error and `jar tf target/*.jar | grep '^sun/'` is empty.
- [x] 2.2 Port `Async.kt` to public APIs: `Platform.isFxApplicationThread()`, `Platform.enterNestedEventLoop/exitNestedEventLoop`, and rethrow-with-original-message for the nested-loop precondition. Verification: `mvn test -Dtest=AsyncTest` passes.
- [x] 2.3 Swap direct skin imports in `ItemControls.kt` and `Nodes.kt` (`com.sun.javafx.scene.control.skin.*` → `javafx.scene.control.skin.*`). Verification: both files compile and `TableViewTest` passes.
- [x] 2.4 Reimplement `DataGrid` selection behavior as event handlers in `DataGridSkin` (drop `BehaviorBase`/`CellBehaviorBase`); delete the dead `com.sun.xml.internal.bind` import in `ListMenu.kt`. Verification: DataGrid selection/keyboard tests pass (add selection scenario to `ControlsTest` if absent) and `grep -r "com.sun.javafx" src/main/java/tornadofx/DataGrid.kt src/main/java/tornadofx/ListMenu.kt` is empty.
- [x] 2.5 Reimplement `SqueezeBox` on `SkinBase` with expanded-property listeners (drop `BehaviorSkinBase`); verify single-expand and multi-expand scenarios from `ControlsTest`. Verification: `mvn test -Dtest=ControlsTest` passes.
- [x] 2.6 Rebase `AutoCompleteComboBoxSkin` on public `ComboBoxPopupControl` with own popup show/hide and editor key handling (drop `ComboBoxListViewBehavior`). Verification: auto-complete filtering/commit scenarios pass (add to `ControlsTest` if absent).
- [x] 2.7 Full compile gate: `mvn clean compile` succeeds on JDK 21 + JavaFX 21 + Kotlin 2 with no `--add-exports` flags, and `grep -r "import sun\.\|import com.sun.glass\|import com.sun.xml.internal" src/main` is empty. Verification: build log clean.

## 3. Test stack migration (design D8)

- [x] 3.1 Replace `junit`/`kotlin-test-junit` with `junit-jupiter`/`kotlin-test-junit5` and mechanically migrate ~28 test files (`Test`, `Before`/`BeforeClass`, `AfterClass`, `Assert` → Jupiter equivalents). Verification: `mvn test` runs the JUnit platform (surefire reports show jupiter provider) and suites pass.
- [x] 3.2 Upgrade TestFX to 4.0.18 and keep `FxToolkit`/`FxRobot` usage as-is. Verification: FxToolkit-based suites (e.g. `ControlsTest`, `LayoutsTest`) pass under the new versions.

## 4. REST client on HttpClient 5 (design D6)

- [x] 4.1 Add `org.apache.httpcomponents.client5:httpclient5` (scope `provided`) and port `Rest.kt` to the client5 classic API (context, entities, config, basic auth via client5 `AuthScope`/`BasicScheme`), keeping `Rest`, `Rest.Engine` and facade signatures unchanged while `HttpClientEngine`/`HttpClientRequest`/`HttpClientResponse` expose client5 types. Verification: `mvn compile` succeeds and `grep -n "org.apache.http\.\b" src/main/java/tornadofx/Rest.kt` shows no client4 imports.
- [x] 4.2 Port `RestClientTest` to client5 semantics 1:1 against the existing `com.sun.net.httpserver` fixture; add explicit basic-auth and request-timeout scenarios per the `rest-client` spec. Verification: `mvn test -Dtest=RestClientTest` passes including auth and timeout scenarios.

## 5. Remove OSGi support (design D7)

- [x] 5.1 Delete `src/main/java/tornadofx/osgi/**`, any OSGi test apps/fixtures referencing it, the Felix framework dependency (main + test), and `maven-bundle-plugin` with its `Bundle-Activator`/manifest wiring from `pom.xml`. Verification: `grep -ri "felix\|org.osgi\|osgi" pom.xml src/main src/test` returns no build/code references and `mvn clean verify` succeeds.
- [x] 5.2 Confirm the built jar no longer carries bundle manifest expectations (`META-INF/MANIFEST.MF` exists only as jar defaults). Verification: `unzip -p target/*.jar META-INF/MANIFEST.MF` contains no `Bundle-*` headers.

## 6. Integration verification & docs

- [x] 6.1 Full integration gate: `mvn clean verify` green on JDK 21. Verification: build log shows all test suites passing with the JUnit 5 provider.
- [x] 6.2 Verify `platform-compatibility` spec scenarios on the artifact: class files at Java 17 level (`javap -verbose` on a core class), no `sun/` packages, no `com.sun.javafx.scene.control` references in shipped classes. Verification: commands executed and outputs recorded in the change notes.
- [x] 6.3 Update `README.md` (supported JDK/JavaFX/Kotlin baselines, OpenJFX classifier override note, Rest types change, OSGi removal note for 1.7.x users) and add a `CHANGELOG.md` entry describing all **BREAKING** changes. Verification: docs state the new baselines and the migration notes listed in the proposal are present.

## Workflow follow-up

- Archive the change after the project's review requirements are satisfied.
- Verify the archived result.
