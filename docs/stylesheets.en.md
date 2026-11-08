# Type-safe stylesheets

Stylesheets are written as Kotlin classes (`Stylesheet` subclasses), giving you
compile-time checked selectors, properties and colors. Attach one to an `App`:

```kotlin
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

Multiple stylesheets: pass several `Stylesheet` subclasses to `App(view, *styles)`.

## Classes, ids and selectors

Define style classes and ids as properties, then apply them to nodes:

```kotlin
class Styles : Stylesheet() {
    companion object {
        val heading by cssclass()
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

Apply a class to a node in a builder:

```kotlin
label("Hello world") {
    addClass(heading)
}
```

`toggleClass()` toggles conditionally, `removeClass()` removes:

```kotlin
button {
    toggleClass(activeStyle, condition)
}
```

## Colors and units

- Colors: `c("#bdbd22")`, `Color.DARKGRAY`, `c(color = "red", opacity = 0.5)`
- Sizes: `10.px`, `1.2.em`, `2.percent`, `20.pt`
- Boxes (insets/borders): `box(top, right, bottom, left)`

## Mixins and nested selectors

A `mixin` is a reusable rule body applied with `+mixin` inside any selector
block. Selectors nest naturally:

```kotlin
init {
    tableview {
        columnHeader {
            fontWeight = BOLD
        }
        tableRowCell {
            and(hover) {
                backgroundColor += c("#f0f0f0")
            }
        }
    }
}
```

Pseudo classes (`hover`, `focused`, ...) and combinators (`and`, `or`, `except`,
`s()` for grouping several selectors) are available as Kotlin functions.

## Inline styling

For one-off styling directly on a node, use the `style { }` block from
[Type-safe builders](builders.md#styling-inline) — a stylesheet class is for
reusable, named styles.

!!! note "URL handler relocation in this fork"
    The stylesheet URL handler lives in `tornadofx.css` (previously
    `sun.net.www.protocol.css`). The framework registers it automatically;
    only applications that referenced the old package directly must update.
    See [Migration from 1.7.20](migration.md).
