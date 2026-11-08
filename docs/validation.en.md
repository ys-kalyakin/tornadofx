# Validation

Validation is built around a `ValidationContext` per `ViewModel`, with
declarative validators on controls and decoration of invalid fields.

## Required fields

Controls bound to a ViewModel property get validation helpers — `required()`,
`requiredWhen()` — that register a validator for the field:

```kotlin
class PersonModel : ItemViewModel<Person>() {
    val name = bind(Person::nameProperty)
    val email = bind(Person::emailProperty)
}

class PersonEditor : View() {
    val model: PersonModel by inject()

    override val root = form {
        fieldset("Person") {
            field("Name") {
                textfield(model.name).required()
            }
            field("Email") {
                textfield(model.email).required()
            }
        }
    }
}
```

The default trigger is `ValidationTrigger.OnChange()` — the field revalidates
as the user types. Other triggers (`Blur`, `EnterPress`) are available.

## Custom validators

`validator()` adds any rule; a `ValidationMessage?` result marks the field
invalid, `null` marks it valid:

```kotlin
textfield(model.email).validator(ValidationTrigger.OnChange()) {
    if (it != null && !it.contains('@')) error("Not an email address") else null
}
```

Properties inside a ViewModel can also carry validators via
`Property.addValidator(node) { ... }`.

## Validating and committing

`validate()` runs all validators (focusing the first invalid field and
decorating errors by default); `commit()` validates first and only transfers
values into the underlying item when every field is valid:

```kotlin
button("Save") {
    action {
        if (model.validate()) {
            model.commit()
            close()
        }
    }
}
```

`rollback()` restores the facades from the underlying item:

```kotlin
button("Cancel") {
    action {
        model.rollback()
        close()
    }
}
```

`commit()` accepts specific fields and a success callback:
`model.commit(model.name) { println("saved") }`.

## Decorations

Invalid fields are decorated by an error decoration provider. The default
`DefaultDecoration` paints the input with a validation error style; supply a
custom decorator through `ValidationContext.decorationProvider`.
