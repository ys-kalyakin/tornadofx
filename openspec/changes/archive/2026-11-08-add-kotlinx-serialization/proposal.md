# Proposal

## Why

The Rest client's automatic JSON conversion runs on a hand-rolled reflection mapper (`Json.kt`) layered on `javax.json` (JSR-353, frozen since ~2018). Modern Kotlin projects standardize on `kotlinx.serialization` — compiler-checked codegen, actively maintained, multiplatform lineage. Today a consumer of this fork must hand-roll conversion between `Rest` responses and their own models or lean on the legacy reflection mapper, which is the most dated part of the framework.

## What Changes

- Add `kotlinx-serialization-core` and `kotlinx-serialization-json` as **provided-scope** dependencies (same opt-in pattern as `Rest`/HttpClient 5 and the coroutines bridge): consumers declare kotlinx.serialization themselves; consumers not using the new API are unaffected.
- Add a serialization bridge in the core artifact (new `tornadofx` source file), additive only:
  - **Response side**: decode a `Rest` response body into a value of a `@Serializable` type — consuming the response like `one()`/`list()` do, and wrapping decode failures into `RestException` with the request/response context for consistency with the existing JSON helpers.
  - **Request side**: send a `@Serializable` value as a JSON request body through `Rest.post`/`put`/`patch`/`delete`.
  - **Configuration**: the `kotlinx.serialization.json.Json` instance used by the bridge is configurable, with a sensible default (`ignoreUnknownKeys = true`), overridable globally and per call.
  - Consumers apply the kotlinx.serialization compiler plugin to their own build for their `@Serializable` classes (documented); the framework itself needs only the runtime library.
- The hand-rolled `Json`/`javax.json` layer and all existing `Rest` behavior (`one()`, `list()`, `JsonModel`, `JsonBuilder`) remain untouched.
- Non-goals (explicitly out of scope): suspend/await variants of the `Rest` client (coroutines follow-up), other kotlinx formats (CBOR, ProtoBuf), any change to or removal of the legacy JSON layer, ViewModel/`JsonModel` integration.

## Capabilities

### New Capabilities

- `serialization`: pluggable JSON conversion via kotlinx.serialization integrated with the Rest client lifecycle — decoding responses into `@Serializable` values, encoding request bodies, configuring the `Json` instance, and the opt-in dependency contract.

### Modified Capabilities

<!-- None: existing rest-client behavior (one(), list(), JsonModel, engines) is unchanged; the bridge is additive. -->

## Impact

- **Dependencies** (`pom.xml`): `kotlinx-serialization-core`/`kotlinx-serialization-json` (stable 1.11.x line) as provided-scope dependencies with version properties; the `org.jetbrains.kotlin.plugin.serialization` compiler plugin added to the build so test fixtures can declare `@Serializable` models (no-op for main sources, which contain none).
- **Code**: new file `src/main/java/tornadofx/Serialization.kt` (name indicative); no modification of `Rest.kt` or `Json.kt` behavior beyond additive extensions.
- **Tests**: headless-compatible tests decoding/encoding against the existing Rest test infrastructure (local endpoint fixtures), including failure wrapping and `Json` configuration override; existing suite must stay green.
- **Docs**: README feature list and docs-site REST page gain a kotlinx.serialization section (EN + RU); opt-in dependency and compiler-plugin requirements documented; CHANGELOG entry under unreleased 2.0.1.
- **Compatibility**: zero breaking changes — existing `Rest` signatures and behavior untouched; main sources stay warning-free; no new Kotlin floor for consumers.
