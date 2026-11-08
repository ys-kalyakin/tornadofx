# Свойства и биндинги

## Observable-свойства

JavaFX-свойства — граждане первого класса. Делегаты `getValue`/`setValue`
позволяют выставить JavaFX-свойство как обычный Kotlin `var`:

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

## Биндинги

Биндинги работают с любым observable:

```kotlin
nameField.textProperty().bind(model.nameProperty)
// или прямо в конструкторе
textfield(model.name)
```

## ItemViewModel

`ItemViewModel` оборачивает доменный объект («текущий элемент») и выставляет
биндимые фасады его свойств. Привязка поля к модели не записывает значения
напрямую до `commit()` — безопасное редактирование выбранной строки:

```kotlin
class PersonModel : ItemViewModel<Person>() {
    val id = bind(Person::idProperty)
    val name = bind(Person::nameProperty)
    val birthday = bind(Person::birthdayProperty)
}

class PersonEditor : View() {
    val model: PersonModel by inject()

    override val root = form {
        fieldset("Edit person") {
            field("Name") {
                textfield(model.name)
            }
            field("Birthday") {
                datepicker(model.birthday)
            }
            button("Save") {
                action {
                    model.commit()
                }
            }
        }
    }
}
```

Текущий элемент задаётся через `model.item = person` (или `rebind()`); `bind()`
у `ViewModel` принимает геттер свойства, `KProperty` или провайдер.

## ViewModel

`ViewModel` — тот же фасад без единого «текущего элемента»: удобен для
состояния поиска/фильтров и прочих форм:

```kotlin
class SearchModel : ViewModel() {
    val query = bind { SimpleStringProperty() }
}

textfield(model.query)
```

`commit()` / `rollback()` валидируют и переносят значения между фасадами и
лежащими в основе свойствами.

## JSON

`JsonModel` (показан выше) конвертируется в обе стороны с
`JsonObject`/`JsonArray` — на этом построен [REST-клиент](rest.md):
`toModel()`, `toJSON()`, `string("key")`, `int("key")`, `date("key")` и
друзья.
