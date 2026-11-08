# Testing

How to test a TornadoFX application: TestFX for robot-driven UI tests, and
the Monocle toolkit to run them headless on CI machines without a display.
The versions and flags below are the ones this repository itself builds and
tests with — its [headless Maven profile](https://github.com/ys-kalyakin/tornadofx/blob/master/pom.xml)
and [CI workflow](https://github.com/ys-kalyakin/tornadofx/blob/master/.github/workflows/ci.yml)
are the live reference implementations.

## Dependencies

Three test-scoped dependencies are needed: JUnit 5, TestFX core, and the
Monocle glass platform (not part of the OpenJFX Maven artifacts, so it comes
separately). Versions shown are the ones used by this repository's build.

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

## Running headless with Monocle

The JavaFX toolkit needs a display by default. Three system properties swap
it to Monocle's headless mode with software rendering:

| Property | Value |
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

## A first UI test

The pattern used throughout this repository's test suite: register the
primary stage once, run UI work inside `FxToolkit.setupFixture` (which moves
it onto the JavaFX thread), then drive and assert with a plain `FxRobot`:

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

`FxToolkit.setupFixture` throws if the JavaFX thread has an uncaught
exception, so a broken view fails the test instead of hanging it.

## Assertions

TestFX matchers express expectations about the scene graph:

```kotlin
val robot = FxRobot()
verifyThat(robot.lookup(".greeting").queryAs(Label::class.java), hasText("Hello"))

robot.clickOn("Click me")   // interactions return the robot, so they chain
```

`lookup` accepts CSS selectors, including the style classes you added with
`addClass`, which makes assertions robust against visual changes.

## Quarantining slow or flaky tests

UI tests that need a real window animation loop, timing or OS behavior can be
isolated in a JUnit tag and run in a separate, non-blocking build step. Tag
the test:

```kotlin
class DragAndDropTest {
    @Test
    @Tag("quarantine")
    fun `rows can be dragged between tables`() {
        // ...
    }
}
```

Exclude the tag from the default run, and add a profile that runs only the
quarantined tests:

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

In Gradle the same split is expressed with JUnit tags on the test task:

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

In CI the default suite and the quarantine suite run as separate jobs, the
latter with `continue-on-error` so flaky UI tests never block the pipeline.

## What the repository itself does

The `headless` and `quarantine` Maven profiles shown above are the actual
profiles this repository runs its ~400 tests with on GitHub Actions — see
the [pom.xml](https://github.com/ys-kalyakin/tornadofx/blob/master/pom.xml)
and [ci.yml](https://github.com/ys-kalyakin/tornadofx/blob/master/.github/workflows/ci.yml)
for the complete, always-current configuration.
