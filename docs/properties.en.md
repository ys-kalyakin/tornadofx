# Properties & binding

## Observable properties

JavaFX properties are first-class citizens. The `getValue`/`setValue` delegates
(let you expose a JavaFX property as a plain Kotlin `var`:

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

## Binding

Bindings work with any observable:

```kotlin
nameField.textProperty().bind(model.nameProperty)
// or in builders
textfield(model.name)
```

## ItemViewModel

`ItemViewModel` wraps a domain item (a "current item") and exposes bindable
facades for its properties. Binding a field to the model does not write
through until you `commit()` — safe editing of a currently selected row:

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

Set the current item with `model.item = person` (or `rebind()`); `bind()` on a
`ViewModel` accepts a property getter, a KProperty or a provider.

## ViewModel

`ViewModel` is the same facade without a single "current item" — useful for
search/filter state and other form data:

```kotlin
class SearchModel : ViewModel() {
    val query = bind { SimpleStringProperty() }
}

textfield(model.query)
```

`commit()` / `rollback()` validate and transfer values between the facades and
the underlying properties.

## JSON

`JsonModel` (shown above) converts to and from `JsonObject`/`JsonArray` — used
heavily by the [REST client](rest.md): `toModel()`, `toJSON()`, `string("key")`,
`int("key")`, `date("key")` and friends.
