# Начало работы

## Требования

Чтобы *использовать* TornadoFX, нужно:

- **JDK 17+** в рантайме (рекомендуется JDK 21)
- **JavaFX 21** как зависимость проекта (фреймворк компилируется против
  OpenJFX 21; JavaFX, встроенный в JDK, больше не существует)
- **Kotlin 2.3 или новее** с `jvmTarget` 17 — фреймворк содержит метаданные
  Kotlin 2.4.0, а компилятор Kotlin читает метаданные максимум на одну минорную
  версию вперёд

Чтобы *собрать сам фреймворк* из исходников, используйте JDK 21
(`./mvnw test -Pheadless` запускает тесты headless через Monocle).

## Maven

Подключите зависимость фреймворка (текущий релиз: `2.0.0`, опубликован в Maven
Central под неймспейсом форка `io.github.ys-kalyakin`):

```xml
<dependency>
    <groupId>io.github.ys-kalyakin</groupId>
    <artifactId>tornadofx</artifactId>
    <version>2.0.0</version>
</dependency>
```

Добавьте используемые модули JavaFX с платформенным классификатором:

```xml
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>21</version>
    <classifier>linux</classifier>
</dependency>
```

Настройте компилятор Kotlin — без `jvmTarget` 17 компилятор не сможет
инлайнить вызовы фреймворка:

```xml
<configuration>
    <jvmTarget>17</jvmTarget>
</configuration>
```

## Gradle

```groovy
dependencies {
    implementation 'io.github.ys-kalyakin:tornadofx:2.0.0'
    implementation 'org.openjfx:javafx-controls:21:linux'
}

compileKotlin {
    kotlinOptions.jvmTarget = '17'
}
```

!!! note "Полная пересборка после обновления"
    После обновления версии нужна полная пересборка кода. Если что-то пошло
    не так, очистите кэши и перезапустите IDEA (File → Invalidate Caches /
    Restart).

## Первое приложение

`View` описывает UI в `root`; `App` выбирает первый показываемый View:

```kotlin
import javafx.scene.text.FontWeight
import tornadofx.*

class HelloWorld : View() {
    override val root = hbox {
        label("Hello world")
    }
}

class HelloWorldApp : App(HelloWorld::class, Styles::class)

class Styles : Stylesheet() {
    init {
        label {
            fontSize = 20.px
            fontWeight = FontWeight.BOLD
            backgroundColor += c("#cecece")
        }
    }
}
```

Запустите приложение через `Application.launch(HelloWorldApp::class.java)` —
или через [плагин TornadoFX для IDEA](https://github.com/edvin/tornadofx-idea-plugin).

Типобезопасные конструкторы позволяют быстро собирать сложные интерфейсы:

```kotlin
class MyView : View() {
    private val persons = FXCollections.observableArrayList(
        Person(1, "Samantha Stuart", LocalDate.of(1981, 12, 4)),
        Person(2, "Tom Marks", LocalDate.of(2001, 1, 23)),
        Person(3, "Stuart Gills", LocalDate.of(1989, 5, 23)),
        Person(3, "Nicole Williams", LocalDate.of(1998, 8, 11))
    )

    override val root = tableview(persons) {
        column("ID", Person::id)
        column("Name", Person::name)
        column("Birthday", Person::birthday)
        column("Age", Person::age)
        columnResizePolicy = SmartResize.POLICY
    }
}
```

## Что дальше

- View, Fragment и внедрение зависимостей: [View и DI](views.md)
- DSL конструкторов подробнее: [Типобезопасные конструкторы](builders.md)
- Если вы приходите с 1.7.20: [Миграция с 1.7.20](migration.md)
