# Design

## Context

`Rest` (`src/main/java/tornadofx/Rest.kt`) sends request bodies as `JsonModel`/`JsonValue`/`InputStream` — both engines (`HttpURLEngine`, `HttpClientEngine`) byte-serialize the first two via `toString()`, and reject any other entity type with `IllegalArgumentException`. Responses are consumed through `text()`, `one()`, `list()`, which parse javax.json structures and wrap parse failures into `RestException` with request/response context; `one()`/`list()` consume the response and return empty containers for empty bodies. The legacy conversion layer (`Json.kt`, `JsonModel`, `JsonConfig`, `JsonBuilder`) stays as-is. Notably, posting a `JsonValue` today sets no `Content-Type` header on the HttpURL engine.

Constraints inherited from existing specs: warning-free main sources on the pinned JDK 21 / Kotlin 2.x toolchain, no JDK-internal APIs, Java 17 bytecode; provided-scope pattern for optional integrations (Rest/HttpClient 5, coroutines bridge).

## Goals / Non-Goals

**Goals:**

- kotlinx.serialization as an opt-in conversion path for the Rest client: typed response decoding and typed request bodies.
- Failure and consumption semantics consistent with the existing `one()`/`list()` helpers.
- Configurable `Json` instance (global default + per call), default tolerant of unknown keys.
- Zero changes to existing `Rest`/`Json.kt` behavior.

**Non-Goals:**

- Suspend/await variants of `Rest` (coroutines follow-up).
- Other kotlinx formats (CBOR, ProtoBuf) — JSON only.
- Fixing or changing legacy JSON behavior (e.g. adding `Content-Type` to existing `JsonValue` posting) or removing the hand-rolled layer.
- `JsonModel`/ViewModel integration.

## Decisions

### D1: Ship in the core artifact with provided-scope dependencies

`kotlinx-serialization-core` and `kotlinx-serialization-json` (stable 1.11.x line, pinned via Maven properties) are `provided` dependencies; the bridge lives in a new `src/main/java/tornadofx/Serialization.kt` in package `tornadofx`.

- *Why*: the established opt-in pattern in this codebase (`Rest`/HttpClient 5, coroutines bridge) — single artifact, no transitive leakage, release/dokka/publishing pipeline untouched.
- *Alternatives considered*: separate `tornadofx-serialization` module — rejected for the same build-restructure cost as with coroutines; compile-scope dependency — violates the no-transitive-leakage contract.

### D2: API surface

```kotlin
// Response side
fun <T> Rest.Response.value(deserializer: DeserializationStrategy<T>): T
inline fun <reified T> Rest.Response.value(): T

// Request side (post/put/patch/delete; delete without the InputStream variant exists today)
fun <T> Rest.post(path: String, serializer: SerializationStrategy<T>, body: T, processor: (Request) -> Unit = {}): Response
inline fun <reified T> Rest.post(path: String, body: T, processor: (Request) -> Unit = {}): Response

// Configuration
var restJson: Json   // module-level default instance; every bridge function takes an optional Json override
```

- *Why*: explicit-serializer variants carry no reified magic and mirror kotlinx idioms; reified overloads are the ergonomic entry point. Misuse fails at compile time — `serializer<T>()` only resolves for `@Serializable` types (compiler-plugin-checked on the consumer side).
- *Naming*: `restJson` and method names are polish-level; final names decided in review, kept consistent across the surface.
- *Overload precedence*: the concrete `JsonValue`/`JsonModel`/`InputStream` overloads always win over the reified one for those argument types; a `String` body (no existing overload) would be captured by the reified variant and JSON-encoded — documented so raw-body users pass an `InputStream` explicitly.

### D3: Mechanics — reuse the existing engine paths

- **Request side**: encode the value with the configured `Json` into a JSON string, parse it into a javax.json `JsonValue`, and send it through the unchanged existing entity path; the bridge wraps the user `processor` to first set `Content-Type: application/json` on its own requests (today's `JsonValue` posting sets none — left untouched). No engine modifications, no behavioral drift for existing callers.
- **Response side**: decode from `text()`; wrap any decode failure into `RestException` with request/response attached; `consume()` in `finally`, mirroring `one()`/`list()`. An empty/blank body has no typed zero-value counterpart (unlike `one()`/`list()`'s empty containers), so it is treated as a decode failure — recorded here as the deliberate divergence.
- *Alternatives considered*: teaching engines a native `String` entity type — rejected: two engines to touch, header semantics differ between them, and entity dispatch is `when`-based public-adjacent behavior; passing the encoded string raw — same rejection plus no content-type guarantee.

### D4: Build — serialization compiler plugin for tests

Apply `org.jetbrains.kotlin.plugin.serialization` (version aligned with `${kotlin.version}`) in the pom so test fixtures can declare `@Serializable` models. Main sources contain no `@Serializable` classes, so the plugin is a no-op there; consumers still apply the plugin in their own builds (documented, not a framework concern).

### D5: Verification

Headless tests alongside the existing `RestClientTest` fixtures: object decode, array decode, malformed-body failure wrapping, empty-body failure, unknown-key tolerance by default vs per-call override, encoded body received as JSON with `application/json`, on both engines where the test infrastructure allows. Existing suite green; new code warning-free.

## Risks / Trade-offs

- [Reified overload captures unintended types (e.g. `String` bodies)] → concrete overloads keep precedence; documented; misuse is compile-time visible via missing serializers.
- [kotlinx-serialization version floor vs consumer Kotlin floor] → 1.11.x supports the current Kotlin 2.x line and imposes no floor beyond the documented Kotlin ≥ 2.3; consumers pick their own version (strict binary compatibility upstream).
- [`Content-Type` divergence between engines] → the bridge sets the header explicitly on its own requests; a round-trip test asserts it on the exercised engine(s).
- [Empty-body semantic differs from `one()`/`list()`] → deliberate, documented in docs and this design; typed decode cannot invent a zero value.
- [kotlinx-serialization absent at runtime when the bridge is touched] → fast `NoClassDefFoundError` at the call site, matching `Rest`/HttpClient 5 and coroutines behavior; documented.

## Migration Plan

Additive change; no migration. Rollback is reverting the commit: two provided dependencies, one new source file, one build plugin (test-only effect), docs. Docs updates (README feature list, docs-site REST page EN/RU, CHANGELOG under unreleased 2.0.1) ship with the change.

## Open Questions

None — D2 leaves only naming polish, which does not affect behavior or task structure.
