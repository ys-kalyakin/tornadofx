# Tasks

## 1. Build setup

- [x] 1.1 Add `kotlinx.serialization.version` property and `kotlinx-serialization-core`/`kotlinx-serialization-json` as provided-scope dependencies; apply `org.jetbrains.kotlin.plugin.serialization` (aligned with `${kotlin.version}`) so test fixtures can use `@Serializable`; verify `./mvnw -Pheadless compile` resolves the dependencies and main sources stay warning-free

## 2. Bridge implementation

- [x] 2.1 Add response decoding in `src/main/java/tornadofx/Serialization.kt`: `value(deserializer)`/`value()` on `Rest.Response` decoding from `text()`, consuming the response in `finally`, wrapping failures (malformed JSON, wrong shape, empty/blank body) into `RestException` with request/response; verify with headless tests: object decode, array decode into list, malformed body raises `RestException` carrying the request, empty body fails
- [x] 2.2 Add `Json` configuration: module-level default instance (`ignoreUnknownKeys = true`) and per-call `Json` override on every bridge function; verify with headless tests: default tolerates unknown keys, per-call override rejecting unknown keys fails as configured
- [x] 2.3 Add request-side overloads for `post`/`put`/`patch`/`delete` taking a `@Serializable` body (explicit-serializer and reified variants): encode with the configured `Json`, parse into a javax.json `JsonValue`, send through the unchanged engine entity path, set `Content-Type: application/json` on the bridge's requests via the processor wrap; verify with headless tests: server receives the JSON-encoded body and the content-type header, existing engine behavior unchanged for non-bridge calls
- [x] 2.4 Verify overload resolution and compatibility: concrete `JsonValue`/`JsonModel`/`InputStream` overloads keep precedence, both call styles of each bridge overload compile and run, full existing suite green (`./mvnw -Pheadless test`) and main sources warning-free

## 3. Documentation

- [x] 3.1 Add a kotlinx.serialization section to the docs-site REST page (EN + RU, EN/RU parity via `docs/check-parity.py`) covering typed decode, typed bodies, `Json` configuration, the empty-body divergence from `one()`/`list()`, the opt-in dependency, and the consumer-side compiler plugin; verify parity check passes and snippets compile as written
- [x] 3.2 Update README feature list and the opt-in note; add the CHANGELOG entry under the unreleased 2.0.1 section; verify the pages appear in mkdocs nav
