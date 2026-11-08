# serialization — Delta Specification

## Purpose

Lets consumers convert between the Rest client and their own data classes using kotlinx.serialization instead of the legacy reflection-based JSON layer: responses decode into `@Serializable` values, request bodies encode from them, and the `Json` instance is configurable.

## ADDED Requirements

### Requirement: Decode a response body into a serializable value

A `Rest` response body SHALL be decodable into a value of a `@Serializable` type. Decoding consumes the response like the existing `one()`/`list()` helpers do. A JSON object body SHALL decode into the requested type; a JSON array body SHALL decode into a list of the requested type.

#### Scenario: Object body decodes into the requested type

- **WHEN** a response whose body is a JSON object is decoded into a `@Serializable` type
- **THEN** the result is an instance of that type populated from the JSON and the response is consumed

#### Scenario: Array body decodes into a list

- **WHEN** a response whose body is a JSON array is decoded into a list of a `@Serializable` type
- **THEN** the result is a list with one element per JSON array entry and the response is consumed

### Requirement: Decode failures carry request context

If the response body cannot be decoded — malformed JSON or a body that does not match the requested type — the failure SHALL surface as a `RestException` carrying the request and response, consistent with how the existing `one()`/`list()` helpers report parse failures.

#### Scenario: Malformed body raises RestException

- **WHEN** a response whose body is not valid JSON is decoded
- **THEN** a `RestException` is thrown and it exposes the originating request and response

### Requirement: Encode serializable request bodies

`Rest.post`/`put`/`patch`/`delete` SHALL accept a `@Serializable` value as the request body, serialized to JSON and sent with an `application/json` content type.

#### Scenario: Serializable body arrives as JSON at the server

- **WHEN** a request is sent with a `@Serializable` value as its body
- **THEN** the server receives the value serialized as JSON with an `application/json` content type

### Requirement: Configurable Json instance

The `Json` instance used by the bridge SHALL default to one that ignores unknown keys, and SHALL be overridable globally and per call.

#### Scenario: Default tolerates unknown keys

- **WHEN** a response body contains keys absent from the target `@Serializable` type and the default `Json` instance is in effect
- **THEN** decoding succeeds and the unknown keys are ignored

#### Scenario: Per-call override applies

- **WHEN** a decode call passes an explicit `Json` instance configured to fail on unknown keys and the body contains an unknown key
- **THEN** decoding fails even though the default instance would have tolerated it

### Requirement: Serialization support is an opt-in dependency

The serialization bridge SHALL be shipped in the core artifact with kotlinx-serialization as a provided-scope dependency: consumers who use it declare kotlinx-serialization (and apply the kotlinx.serialization compiler plugin for their own `@Serializable` classes), and consumers who do not are unaffected.

#### Scenario: Classic JSON usage without kotlinx-serialization on the classpath

- **WHEN** a consumer uses only the legacy JSON layer and `Rest` without declaring kotlinx-serialization
- **THEN** no kotlinx-serialization class is loaded and behavior is unchanged

#### Scenario: Bridge usage with kotlinx-serialization declared

- **WHEN** a consumer declares kotlinx-serialization and applies its compiler plugin, then uses the bridge
- **THEN** decoding and encoding work against the kotlinx-serialization version the consumer provides

### Requirement: Existing Rest and JSON behavior remains unchanged

Adding the serialization bridge MUST NOT change the signatures or observable behavior of the existing `Rest` client or the legacy JSON layer (`one()`, `list()`, `JsonModel`, `JsonBuilder`, `JsonConfig`): all existing usages SHALL compile and behave identically.

#### Scenario: Existing Rest code compiles and behaves identically

- **WHEN** code written against the pre-change `Rest` and JSON APIs is compiled and run against the new build
- **THEN** its behavior is identical to the previous release, as covered by the existing test suite
