package tornadofx

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import javax.json.Json as JavaxJson
import javax.json.JsonValue
import java.io.StringReader

/**
 * The [Json] instance used by the kotlinx.serialization bridge by default.
 *
 * Ignores unknown keys, so responses carrying fields absent from the target
 * type decode without failing. Override this for a global configuration, or
 * pass an instance per call — every bridge function takes an optional [Json]
 * parameter.
 */
var restJson: Json = Json { ignoreUnknownKeys = true }

/**
 * Decodes this response body into a value of the type described by
 * [deserializer], using kotlinx.serialization.
 *
 * Consumes the response like the existing [Rest.Response.one]/[Rest.Response.list]
 * helpers do. Decode failures - malformed JSON, a body that does not match the
 * requested type, or an empty/blank body - surface as a [RestException] carrying
 * the request and response.
 */
fun <T> Rest.Response.value(deserializer: DeserializationStrategy<T>, json: Json = restJson): T {
    try {
        val content = text()

        if (content.isNullOrBlank())
            throw IllegalArgumentException("Response body is empty; cannot decode into ${deserializer.descriptor.serialName}")

        return json.decodeFromString(deserializer, content)
    } catch (t: Throwable) {
        throw RestException("Response decoding failed", request, this, t)
    } finally {
        consume()
    }
}

/**
 * Decodes this response body into a value of type [T], using kotlinx.serialization.
 *
 * `T` must be a `@Serializable` type (or carry a builtin serializer, like
 * `List<Person>` for a JSON array body); misuse fails at compile time.
 */
inline fun <reified T> Rest.Response.value(json: Json = restJson): T = value(serializer<T>(), json)

/**
 * Encodes [body] with the configured [Json] into a javax.json [JsonValue], so
 * the request goes through the unchanged engine entity path (no new entity
 * type is introduced).
 */
private fun <T> serializedBody(serializer: SerializationStrategy<T>, body: T, json: Json): JsonValue =
        JavaxJson.createReader(StringReader(json.encodeToString(serializer, body))).readValue()

/**
 * Sends [body], encoded to JSON with [json], as the payload of a POST request.
 *
 * The bridge's requests carry `Content-Type: application/json`; the wrapped
 * [processor] still runs and may override headers.
 */
fun <T> Rest.post(path: String, serializer: SerializationStrategy<T>, body: T, json: Json = restJson, processor: (Rest.Request) -> Unit = {}): Rest.Response =
        post(path, serializedBody(serializer, body, json)) { request ->
            request.addHeader("Content-Type", "application/json")
            processor(request)
        }

/**
 * Sends [body], encoded to JSON with [json], as the payload of a PUT request.
 *
 * The bridge's requests carry `Content-Type: application/json`; the wrapped
 * [processor] still runs and may override headers.
 */
fun <T> Rest.put(path: String, serializer: SerializationStrategy<T>, body: T, json: Json = restJson, processor: (Rest.Request) -> Unit = {}): Rest.Response =
        put(path, serializedBody(serializer, body, json)) { request ->
            request.addHeader("Content-Type", "application/json")
            processor(request)
        }

/**
 * Sends [body], encoded to JSON with [json], as the payload of a PATCH request.
 *
 * The bridge's requests carry `Content-Type: application/json`; the wrapped
 * [processor] still runs and may override headers.
 */
fun <T> Rest.patch(path: String, serializer: SerializationStrategy<T>, body: T, json: Json = restJson, processor: (Rest.Request) -> Unit = {}): Rest.Response =
        patch(path, serializedBody(serializer, body, json)) { request ->
            request.addHeader("Content-Type", "application/json")
            processor(request)
        }

/**
 * Sends [body], encoded to JSON with [json], as the payload of a DELETE request.
 *
 * The bridge's requests carry `Content-Type: application/json`; the wrapped
 * [processor] still runs and may override headers.
 */
fun <T> Rest.delete(path: String, serializer: SerializationStrategy<T>, body: T, json: Json = restJson, processor: (Rest.Request) -> Unit = {}): Rest.Response =
        delete(path, serializedBody(serializer, body, json)) { request ->
            request.addHeader("Content-Type", "application/json")
            processor(request)
        }

/**
 * Sends [body], a `@Serializable` value, encoded to JSON with [json], as the
 * payload of a POST request. Concrete `JsonValue`/`JsonModel`/`InputStream`
 * overloads take precedence for those argument types.
 */
inline fun <reified T> Rest.post(path: String, body: T, json: Json = restJson, noinline processor: (Rest.Request) -> Unit = {}): Rest.Response =
        post(path, serializer<T>(), body, json, processor)

/**
 * Sends [body], a `@Serializable` value, encoded to JSON with [json], as the
 * payload of a PUT request. Concrete `JsonValue`/`JsonModel`/`InputStream`
 * overloads take precedence for those argument types.
 */
inline fun <reified T> Rest.put(path: String, body: T, json: Json = restJson, noinline processor: (Rest.Request) -> Unit = {}): Rest.Response =
        put(path, serializer<T>(), body, json, processor)

/**
 * Sends [body], a `@Serializable` value, encoded to JSON with [json], as the
 * payload of a PATCH request. Concrete `JsonValue`/`JsonModel`/`InputStream`
 * overloads take precedence for those argument types.
 */
inline fun <reified T> Rest.patch(path: String, body: T, json: Json = restJson, noinline processor: (Rest.Request) -> Unit = {}): Rest.Response =
        patch(path, serializer<T>(), body, json, processor)

/**
 * Sends [body], a `@Serializable` value, encoded to JSON with [json], as the
 * payload of a DELETE request. Concrete `JsonValue`/`JsonModel` overloads take
 * precedence for those argument types.
 */
inline fun <reified T> Rest.delete(path: String, body: T, json: Json = restJson, noinline processor: (Rest.Request) -> Unit = {}): Rest.Response =
        delete(path, serializer<T>(), body, json, processor)
