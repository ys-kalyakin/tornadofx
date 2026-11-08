![TornadoFX Logo](graphics/tornado-fx-logo.png?raw=true "TornadoFX")
# TornadoFX

JavaFX Framework for Kotlin

[![Maven Central](https://img.shields.io/maven-central/v/io.github.ys-kalyakin/tornadofx)](https://central.sonatype.com/artifact/io.github.ys-kalyakin/tornadofx)
[![Apache License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

## Supported stack

TornadoFX targets the modern JavaFX stack:

- **Build toolchain:** JDK 21 (published bytecode is Java 17)
- **JavaFX:** OpenJFX 21 (declared as a Maven dependency with platform classifiers)
- **Kotlin:** 2.x (K2), `jvmTarget` 17

Migrating from 1.7.x? See the breaking changes in the
[Change Log](CHANGELOG.md). OSGi users must stay on the 1.7.x line — OSGi
support was removed together with the Java 8/JavaFX 8 era stack.

## Features

- Supports both MVC, MVP and their derivatives
- Dependency injection
- Type safe GUI builders
- Type safe CSS builders
- First class FXML support
- Async task execution
- Kotlin coroutines integration: suspend task bodies, `Task.await()`, `ui { }`
  FX-thread dispatch (opt-in kotlinx-coroutines dependency)
- EventBus with thread targeting
- Hot reload of Views and Stylesheets
- REST client with automatic JSON conversion (Apache HttpClient 5)
- kotlinx.serialization bridge for the REST client: decode responses into
  `@Serializable` types and send typed request bodies (opt-in
  kotlinx-serialization dependency)
- Zero config, no XML, no annotations

## Important version note

TornadoFX requires Kotlin 2.x and jvmTarget 17. Make sure you update your IDE plugins (Kotlin).

Remember to update your build system to configure the `jvmTarget` as well.

For Maven, you add the following configuration block to `kotlin-maven-plugin`:

```xml
<configuration>
    <jvmTarget>17</jvmTarget>
</configuration>
```

For Gradle, it means configuring the `kotlinOptions` of the Kotlin compilation task:

```gradle
compileKotlin {
    kotlinOptions.jvmTarget = "17"
}
```

Failing to do so will yield errors about the compiler not being able to inline certain calls.

You also need a full rebuild of your code after a version upgrade. If you run into trouble, try to clean caches and restart IDEA (File -> Invalidate caches / Restart).
 
## Getting started

- [Guide](https://ys-kalyakin.github.io/tornadofx/) — documentation for this fork (English/Russian, switchable)
- [Changelog](CHANGELOG.md)
- [Screencasts](https://www.youtube.com/user/MrEdvinsyse)
- [Slack](https://kotlinlang.slack.com/messages/tornadofx/details/)
- [Stack Overflow](http://stackoverflow.com/questions/ask?tags=tornadofx)
- [IntelliJ IDEA Plugin](https://github.com/edvin/tornadofx-idea-plugin) 
- [Example Application](https://github.com/edvin/tornadofx-samples) 
- [Maven QuickStart Archetype](https://github.com/edvin/tornadofx-quickstart-archetype) — upstream resource; it scaffolds projects with the old `no.tornado` coordinates, not this fork
- [Legacy 1.7.x wiki](https://github.com/edvin/tornadofx/wiki) — documentation for the upstream 1.7.x line

### Add TornadoFX to your project

#### Maven

```xml
<dependency>
    <groupId>io.github.ys-kalyakin</groupId>
    <artifactId>tornadofx</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.ys-kalyakin:tornadofx:2.0.0'
```

### Snapshots

This fork does not publish snapshot repositories. The upstream daily Sonatype
snapshot publishing described in older documentation does not apply here; use a
released version as shown above, or build from source with `./mvnw install`.

### What does it look like? (Code snippets)

Create a View

```kotlin
class HelloWorld : View() {
    override val root = hbox {
        label("Hello world")
    }
}
```

Start your application and show the primary `View` and add a type safe stylesheet
    
```kotlin
import javafx.scene.text.FontWeight
import tornadofx.*

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
> Start app and load a type safe stylesheet

Use [Type Safe Builders](https://github.com/edvin/tornadofx/wiki/Type-Safe-Builders) to quickly create complex user interfaces

```kotlin
class MyView : View() {
    private val persons = FXCollections.observableArrayList(
            Person(1, "Samantha Stuart", LocalDate.of(1981,12,4)),
            Person(2, "Tom Marks", LocalDate.of(2001,1,23)),
            Person(3, "Stuart Gills", LocalDate.of(1989,5,23)),
            Person(3, "Nicole Williams", LocalDate.of(1998,8,11))
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

**RENDERED UI**

![](https://i.imgur.com/AGMCP8S.png)

Create a Customer model object that can be converted to and from JSON and exposes both a JavaFX Property and getter/setter pairs:

```kotlin
import tornadofx.getValue
import tornadofx.setValue

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
    
Create a controller which downloads a JSON list of customers with the REST api:

```kotlin
class HelloWorldController : Controller() {
    val api : Rest by inject()
    
    fun loadCustomers(): ObservableList<Customer> = 
        api.get("customers").list().toModel() 
}
```
    
Configure the REST API with a base URI and Basic Authentication:
    
```kotlin
with (api) {
    baseURI = "http://contoso.com/api"
    setBasicAuth("user", "secret")
}
```
    
With the opt-in kotlinx-serialization dependency, REST responses decode into
`@Serializable` types and request bodies encode from them — no reflection-based
mapping involved. TornadoFX compiles against `kotlinx-serialization-core` and
`kotlinx-serialization-json` as `provided` dependencies and never pulls them
transitively — declare kotlinx-serialization yourself and apply its compiler
plugin to your build for your `@Serializable` classes; the classic
`JsonModel`/`JsonValue` conversion works without them:

```kotlin
@Serializable
data class CustomerDto(val name: String)

val customer = api.get("customers/1").value<CustomerDto>()
api.post("customers", CustomerDto("Edvin"))
```

Load customers in the background and update a TableView on the UI thread:

```kotlin
runAsync {
    controller.loadCustomers()
} ui {
    customerTable.items = it
}
```

With the opt-in kotlinx-coroutines dependency, task bodies become suspend
functions: call other suspend functions, hop to the UI thread with `ui { }`,
and await any task from a coroutine with `await()`. Cancellation is mapped in
both directions — cancelling the task cancels the running coroutine at its
next suspension point, and cancelling an awaiting coroutine cancels its task.
TornadoFX compiles against `kotlinx-coroutines-core` and
`kotlinx-coroutines-javafx` as `provided` dependencies and never pulls them
transitively — declare them yourself when you use the coroutine API; classic
`runAsync { } ui { }` chains work without them:

```kotlin
runAsync(status) {
    updateMessage("Loading customers...")
    val customers = controller.fetchCustomers()   // suspend call
    ui { customerTable.items = customers }
    customers
} ui {
    statusLabel.text = "Loaded ${it.size} customers"
}
```

Load customers and apply to table declaratively:

```kotlin
customerTable.asyncItems { controller.loadCustomers() }
```

Define a type safe CSS stylesheet:

```kotlin
class Styles : Stylesheet() {
    companion object {
        // Define css classes
        val heading by cssclass()
        
        // Define colors
        val mainColor = c("#bdbd22")
    }

    init {
        heading {
            textFill = mainColor
            fontSize = 20.px
            fontWeight = BOLD
        }
        
        button {
            padding = box(10.px, 20.px)
            fontWeight = BOLD
        }

        val flat = mixin {
            backgroundInsets += box(0.px)
            borderColor += box(Color.DARKGRAY)
        }

        s(button, textInput) {
            +flat
        }
    }
}
```

Create an HBox with a Label and a TextField with type safe builders:

```kotlin
hbox {
    label("Hello world") {
        addClass(heading)
    }
    
    textfield {
        promptText = "Enter your name"
    }
}
```
    
Get and set per component configuration settings:
    
```kotlin
// set prefWidth from setting or default to 200.0
node.prefWidth(config.double("width", 200.0))

// set username and age, then save
with (config) {
    set("username", "john")
    set("age", 30)
    save()
}
```
    
Create a `Fragment` instead of a `View`. A `Fragment` is not a `Singleton` like `View` is, so you will
create a new instance and you can reuse the Fragment in multiple ui locations simultaneously.
     
```kotlin
class MyFragment : Fragment() {
    override val root = hbox {
    }
}
```
     
Open it in a Modal Window:
                   
```kotlin
find<MyFragment>().openModal()
``` 
         
Lookup and embed a `View` inside another `Pane` in one go
           
```kotlin
add<MyFragment>()
```

Inject a `View` and embed inside another `Pane`
       
```kotlin
val myView: MyView by inject()
 
init {
    root.add(myFragment)
}
```

Swap a View for another (change Scene root or embedded View)

```kotlin
button("Go to next page") {
    action {
        replaceWith<PageTwo>(ViewTransition.Slide(0.3.seconds, Direction.LEFT)
    }
}
```

Open a View in an internal window over the current scene graph

```kotlin
button("Open") {
    action {
        openInternalWindow<MyOtherView>()
    }
}
```
