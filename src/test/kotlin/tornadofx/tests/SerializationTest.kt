package tornadofx.tests

import com.sun.net.httpserver.HttpServer
import javafx.stage.Stage
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions as Assert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testfx.api.FxToolkit
import tornadofx.*
import java.net.InetSocketAddress

@Serializable
data class PersonDto(val name: String, val age: Int = 0)

@Serializable
data class EchoDto(val contentType: String, val body: PersonDto)

class SerializationTest {
    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    companion object {
        lateinit var httpServer: HttpServer
        lateinit var api: Rest

        @BeforeAll
        @JvmStatic
        fun startWebserverAndDefineAPI() {
            api = Rest().apply {
                baseURI = "http://localhost:8955"
            }

            httpServer = HttpServer.create(InetSocketAddress("localhost", 8955), 0).apply {
                createContext("/person") { exchange ->
                    exchange.responseHeaders.add("Content-Type", "application/json")
                    exchange.sendResponseHeaders(200, 0)
                    exchange.responseBody.use {
                        javax.json.Json.createWriter(it).writeObject(
                                javax.json.Json.createObjectBuilder().add("name", "Edvin").add("age", 30).build())
                    }
                }

                createContext("/people") { exchange ->
                    exchange.responseHeaders.add("Content-Type", "application/json")
                    exchange.sendResponseHeaders(200, 0)
                    exchange.responseBody.use {
                        val people = javax.json.Json.createArrayBuilder()
                        people.add(javax.json.Json.createObjectBuilder().add("name", "Edvin").add("age", 30))
                        people.add(javax.json.Json.createObjectBuilder().add("name", "Thomas"))
                        javax.json.Json.createWriter(it).writeArray(people.build())
                    }
                }

                createContext("/extra") { exchange ->
                    exchange.responseHeaders.add("Content-Type", "application/json")
                    exchange.sendResponseHeaders(200, 0)
                    exchange.responseBody.use {
                        javax.json.Json.createWriter(it).writeObject(
                                javax.json.Json.createObjectBuilder()
                                        .add("name", "Edvin").add("age", 30).add("unknown", "ignored").build())
                    }
                }

                createContext("/malformed") { exchange ->
                    exchange.responseHeaders.add("Content-Type", "application/json")
                    exchange.sendResponseHeaders(200, 0)
                    exchange.responseBody.use { it.write("definitely not json".toByteArray()) }
                }

                createContext("/empty") { exchange ->
                    exchange.sendResponseHeaders(200, -1)
                }

                createContext("/echo") { exchange ->
                    val contentType = exchange.requestHeaders.getFirst("Content-Type") ?: ""
                    val body = javax.json.Json.createReader(exchange.requestBody).readValue()
                    exchange.responseHeaders.add("Content-Type", "application/json")
                    exchange.sendResponseHeaders(200, 0)
                    exchange.responseBody.use {
                        javax.json.Json.createWriter(it).writeObject(
                                javax.json.Json.createObjectBuilder()
                                        .add("contentType", contentType)
                                        .add("body", body)
                                        .build())
                    }
                }
                start()
            }
        }

        @AfterAll
        @JvmStatic
        fun stopWebserver() {
            httpServer.stop(0)
        }
    }

    @Test
    fun objectBodyDecodesIntoRequestedType() {
        val person = api.get("/person").value<PersonDto>()
        Assert.assertEquals("Edvin", person.name)
        Assert.assertEquals(30, person.age)
    }

    @Test
    fun decodingConsumesTheResponse() {
        api.get("/person").use { it.value<PersonDto>() }
        FX.runAndWait {
            Assert.assertEquals(0, Rest.ongoingRequests.size)
        }
    }

    @Test
    fun arrayBodyDecodesIntoList() {
        val people = api.get("/people").value<List<PersonDto>>()
        Assert.assertEquals(2, people.size)
        Assert.assertEquals("Edvin", people[0].name)
        Assert.assertEquals(0, people[1].age)
    }

    @Test
    fun malformedBodyRaisesRestExceptionCarryingRequestAndResponse() {
        val exception = Assert.assertThrows(RestException::class.java) {
            api.get("/malformed").value<PersonDto>()
        }
        Assert.assertNotNull(exception.request)
        Assert.assertNotNull(exception.response)
    }

    @Test
    fun emptyBodyFailsToDecode() {
        Assert.assertThrows(RestException::class.java) {
            api.get("/empty").value<PersonDto>()
        }
    }

    @Test
    fun defaultJsonToleratesUnknownKeys() {
        val person = api.get("/extra").value<PersonDto>()
        Assert.assertEquals("Edvin", person.name)
        Assert.assertEquals(30, person.age)
    }

    @Test
    fun perCallJsonOverrideRejectsUnknownKeys() {
        val strict = Json { ignoreUnknownKeys = false }
        Assert.assertThrows(RestException::class.java) {
            api.get("/extra").value<PersonDto>(strict)
        }
    }

    @Test
    fun serializablePostBodyArrivesAsJsonAtServer() {
        val echoed = api.post("/echo", PersonDto("Edvin", 30)).value<EchoDto>()
        Assert.assertEquals("Edvin", echoed.body.name)
        Assert.assertEquals(30, echoed.body.age)
        Assert.assertTrue(echoed.contentType.startsWith("application/json"), echoed.contentType)
    }

    @Test
    fun explicitSerializerVariantPosts() {
        val echoed = api.post("/echo", PersonDto.serializer(), PersonDto("Thomas", 45)).value<EchoDto>()
        Assert.assertEquals("Thomas", echoed.body.name)
        Assert.assertEquals(45, echoed.body.age)
    }

    @Test
    fun serializablePutDeleteBodiesArriveAsJson() {
        Assert.assertEquals("Edvin", api.put("/echo", PersonDto("Edvin", 30)).value<EchoDto>().body.name)
        Assert.assertEquals("Edvin", api.delete("/echo", PersonDto("Edvin", 30)).value<EchoDto>().body.name)
    }

    @Test
    fun serializablePatchBodyArrivesAsJsonViaApacheEngine() {
        // HttpURLConnection (the default HttpURLEngine) rejects PATCH outright,
        // so PATCH runs on the HttpClient 5 engine, like the rest of the suite.
        val apache = Rest().apply {
            baseURI = "http://localhost:8955"
            engine = HttpClientEngine(this)
        }
        Assert.assertEquals("Edvin", apache.patch("/echo", PersonDto("Edvin", 30)).value<EchoDto>().body.name)
    }

    @Test
    fun perCallJsonAppliesToRequestBodyEncoding() {
        // Default Json omits defaults: the server sees no "age" key, and the
        // decoded body falls back to the DTO's default of 0.
        Assert.assertEquals(0, api.post("/echo", PersonDto("Omitted")).value<EchoDto>().body.age)

        // A per-call Json that encodes defaults sends the "age" key explicitly.
        val encodeDefaults = Json { encodeDefaults = true }
        Assert.assertEquals(0, api.post("/echo", PersonDto("Included"), encodeDefaults).value<EchoDto>().body.age)
        val echoed = api.post("/echo", PersonDto("Included"), encodeDefaults) { request ->
            Assert.assertNotNull(request.getHeader("Content-Type"))
        }.value<EchoDto>()
        Assert.assertEquals("Included", echoed.body.name)
    }

    @Test
    fun serializablePostBodyWithApacheEngine() {
        val apache = Rest().apply {
            baseURI = "http://localhost:8955"
            engine = HttpClientEngine(this)
        }
        val echoed = apache.post("/echo", PersonDto("Edvin", 30)).value<EchoDto>()
        Assert.assertEquals("Edvin", echoed.body.name)
        Assert.assertTrue(echoed.contentType.startsWith("application/json"), echoed.contentType)
    }

    @Test
    fun explicitDeserializerStyleDecodes() {
        val person = api.get("/person").value(PersonDto.serializer())
        Assert.assertEquals("Edvin", person.name)

        val people = api.get("/people").value(ListSerializer(PersonDto.serializer()))
        Assert.assertEquals(2, people.size)
    }

    @Test
    fun explicitSerializerStyleCoversEveryVerb() {
        Assert.assertEquals("Edvin", api.put("/echo", PersonDto.serializer(), PersonDto("Edvin", 30)).value<EchoDto>().body.name)
        Assert.assertEquals("Edvin", api.delete("/echo", PersonDto.serializer(), PersonDto("Edvin", 30)).value<EchoDto>().body.name)

        val apache = Rest().apply {
            baseURI = "http://localhost:8955"
            engine = HttpClientEngine(this)
        }
        Assert.assertEquals("Edvin", apache.patch("/echo", PersonDto.serializer(), PersonDto("Edvin", 30)).value<EchoDto>().body.name)
    }

    @Test
    fun concreteOverloadsKeepPrecedence() {
        // A JsonValue body keeps taking the concrete JsonValue overload...
        val jsonValue = JsonBuilder().add("kind", "jsonvalue").build()
        Assert.assertEquals("jsonvalue", api.post("/echo", jsonValue).one().getJsonObject("body").getString("kind"))

        // ...a JsonModel keeps taking the concrete JsonModel overload (toJSON output)...
        val customer = Customer().apply { name = "model" }
        Assert.assertEquals("model", api.post("/echo", customer).one().getJsonObject("body").getString("name"))

        // ...and an InputStream keeps taking the concrete InputStream overload: raw
        // bytes, no JSON re-encoding.
        val raw = "{\"kind\":\"stream\"}".toByteArray().inputStream()
        Assert.assertEquals("stream", api.post("/echo", raw).one().getJsonObject("body").getString("kind"))
    }
}
