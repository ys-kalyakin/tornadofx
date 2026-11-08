# Тестирование

Как тестировать приложение на TornadoFX: TestFX для робо-тестов UI и тулкит
Monocle, чтобы запускать их headless на CI-машинах без дисплея. Версии и флаги
ниже — те, с которыми сам этот репозиторий собирается и тестируется; его
[headless-профиль Maven](https://github.com/ys-kalyakin/tornadofx/blob/master/pom.xml)
и [CI-workflow](https://github.com/ys-kalyakin/tornadofx/blob/master/.github/workflows/ci.yml)
— живые эталонные реализации.

## Зависимости

Нужны три test-scoped зависимости: JUnit 5, TestFX core и glass-платформа
Monocle (в артефакты OpenJFX в Maven она не входит, поэтому подключается
отдельно). Версии показаны те же, что использует сборка этого репозитория.

=== "Maven"

```xml
<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.14.4</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testfx</groupId>
        <artifactId>testfx-core</artifactId>
        <version>4.0.18</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testfx</groupId>
        <artifactId>openjfx-monocle</artifactId>
        <version>21.0.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

=== "Gradle (Kotlin DSL)"

```kotlin
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.4")
    testImplementation("org.testfx:testfx-core:4.0.18")
    testImplementation("org.testfx:openjfx-monocle:21.0.2")
}

tasks.test {
    useJUnitPlatform()
}
```

## Headless-запуск на Monocle

Тулкит JavaFX по умолчанию требует дисплей. Три системных свойства
переключают его в headless-режим Monocle с программным рендером:

| Свойство | Значение |
|---|---|
| `glass.platform` | `Monocle` |
| `monocle.platform` | `Headless` |
| `prism.order` | `sw` |

=== "Maven"

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.6.1</version>
    <configuration>
        <systemPropertyVariables>
            <glass.platform>Monocle</glass.platform>
            <monocle.platform>Headless</monocle.platform>
            <prism.order>sw</prism.order>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

=== "Gradle (Kotlin DSL)"

```kotlin
tasks.test {
    systemProperty("glass.platform", "Monocle")
    systemProperty("monocle.platform", "Headless")
    systemProperty("prism.order", "sw")
}
```

## Первый UI-тест

Паттерн, которым пользуется тестовый набор этого репозитория: один раз
зарегистрировать primary stage, UI-работу выполнять внутри
`FxToolkit.setupFixture` (она переносит ее на JavaFX-поток), а затем водить и
проверять обычным `FxRobot`:

```kotlin
class GreetingView : View() {
    override val root = vbox {
        label("Hello") { addClass("greeting") }
        button("Click me")
    }
}

class GreetingViewTest {
    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    @Test
    fun `greeting label renders its text`() {
        FxToolkit.setupFixture {
            primaryStage.scene = Scene(GreetingView().root)
            primaryStage.show()
        }

        val robot = FxRobot()
        verifyThat(robot.lookup(".greeting").queryAs(Label::class.java), hasText("Hello"))
    }
}
```

`FxToolkit.setupFixture` бросает исключение, если на JavaFX-потоке случилось
необработанное — сломанное вью валит тест, а не подвешивает его.

## Проверки

Матчеры TestFX выражают ожидания о графе сцены:

```kotlin
val robot = FxRobot()
verifyThat(robot.lookup(".greeting").queryAs(Label::class.java), hasText("Hello"))

robot.clickOn("Click me")   // взаимодействия возвращают робота, поэтому их можно объединять в цепочку
```

`lookup` принимает CSS-селекторы, включая style-классы, добавленные через
`addClass`, — проверки не ломаются от визуальных изменений.

## Карантин для медленных и нестабильных тестов

UI-тесты, которым нужен настоящий цикл анимации окна, тайминги или поведение
ОС, можно изолировать в JUnit-тег и выполнять отдельным неблокирующим шагом
сборки. Пометьте тест:

```kotlin
class DragAndDropTest {
    @Test
    @Tag("quarantine")
    fun `rows can be dragged between tables`() {
        // ...
    }
}
```

Исключите тег из обычного прогона и добавьте профиль, который запускает
только карантинные тесты:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <excludedGroups>quarantine</excludedGroups>
    </configuration>
</plugin>

<profile>
    <id>quarantine</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <groups>quarantine</groups>
                    <excludedGroups combine.self="override"/>
                </configuration>
            </plugin>
        </plugins>
    </build>
</profile>
```

В Gradle тот же разрез выражается JUnit-тегами на test-задаче:

```kotlin
tasks.test {
    useJUnitPlatform {
        excludeTags("quarantine")
    }
}

val quarantineTest = tasks.register<Test>("quarantineTest") {
    group = "verification"
    useJUnitPlatform {
        includeTags("quarantine")
    }
}
```

В CI обычный набор и карантин идут отдельными job, у карантина стоит
`continue-on-error`, так что нестабильные UI-тесты никогда не блокируют
конвейер.

## Что делает сам репозиторий

Показанные выше профили Maven `headless` и `quarantine` — это настоящие
профили, с которыми репозиторий гоняет свои ~400 тестов на GitHub Actions:
полная и всегда актуальная конфигурация — в
[pom.xml](https://github.com/ys-kalyakin/tornadofx/blob/master/pom.xml) и
[ci.yml](https://github.com/ys-kalyakin/tornadofx/blob/master/.github/workflows/ci.yml).
