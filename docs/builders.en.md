# Type-safe builders

Every layout and control is a builder function: you construct the UI as a
nested Kotlin expression instead of XML, and it type-checks at compile time.

## Layouts

```kotlin
override val root = borderpane {
    top = label("Header")
    center = vbox(8.0) {
        label("Name")
        textfield()
        button("Save")
    }
    bottom = hbox {
        paddingAll = 4.0
        button("Cancel")
        button("OK")
    }
}
```

Common layout builders: `hbox`, `vbox`, `stackpane`, `gridpane`, `borderpane`,
`flowpane`, `tilepane`, `anchorpane`, `toolbar`, `tabpane`.

## Controls

Control builders create the control, configure it and add it to the parent:

```kotlin
vbox {
    label("Pick a value")
    combobox<Int>(values = listOf(1, 2, 3))
    checkbox("Enabled")
    slider(0.0..100.0)
    textfield {
        promptText = "Enter your name"
    }
    button("Save") {
        action { println("saved") }
    }
}
```

## Tables

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

Cell rendering per column uses `cellFormat`:

```kotlin
tableview(persons) {
    column<Int, Person>("Age", Person::age).cellFormat {
        text = it.toString()
        style {
            if (it < 18) textFill = Color.ORANGERED
        }
    }
}
```

Inside a custom `TableCellFragment`, `rowItem` gives access to the row's item.

`listview`, `treeview` and `treetableview` have the same style of builders:

```kotlin
listview(names) {
    cellFormat { text = it.uppercase() }
}
```

## Charts, menus and dialogs

- Charts: `piechart`, `barchart`, `linechart` with series builders (`Charts.kt`)
- Menus: `menubar { menu("File") { item("Open") } }` (`Menu.kt`)
- Dialogs: `dialog`, `alert`, `confirm`, `choiceDialog` and file choosers (`Dialogs.kt`)

## Styling inline

`style { }` sets properties on a single node without a stylesheet class:

```kotlin
label("Warning") {
    style {
        textFill = Color.ORANGE
        fontSize = 14.px
    }
}
```

For reusable styling, see [Type-safe stylesheets](stylesheets.md).
