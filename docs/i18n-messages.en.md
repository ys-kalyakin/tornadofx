# i18n & messages

TornadoFX loads Java resource bundles for you and makes translations
reachable with one property: `messages`. Bundles are read as **UTF-8**, so
translations need no `native2ascii` escaping.

## The global bundle

A bundle named `Messages` at the classpath root is the global fallback:
`Messages.properties` plus per-locale files like `Messages_ru.properties`.
Every component's `messages` falls back to it for missing keys:

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

![The main view showing the localized title label and greeting button](images/i18n-messages-view.png)

`messages["key"]` looks up a translation, and `format(key, ...)` fills
`{0}`, `{1}`, ... placeholders through `MessageFormat`.

## Per-component bundles

Each component class gets its own bundle named after the class. For
`com.example.CustomerView` the file is `com/example/CustomerView.properties` —
handy for keeping large views self-contained. Keys missing there fall back to
the global bundle, and a key missing everywhere renders as `[the.key]`
instead of throwing `MissingResourceException`:

```kotlin
// CustomerView.properties defines "customer.name"; Messages.properties does not
messages["customer.name"]   // "Customer name"
messages["no.such.key"]     // "[no.such.key]"
```

## Locale

`FX.locale` decides which bundle files are loaded. The global bundle reloads
when the locale changes; switch it before components are created, or set it
from the platform:

```kotlin
FX.locale = Locale("ru")
// or pick up the OS language at startup:
FX.locale = Locale.getDefault()
```

## Custom bundle naming

Bundle names are produced by `FX.messagesNameProvider`. Replacing it changes
where bundles are looked up for every component — for example, to use simple
class names in a dedicated `i18n` package:

```kotlin
FX.messagesNameProvider = { componentClass ->
    componentClass?.let { "i18n.${it.simpleName}" } ?: "i18n.Messages"
}
```
