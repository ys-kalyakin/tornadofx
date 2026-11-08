# Migration from 1.7.20

This fork modernizes the framework onto the current Java stack — a deliberate
compatibility break from the 1.7.20 release. Consumers who cannot migrate
should stay on 1.7.20. Every breaking change below mirrors the
[project change log](https://github.com/ys-kalyakin/tornadofx/blob/master/CHANGELOG.md).

## New Maven coordinates (2.0.0)

- The fork publishes as **`io.github.ys-kalyakin:tornadofx`** (first release:
  `2.0.0`) instead of `no.tornado:tornadofx`. The artifactId and all package
  names (`tornadofx.*`) are unchanged — only the Maven coordinates move.
- Update your dependency declarations: `<groupId>io.github.ys-kalyakin</groupId>`
  in Maven, `io.github.ys-kalyakin:tornadofx:2.0.0` in Gradle
  (see [Getting started](getting-started.md#maven)).
- Old 1.7.x releases stay available on Maven Central under `no.tornado:tornadofx`;
  this fork publishes no versions under the old coordinates.

## Platform baseline

- The framework is built with a **JDK 21 toolchain**, compiled against
  **OpenJFX 21** (declared as Maven dependencies with platform classifiers) and
  published as **Java 17 bytecode**.
- **JavaFX 8 / JDK 8 support is dropped**: consumers need a JDK 17+ runtime
  with JavaFX 21 on the classpath. There is no JDK-bundled JavaFX to rely on —
  declare the `org.openjfx` modules you use (see
  [Getting started](getting-started.md#maven)).

## Kotlin 2.x (K2)

- The framework is built with Kotlin 2 and `jvmTarget` 17.
- Consumers must build with **Kotlin 2.3 or newer** and `jvmTarget` 17: the
  framework ships Kotlin metadata 2.4.0, and a Kotlin compiler reads metadata
  up to one minor version ahead.
- Update your IDE Kotlin plugin, configure `jvmTarget = "17"` in Maven/Gradle,
  and do a full rebuild after upgrading (clean caches and restart IDEA if
  something does not inline).

## REST client ported to HttpClient 5

- `Rest` is ported from Apache HttpClient 4 to **HttpClient 5**
  (`org.apache.httpcomponents.client5:httpclient5`, `provided` scope — declare
  it in your application if you use the Apache engine).
- The public `HttpClientEngine`, `HttpClientRequest` and `HttpClientResponse`
  classes now expose **client5 types** — code that touched client4 types must
  switch to the corresponding `org.apache.hc.client5` classes.
- Basic auth is applied **pre-emptively** through an `AuthContext`: HttpClient
  5.6 no longer honors the old authCache preemptive recipe, so setting
  credentials via `setBasicAuth()` works without a challenge round-trip.
- The digest `AuthContext` now runs for the Apache engine as well.
- Per-request timeouts are configured via
  `request.properties["tornadofx.requestTimeoutMs"]` ([REST client](rest.md#per-request-timeout)).

## OSGi support removed

- The `tornadofx.osgi` package, the Felix dependency, the bundle manifest
  configuration and `FX.osgiDebuggerShortcut` are gone.
- `FX.osgiAvailable` is deprecated and always returns `false`.
- **OSGi consumers must stay on the 1.7.x line** (1.7.20).

## CSS URL handler relocated

- The type-safe stylesheet URL handler moved from `sun.net.www.protocol.css`
  to **`tornadofx.css`**. The framework registers it automatically; only
  applications that referenced the old package directly must update.

## ViewModel.bind facade types under Kotlin 2

- With K2's more precise type inference, binding a producer that returns an
  `ObjectProperty` now yields an **object facade** (matching the inferred
  result type) instead of a type-specific facade. Code relying on the old
  type-specific facade of such a binding must adapt (bind the typed property
  overload instead).

## Stay on 1.7.20 when...

- You deploy OSGi bundles.
- You cannot move to a JDK 17+ runtime or JavaFX 21.
- You cannot raise your Kotlin compiler to 2.3+.

The upstream 1.7.x documentation (the original
[TornadoFX wiki](https://github.com/edvin/tornadofx/wiki)) describes that
line.
