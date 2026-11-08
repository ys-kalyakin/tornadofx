# Views & DI

## View

A `View` is a UI component whose `root` is a Node. It is a lazily created
singleton: `find<MyView>()` (or an `inject` of it) always returns the same
instance.

```kotlin
class MainView : View("My title") {
    override val root = vbox {
        label("Name:")
        textfield()
        button("Submit")
    }
}
```

`App(MainView::class)` launches the application showing this View.

## Fragment

A `Fragment` is like a `View`, but not a singleton: each `find<MyFragment>()`
creates a new instance, and the same Fragment can be embedded in multiple UI
locations simultaneously.

```kotlin
class MyFragment : Fragment("My fragment") {
    override val root = hbox { }
}
```

Open it in a modal window:

```kotlin
find<MyFragment>().openModal()
```

Embed a View or Fragment into another pane in one go:

```kotlin
add<MyFragment>()
```

Inject a View and embed it inside another pane:

```kotlin
val myView: MyView by inject()

init {
    root.add(myView)
}
```

## Dependency injection

`inject` resolves components (Views, Controllers, Fragments) lazily from the
framework's component registry:

```kotlin
class MyController : Controller() {
    fun loadPeople() = listOf("Alice", "Bob")
}

class MainView : View() {
    val controller: MyController by inject()

    override val root = label(controller.loadPeople().first())
}
```

Controllers are `Controller` subclasses — plain singletons without UI.

## Swapping views

Replace the current View (scene root or embedded):

```kotlin
button("Go to next page") {
    action {
        replaceWith<PageTwo>(ViewTransition.Slide(0.3.seconds, Direction.LEFT))
    }
}
```

Open a View in an internal window over the current scene graph:

```kotlin
button("Open") {
    action {
        openInternalWindow<MyOtherView>()
    }
}
```

## Scopes

Scopes provide isolated dependency injection domains — a View, its fragments
and controllers inside one scope share singletons without leaking to other
scopes:

```kotlin
Scope().apply {
    inject<MyView>()
}
```

Inside a component, `scope` gives access to its current scope, and components
created within that scope resolve their `inject` dependencies from it. This is
the basis of the [Workspace](workspace.md) and Wizard patterns.
