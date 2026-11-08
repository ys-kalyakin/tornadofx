# Getting started

## Prerequisites

To *consume* TornadoFX you need:

- A **JDK 17+ runtime** (JDK 21 recommended)
- **JavaFX 21** as a project dependency (the framework compiles against OpenJFX 21;
  there is no JDK-bundled JavaFX anymore)
- **Kotlin 2.3 or newer** with `jvmTarget` 17 — the framework ships Kotlin
  metadata 2.4.0, and a Kotlin compiler reads metadata up to one minor version ahead

To *build the framework itself* from source, use a JDK 21 toolchain
(`./mvnw test -Pheadless` runs the suite headless via Monocle).

## Maven

Add the framework dependency (current release: `2.0.0`, published to Maven
Central under the fork's `io.github.ys-kalyakin` namespace):

```xml
<dependency>
    <groupId>io.github.ys-kalyakin</groupId>
    <artifactId>tornadofx</artifactId>
    <version>2.0.0</version>
</dependency>
```

Add the JavaFX modules you use, with the platform classifier:

```xml
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>21</version>
    <classifier>linux</classifier>
</dependency>
```

Configure the Kotlin compiler — without `jvmTarget` 17 the compiler fails to
inline framework calls:

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

!!! note "Full rebuild after upgrades"
    You need a full rebuild of your code after a version upgrade. If you run
    into trouble, clean caches and restart IDEA (File → Invalidate Caches /
    Restart).

## Your first application

A `View` declares its UI in `root`; an `App` picks the first View to show:

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

Start the application with `Application.launch(HelloWorldApp::class.java)` —
or let IntelliJ do it with the [TornadoFX IDEA plugin](https://github.com/edvin/tornadofx-idea-plugin).

Type safe builders let you create complex user interfaces quickly:

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

## Where to go next

- Views, Fragments and dependency injection: [Views & DI](views.md)
- The builder DSL in depth: [Type-safe builders](builders.md)
- Coming from 1.7.20: [Migration from 1.7.20](migration.md)
