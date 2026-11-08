# Валидация

Валидация построена вокруг `ValidationContext` у каждого `ViewModel` — с
декларативными валидаторами на контролах и декорированием невалидных полей.

## Обязательные поля

Контролы, привязанные к свойству ViewModel, получают хелперы валидации —
`required()`, `requiredWhen()` — которые регистрируют валидатор для поля:

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

Триггер по умолчанию — `ValidationTrigger.OnChange()`: поле перевалидируется
по мере ввода. Доступны и другие триггеры (`Blur`, `EnterPress`).

## Свои валидаторы

`validator()` добавляет любое правило; результат `ValidationMessage?` помечает
поле невалидным, `null` — валидным:

```kotlin
textfield(model.email).validator(ValidationTrigger.OnChange()) {
    if (it != null && !it.contains('@')) error("Not an email address") else null
}
```

Свойства внутри ViewModel тоже могут нести валидаторы — через
`Property.addValidator(node) { ... }`.

## Валидация и фиксация

`validate()` запускает все валидаторы (по умолчанию фокусирует первое
невалидное поле и декорирует ошибки); `commit()` сначала валидирует и только
затем переносит значения в underlying-объект, если все поля валидны:

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

`rollback()` восстанавливает фасады из underlying-объекта:

```kotlin
button("Cancel") {
    action {
        model.rollback()
        close()
    }
}
```

`commit()` принимает конкретные поля и callback успеха:
`model.commit(model.name) { println("saved") }`.

## Декорирование

Невалидные поля декорируются провайдером декораций. Дефолтный
`DefaultDecoration` помечает ввод стилем ошибки валидации; собственный
декоратор подаётся через `ValidationContext.decorationProvider`.
