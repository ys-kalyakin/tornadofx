# REST-клиент

`Rest` оборачивает HTTP-движок с автоматической конвертацией JSON и удобной
интеграцией с DI. В этом форке стек подключаемых движков перенесён на
**Apache HttpClient 5** — публичные классы движка, запроса и ответа
используют типы HttpClient 5.

## Настройка клиента

Внедрите `Rest` и настройте его (обычно в `App.init` или `Controller`):

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

`setBasicAuth` применяет учётные данные превентивно через auth-контекст
(`HttpURLBasicAuthContext` / `DigestAuthContext`); basic auth отправляется до
запроса сервера, поэтому round-trip на challenge не нужен.

## Запросы

Методы GET, POST, PUT, PATCH, DELETE и HEAD принимают путь (разрешается
относительно `baseURI`), опциональные данные (`JsonValue`, `JsonModel` или
`InputStream`) и процессор запроса:

```kotlin
fun loadCustomers(): ObservableList<Customer> =
    api.get("customers").list().toModel()

fun saveCustomer(customer: Customer) =
    api.post("customers", customer).one()

fun deleteCustomer(id: Int) =
    api.delete("customers/$id")
```

## Ответ

Ответ выставляет статус, заголовки, текст тела и разобранный JSON без
дополнительной настройки:

```kotlin
val response = api.get("customers/1")
val status = response.ok            // 2xx?
val body = response.text()
val headers = response.header("Content-Type")
val json = response.one()           // JsonObject
val array = response.list()         // JsonArray
```

Используйте `.list().toModel<Customer>()` / `.one().toModel<Customer>()`,
чтобы превратить JSON сразу в экземпляры `JsonModel`.

## Таймаут на запрос

Таймаут одного запроса задаётся через процессор запроса:

```kotlin
api.get("/slow") { request ->
    request.properties["tornadofx.requestTimeoutMs"] = 300L
}
```

Если эндпоинт не отвечает в пределах таймаута, запрос завершается ошибкой
(`RestException` с лежащей в основе причиной), а не зависает.

## Движки

Движок — подключаемый. Движок по умолчанию — `HttpURLEngine` (JDK
`HttpURLConnection`). С фреймворком поставляется первоклассный движок на
Apache HttpClient 5:

```kotlin
val api = Rest().apply {
    baseURI = "http://localhost:8954"
    engine = HttpClientEngine(this)
    setBasicAuth("user", "pass")
}
```

Приложениям, которым нужен функционал client5 (пул соединений, кастомная
конфигурация TLS, стратегии редиректов), следует использовать
`HttpClientEngine`; фасад `Rest` и все остальные операции не меняются при
подмене движка.

## JSON-хелперы

`JsonModel` конвертируется в обе стороны ([Свойства и биндинги](properties.md)):

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

Загрузите список в фоне и обновите TableView в UI-потоке
([Асинхронность](async.md)):

```kotlin
runAsync {
    controller.loadCustomers()
} ui {
    customerTable.items = it
}
```
