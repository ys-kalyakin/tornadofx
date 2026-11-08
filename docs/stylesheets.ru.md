# Типобезопасные стили

Стили пишутся как Kotlin-классы (наследники `Stylesheet`), что даёт
проверяемые на компиляции селекторы, свойства и цвета. Прикрепите стили к
`App`:

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

Несколько стилей: передайте несколько подклассов `Stylesheet` в
`App(view, *styles)`.

## Классы, id и селекторы

Объявите классы стилей и id как свойства, затем применяйте их к узлам:

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

Примените класс к узлу в конструкторе:

```kotlin
label("Hello world") {
    addClass(heading)
}
```

`toggleClass()` переключает класс по условию, `removeClass()` удаляет:

```kotlin
button {
    toggleClass(activeStyle, condition)
}
```

## Цвета и единицы

- Цвета: `c("#bdbd22")`, `Color.DARKGRAY`, `c(color = "red", opacity = 0.5)`
- Размеры: `10.px`, `1.2.em`, `2.percent`, `20.pt`
- Боксы (отступы/границы): `box(top, right, bottom, left)`

## Миксины и вложенные селекторы

`mixin` — переиспользуемое тело правила, применяемое через `+mixin` внутри
любого селекторного блока. Селекторы вкладываются естественно:

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

Псевдоклассы (`hover`, `focused`, ...) и комбинаторы (`and`, `or`, `except`,
`s()` для группировки нескольких селекторов) доступны как функции Kotlin.

## Стилизация на месте

Для разовой стилизации узла используйте блок `style { }` из раздела
[Типобезопасные конструкторы](builders.md) — класс стилей предназначен для
переиспользуемых именованных стилей.

!!! note "Перенос URL-обработчика в этом форке"
    Обработчик URL стилей живёт в `tornadofx.css` (раньше —
    `sun.net.www.protocol.css`). Фреймворк регистрирует его автоматически;
    обновиться должны только приложения, которые обращались к старому пакету
    напрямую. См. [Миграция с 1.7.20](migration.md).
