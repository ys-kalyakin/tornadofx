# Конфигурация и настройки

TornadoFX дает каждому `Component` — вью, фрагментам и контроллерам — свойство
`config`, которое опирается на properties-файл, плюс хелпер `preferences` для
нативного хранилища настроек ОС.

## Делегат config

`config` — это `ConfigProperties` (наследник `Properties` с типизированными
аксессорами). Он лениво загружается из `<ClassName>.properties` в папке
конфигурации приложения — по умолчанию `conf/` в рабочем каталоге, меняется
переопределением `configBasePath` в `App`:

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

![Вью настроек: fieldset Appearance с комбобоксом темы](images/config-settings-form.png)

### Запись значений

Оператор `set` принимает `String`, `Boolean`, `Double`, `Int` и что угодно
еще; присвоение `null` удаляет ключ. Сохранение — через `save()`:

```kotlin
config["autosave"] = true
config["retention"] = 30
config["window.width"] = 1024.0
config["last.folder"] = lastFolder?.absolutePath   // null удаляет ключ
config.save()
```

### Чтение значений

Типизированные геттеры есть с дефолтом и без; отсутствующий ключ без дефолта
возвращает `null`:

```kotlin
val autosave = config.boolean("autosave", false)
val retention = config.int("retention", 7)
val width = config.double("window.width", 800.0)
val lastFolder = config.string("last.folder")        // String?
```

Целые объекты можно хранить как JSON: всё, что реализует `JsonModel`,
сериализуется при записи и восстанавливается через `jsonModel`/`jsonModels`:

```kotlin
config["last.user"] = user                  // JsonModel → JSON-строка
val lastUser = config.jsonModel<User>("last.user")
val recentUsers = config.jsonModels<User>("recent.users")
```

## Настройки ОС

`preferences` открывает ноду в хранилище `java.util.prefs` JVM, которое ОС
ведет для каждого пользователя (реестр в Windows, plists в macOS, `~/.java`
в Linux). Чтение и запись идут прямо в хранилище — сохранять ничего не нужно:

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

Без имени ноды используется нода пакета класса приложения. Используйте
`config` для структурных файловых настроек, которые, возможно, захочется
посмотреть или положить в поставку, а `preferences` — для небольших
пользовательских флагов, которые должны пережить отсутствие папки конфигурации.
