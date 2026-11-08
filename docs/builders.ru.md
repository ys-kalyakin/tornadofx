# Типобезопасные конструкторы

Каждый лейаут и контрол — это функция-конструктор: UI собирается как вложенное
Kotlin-выражение вместо XML и проверяется на типы во время компиляции.

## Лейауты

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

Основные лейауты: `hbox`, `vbox`, `stackpane`, `gridpane`, `borderpane`,
`flowpane`, `tilepane`, `anchorpane`, `toolbar`, `tabpane`.

## Контролы

Конструкторы контролов создают контрол, настраивают его и добавляют в родителя:

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

## Таблицы

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

Отрисовку ячеек настраивает `cellFormat` для конкретной колонки:

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

Внутри собственного `TableCellFragment` свойство `rowItem` даёт доступ к
элементу строки.

`listview`, `treeview` и `treetableview` строятся в том же стиле:

```kotlin
listview(names) {
    cellFormat { text = it.uppercase() }
}
```

## Диаграммы, меню и диалоги

- Диаграммы: `piechart`, `barchart`, `linechart` с конструкторами серий (`Charts.kt`)
- Меню: `menubar { menu("File") { item("Open") } }` (`Menu.kt`)
- Диалоги: `dialog`, `alert`, `confirm`, `choiceDialog` и выбор файлов (`Dialogs.kt`)

## Стилизация на месте

Блок `style { }` задаёт свойства одному узлу без класса стилей:

```kotlin
label("Warning") {
    style {
        textFill = Color.ORANGE
        fontSize = 14.px
    }
}
```

Для переиспользуемых стилей см. [Типобезопасные стили](stylesheets.md).
