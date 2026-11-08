# i18n и сообщения

TornadoFX сам загружает Java resource bundle и делает переводы доступными
через одно свойство: `messages`. Бандлы читаются как **UTF-8**, поэтому
переводам не нужен экранирующий `native2ascii`.

## Глобальный бандл

Бандл с именем `Messages` в корне classpath — глобальный запасной:
`Messages.properties` плюс файлы для локалей вроде `Messages_ru.properties`.
Каждый `messages` компонента при отсутствии ключа обращается к нему:

```properties
# Messages.properties
app.title=Address book
greeting=Hello, {0}!
```

```properties
# Messages_ru.properties
app.title=Адресная книга
```

```kotlin
class MainView : View() {
    override val root = vbox {
        label(messages["app.title"])
        button(messages.format("greeting", "world"))
    }
}
```

![Главное вью с локализованным заголовком и кнопкой приветствия](images/i18n-messages-view.png)

`messages["key"]` ищет перевод, а `format(key, ...)` подставляет плейсхолдеры
`{0}`, `{1}`, ... через `MessageFormat`.

## Бандлы отдельных компонентов

Каждый класс компонента получает свой бандл, названный по классу. Для
`com.example.CustomerView` это файл `com/example/CustomerView.properties` —
удобно держать большие вью самодостаточными. Отсутствующие там ключи
отыскиваются в глобальном бандле, а ключ, не найденный нигде, отображается
как `[the.key]` вместо исключения `MissingResourceException`:

```kotlin
// CustomerView.properties определяет "customer.name"; в Messages.properties его нет
messages["customer.name"]   // "Customer name"
messages["no.such.key"]     // "[no.such.key]"
```

## Локаль

`FX.locale` решает, какие файлы бандлов загружать. Глобальный бандл
перезагружается при смене локали; меняйте её до создания компонентов или
берите язык с платформы:

```kotlin
FX.locale = Locale("ru")
// или взять язык ОС при старте:
FX.locale = Locale.getDefault()
```

## Свои имена бандлов

Имена бандлов создает `FX.messagesNameProvider`. Замена меняет, где бандлы
ищутся для каждого компонента — например, чтобы использовать простые имена
классов в отдельном пакете `i18n`:

```kotlin
FX.messagesNameProvider = { componentClass ->
    componentClass?.let { "i18n.${it.simpleName}" } ?: "i18n.Messages"
}
```
