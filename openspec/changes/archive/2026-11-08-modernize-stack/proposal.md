# Proposal: Modernize TornadoFX to a new stack (JDK 21 / JavaFX 21 / Kotlin 2)

## Why

TornadoFX 1.7.x is frozen on a Java-8-era stack that can no longer be built or supported: it targets JDK 1.8 with no JavaFX dependency (assuming the JavaFX 8 bundled in the JDK), compiles with Kotlin 1.3.61, and depends on JDK/JavaFX internals (`sun.net.www.protocol.css`, `com.sun.glass`, `com.sun.javafx.tk.Toolkit`, `com.sun.javafx.scene.control.skin.*`) that do not compile on modern JDKs and JavaFX. The build also pulls plugins from jcenter, which shut down in 2021. The project cannot accept bug fixes or run on any current JVM without a stack modernization.

## What Changes

- **BREAKING** — Platform baseline moves to the modern stack: build toolchain JDK 21, framework compiled against OpenJFX 21 as an explicit dependency (`org.openjfx:javafx-*`), Kotlin 2.x (K2). JavaFX 8 / JDK 8 support is dropped.
- **BREAKING** — REST client (`tornadofx.Rest`) migrates from Apache HttpClient 4 to HttpClient 5; public classes `HttpClientEngine`, `HttpClientRequest`, `HttpClientResponse` change their Apache types.
- **BREAKING** — The `sun.net.www.protocol.css.Handler` package is removed (split-package conflict with `java.base` on JDK 9+); the `css://` URL stream handler is relocated into a `tornadofx` package.
- Internal-API usage in main sources is replaced with public JavaFX 21 equivalents: `com.sun.glass.ui.Application.isEventThread()` → `Platform.isFxApplicationThread()`, `com.sun.javafx.tk.Toolkit` nested-loop calls → `Platform.enterNestedEventLoop`/`exitNestedEventLoop`, `com.sun.javafx.scene.control.skin/behavior` → public `javafx.scene.control.skin` and the JavaFX 17+ behavior API, `com.sun.javafx.application.PlatformImpl` (OSGi) → public startup API where possible, unused `com.sun.xml.internal.bind` import (ListMenu) deleted.
- **BREAKING** — OSGi support is removed: the `tornadofx.osgi` package, the Felix framework dependency, the bundle plugin/manifest configuration and the OSGi-specific CSS URL handler service are deleted. OSGi consumers stay on the 1.7.x line.
- Test stack migrates from JUnit 4.12 to JUnit 5 (surefire 3.x) and TestFX 4.0.4-alpha → 4.0.18.
- Build infrastructure: jcenter removed in favor of Maven Central; Maven plugins (compiler, jar, source, javadoc, gpg, release) and Dokka upgraded; `kotlin-stdlib-jdk8` → `kotlin-stdlib` (jdk8 artifact is merged into stdlib since Kotlin 1.8).
- `javax.json` (Glassfish) stays as the JSON API — it is deeply embedded in the public `Json.kt` API and out of scope here.

## Capabilities

### New Capabilities

- `platform-compatibility`: Supported JDK/JavaFX baselines for building and consuming TornadoFX — bytecode target, OpenJFX as explicit dependency, and the rule that main sources compile without JDK-internal (`sun.*`, `com.sun.glass`, `com.sun.xml.internal`) or relocated-internal JavaFX APIs.
- `css-stylesheet-loading`: The type-safe stylesheet pipeline on the modular JDK — `css://` URL handler registration and loading of programmatic stylesheets.
- `rest-client`: The `Rest` client engine behavior on Apache HttpClient 5 — engine configuration, requests, responses, auth and timeouts parity with the 1.7.x feature set.
- `ui-controls`: Custom controls and skins (DataGrid, SqueezeBox, AutoCompleteComboBox, ListMenu) built on the JavaFX 21 public skin/behavior APIs.

### Modified Capabilities

(none — the project has no existing specs)

## Impact

- **Build**: `pom.xml` (JDK, JavaFX, Kotlin, all dependency versions, repositories, plugins), local toolchain/CI must provide JDK 21.
- **Public API (breaking)**: `tornadofx.Rest` Apache types; `sun.net.www.protocol.css` package gone; published bytecode target raised.
- **Main sources**: ~10 files touching JDK/JavaFX internals (`Async.kt`, `CSS.kt`, `DataGrid.kt`, `SqueezeBox.kt`, `AutoCompleteComboBoxSkin.kt`, `ListMenu.kt`, `ItemControls.kt`, `Nodes.kt`) plus deletion of `sun/net/www/protocol/css/Handler.kt` and the whole `tornadofx/osgi` package (~12 files).
- **Tests**: ~28 test files migrate JUnit 4 → 5; TestFX upgraded; REST tests re-targeted to HttpClient 5.
- **Consumers**: apps must run JDK 17+ (bytecode) with JavaFX 21 on the classpath/module path; OSGi consumers must stay on the 1.7.x line.
