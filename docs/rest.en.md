# REST client

The `Rest` client wraps an HTTP engine with automatic JSON conversion and
dependency-injection friendly setup. In this fork the pluggable engine stack is
ported to **Apache HttpClient 5** — the public engine, request and response
classes expose HttpClient 5 types.

## Configuring the client

Inject a `Rest` and configure it (typically in an `App.init` or a
`Controller`):

```kotlin
import tornadofx.*

class MyController : Controller() {
    val api: Rest by inject()

    init {
        with (api) {
            baseURI = "http://contoso.com/api"
            setBasicAuth("user", "secret")
        }
    }
}
```

`setBasicAuth` applies credentials pre-emptively through an auth context
(`HttpURLBasicAuthContext` / `DigestAuthContext`); basic auth is applied before
the server asks for it, so requests do not need a challenge round-trip.

## Requests

Methods for GET, POST, PUT, PATCH, DELETE and HEAD accept a path (resolved
against `baseURI`), optional data (`JsonValue`, `JsonModel` or `InputStream`)
and a request processor:

```kotlin
fun loadCustomers(): ObservableList<Customer> =
    api.get("customers").list().toModel()

fun saveCustomer(customer: Customer) =
    api.post("customers", customer).one()

fun deleteCustomer(id: Int) =
    api.delete("customers/$id")
```

## Response

The response exposes status, headers, body text and parsed JSON without extra
setup:

```kotlin
val response = api.get("customers/1")
val status = response.ok            // 2xx?
val body = response.text()
val headers = response.header("Content-Type")
val json = response.one()           // JsonObject
val array = response.list()         // JsonArray
```

Use `.list().toModel<Customer>()` / `.one().toModel<Customer>()` to convert
JSON straight into `JsonModel` instances.

## Per-request timeout

Configure a timeout on a single request through the request processor:

```kotlin
api.get("/slow") { request ->
    request.properties["tornadofx.requestTimeoutMs"] = 300L
}
```

When the endpoint does not respond within the timeout the request fails
(`RestException` carrying the underlying cause) instead of hanging.

## Engines

The engine abstraction is pluggable. The default engine is `HttpURLEngine`
(JDK `HttpURLConnection`). A first-class engine built on Apache HttpClient 5
ships with the framework:

```kotlin
val api = Rest().apply {
    baseURI = "http://localhost:8954"
    engine = HttpClientEngine(this)
    setBasicAuth("user", "pass")
}
```

Applications that need the client5 feature set (connection pooling, custom
TLS configuration, redirect strategies) use `HttpClientEngine`; the `Rest`
facade and all other operations stay unchanged when the engine is swapped.

## JSON helpers

`JsonModel` converts to and from JSON ([Properties & binding](properties.md)):

```kotlin
class Customer : JsonModel {
    val idProperty = SimpleIntegerProperty()
    var id by idProperty
    val nameProperty = SimpleStringProperty()
    var name by nameProperty

    override fun updateModel(json: JsonObject) {
        with(json) {
            id = int("id") ?: 0
            name = string("name")
        }
    }

    override fun toJSON(json: JsonBuilder) {
        with(json) {
            add("id", id)
            add("name", name)
        }
    }
}
```

Load customers in the background and update a TableView on the UI thread
([Async](async.md)):

```kotlin
runAsync {
    controller.loadCustomers()
} ui {
    customerTable.items = it
}
```

## Typed JSON with kotlinx.serialization

Beyond the `JsonModel`/`JsonValue` layer above, the `Rest` client integrates
with [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization):
responses decode into `@Serializable` values and request bodies encode from
them:

```kotlin
@Serializable
data class PersonDto(val name: String, val age: Int = 0)

val person = api.get("people/1").value<PersonDto>()       // object body
val people = api.get("people").value<List<PersonDto>>()   // array body

api.post("people", PersonDto("Edvin", 30))                // JSON-encoded body
api.put("people/1", PersonDto("Edvin", 31))
```

Every bridge function also comes in an explicit-serializer style, which works
for types the reified shortcut cannot express:

```kotlin
val person = api.get("people/1").value(PersonDto.serializer())
api.post("people", PersonDto.serializer(), PersonDto("Edvin", 30))
```

Decode failures — malformed JSON or a body that does not match the target
type — throw `RestException` carrying the request and response, consistent
with `one()`/`list()`. An **empty body fails to decode**: unlike
`one()`/`list()`, which return empty containers, typed decoding has no zero
value to invent.

The `Json` instance ignores unknown keys by default and is configurable
globally and per call:

```kotlin
restJson = Json { ignoreUnknownKeys = false }              // global default

api.get("people/1").value<PersonDto>(
    Json { ignoreUnknownKeys = false })                    // per call
```

### Opt-in dependency

kotlinx-serialization ships as a **provided** dependency, like HttpClient 5 and
kotlinx-coroutines: declare it in your own build to use the bridge, while
consumers who do not are unaffected:

```xml
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-serialization-json</artifactId>
    <version>1.11.0</version>
</dependency>
```

Your `@Serializable` classes are compiled by the kotlinx.serialization compiler
plugin — apply it in your own build: `org.jetbrains.kotlin.plugin.serialization`
with Gradle or `kotlin-maven-serialization` with Maven, version aligned with
your Kotlin version.
