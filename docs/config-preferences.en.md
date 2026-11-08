# Config & preferences

TornadoFX gives every `Component` — views, fragments and controllers — a
`config` property backed by a properties file, plus a `preferences` helper for
the OS-native Java preferences store.

## The config delegate

`config` is a `ConfigProperties` (a `Properties` subclass with typed
accessors). It is loaded lazily from `<ClassName>.properties` inside the
application's config folder — `conf/` in the working directory by default,
customizable by overriding `configBasePath` on the `App`:

```kotlin
class SettingsView : View() {
    val theme = stringProperty(config.string("theme", "Light"))

    override val root = form {
        fieldset("Appearance") {
            field("Theme") {
                combobox(theme, observableListOf("Light", "Dark"))
            }
        }
    }

    init {
        theme.onChange { config["theme"] = it ?: "Light" }
    }
}
```

![The settings view rendering an Appearance fieldset with a theme combo box](images/config-settings-form.png)

### Writing values

The `set` operator accepts `String`, `Boolean`, `Double`, `Int` and anything
else; assigning `null` removes the key. Persist with `save()`:

```kotlin
config["autosave"] = true
config["retention"] = 30
config["window.width"] = 1024.0
config["last.folder"] = lastFolder?.absolutePath   // null removes the key
config.save()
```

### Reading values

Typed getters exist with and without defaults; a missing key without a
default returns `null`:

```kotlin
val autosave = config.boolean("autosave", false)
val retention = config.int("retention", 7)
val width = config.double("window.width", 800.0)
val lastFolder = config.string("last.folder")        // String?
```

Whole objects can be stored as JSON: anything implementing `JsonModel` is
serialized on write and restored with `jsonModel`/`jsonModels`:

```kotlin
config["last.user"] = user                  // JsonModel → JSON string
val lastUser = config.jsonModel<User>("last.user")
val recentUsers = config.jsonModels<User>("recent.users")
```

## OS preferences

`preferences` opens a node in the JVM's `java.util.prefs` store, which the OS
keeps per user (the registry on Windows, plists on macOS, `~/.java` on
Linux). Reads and writes go straight to the store — nothing to save:

```kotlin
preferences("com.example.myapp") {
    putBoolean("autosave", true)
    put("user.name", "alice")
}

preferences("com.example.myapp") {
    val autosave = getBoolean("autosave", false)
    val userName = get("user.name", "")
}
```

Omit the node name to use the package node of the application class. Use
`config` for structured, file-based settings you might want to inspect or
ship, and `preferences` for small per-user flags that should survive without
a config folder.
