# FXML

TornadoFX supports first class FXML: a View or Fragment loads its root from an
FXML file, and the component itself acts as the controller — no separate
controller class needed.

## A View backed by FXML

Declare the root type and load it with `by fxml()`:

```kotlin
class MainView : View("Main") {
    override val root: VBox by fxml("/view/main.fxml")

    val greeting: Label by fxid()

    fun sayHello() {
        greeting.text = "Hello, TornadoFX"
    }
}
```

In the FXML file, point `fx:controller` at the View class:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<VBox xmlns="http://javafx.com/javafx" xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.example.MainView">
    <children>
        <Label fx:id="greeting" text="Hello"/>
        <Button text="Say hello" onAction="#sayHello"/>
    </children>
</VBox>
```

## Accessing nodes with fxid()

Nodes with `fx:id` are injected by declaring the property name with `by fxid()`:

```kotlin
val greeting: Label by fxid()
```

## Fragments and inline FXML

Fragments load FXML the same way:

```kotlin
class DetailFragment : Fragment("Detail") {
    override val root: GridPane by fxml("/view/detail.fxml")
}
```

To reload FXML content into an existing root (useful with hot reload):

```kotlin
loadFXML("/view/main.fxml")
```

## When to use FXML vs builders

Use FXML when you want Scene Builder-friendly layouts or a designer/developer
split. Use [type-safe builders](builders.md) for compile-time checking and
Kotlin-native composition — the two approaches mix freely: an FXML View can
build sub-nodes in Kotlin and vice versa.
