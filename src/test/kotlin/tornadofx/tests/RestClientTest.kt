package tornadofx.tests

import com.sun.net.httpserver.HttpServer
import javafx.stage.Stage
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions as Assert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testfx.api.FxToolkit
import tornadofx.*
import java.net.InetSocketAddress
import javax.json.Json
import javax.json.JsonObject

class RestClientTest {
    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    companion object {
        lateinit var httpServer: HttpServer
        lateinit var api: Rest
        var itemPostPayload: JsonObject? = null

        @BeforeAll
        @JvmStatic
        fun startWebserverAndDefineAPI() {
            api = Rest().apply {
                baseURI = "http://localhost:8954"
            }

            httpServer = HttpServer.create(InetSocketAddress("localhost", 8954), 0).apply {
                createContext("/item") { exchange ->
                    exchange.sendResponseHeaders(200, 0)
                    itemPostPayload = Json.createReader(exchange.requestBody).readObject()
                    exchange.responseBody.use {
                        Json.createWriter(it).apply {
                            writeObject(JsonBuilder().add("success", true).build())
                        }
                    }
                    exchange.requestBody.close()
                }

                createContext("/category") { exchange ->
                    exchange.responseHeaders.add("Content-Type", "application/json")
                    exchange.sendResponseHeaders(200, 0)

                    exchange.responseBody.use {
                        Json.createWriter(it).apply {
                            val response = Json.createArrayBuilder()
                            response.add(JsonBuilder().add("name", "category 1").build())
                            response.add(JsonBuilder().add("name", "category 2").build())
                            writeArray(response.build())
                        }
                    }
                }

                createContext("/secure") { exchange ->
                    val auth = exchange.requestHeaders.getFirst("Authorization")
                    val expected = "Basic " + java.util.Base64.getEncoder().encodeToString("user:pass".toByteArray())
                    if (auth == expected) {
                        exchange.responseHeaders.add("Content-Type", "application/json")
                        exchange.sendResponseHeaders(200, 0)
                        exchange.responseBody.use {
                            Json.createWriter(it).writeObject(JsonBuilder().add("authenticated", true).build())
                        }
                    } else {
                        exchange.sendResponseHeaders(401, -1)
                    }
                }

                createContext("/slow") { exchange ->
                    Thread.sleep(3000)
                    exchange.sendResponseHeaders(200, -1)
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
    fun testGet() {
        val categories = api.get("category").list()
        Assert.assertEquals(2, categories.size)
    }

    @Test
    fun testPost() {
        val result = api.post("/item", JsonBuilder().add("name", "test").build()).one()
        Assert.assertNotNull(itemPostPayload)
        Assert.assertEquals(true, result.boolean("success"))
    }

    @Test
    fun testAutoclose() {
        val hangingResponse = api.get("/category")
        FX.runAndWait {
            Assert.assertEquals(1, Rest.ongoingRequests.size)
        }
        hangingResponse.use {
        }
        FX.runAndWait {
            Assert.assertEquals(0, Rest.ongoingRequests.size)
        }
    }

    @Test
    fun encodeQueryString() {
        val params = mapOf("name" to "Edvin Syse", "street" to "Møllegaten 12")
        Assert.assertEquals("?name=Edvin+Syse&street=M%C3%B8llegaten+12", params.queryString)
    }

    @Test
    fun testApacheEngineGet() {
        val engine = Rest().apply {
            baseURI = "http://localhost:8954"
            engine = HttpClientEngine(this)
        }
        val categories = engine.get("category").list()
        Assert.assertEquals(2, categories.size)
    }

    @Test
    fun testBasicAuthWithApacheEngine() {
        val secure = Rest().apply {
            baseURI = "http://localhost:8954"
            engine = HttpClientEngine(this)
            setBasicAuth("user", "pass")
        }
        val result = secure.post("/secure", JsonBuilder().add("name", "test").build()).one()
        Assert.assertEquals(true, result.boolean("authenticated"))
    }

    @Test
    fun testRequestTimeout() {
        val rest = Rest().apply {
            baseURI = "http://localhost:8954"
            engine = HttpClientEngine(this)
        }
        val exception = Assert.assertThrows(RestException::class.java) {
            rest.get("/slow") { it.properties["tornadofx.requestTimeoutMs"] = 300L }
        }
        Assert.assertNotNull(exception.cause, "The timeout failure should carry the underlying cause")
        FX.runAndWait { Assert.assertEquals(0, Rest.ongoingRequests.size) }
    }
}
