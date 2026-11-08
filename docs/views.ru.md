# View и DI

## View

`View` — это UI-компонент, чей `root` является Node. Он создаётся лениво как
синглтон: `find<MyView>()` (или `inject` этого View) всегда возвращает один и
тот же экземпляр.

```kotlin
class MainView : View("My title") {
    override val root = vbox {
        label("Name:")
        textfield()
        button("Submit")
    }
}
```

`App(MainView::class)` запускает приложение, показывая этот View.

## Fragment

`Fragment` похож на `View`, но не является синглтоном: каждый вызов
`find<MyFragment>()` создаёт новый экземпляр, а один и тот же Fragment можно
одновременно встраивать в несколько мест UI.

```kotlin
class MyFragment : Fragment("My fragment") {
    override val root = hbox { }
}
```

Открыть его в модальном окне:

```kotlin
find<MyFragment>().openModal()
```

Встроить View или Fragment в другую панель одним вызовом:

```kotlin
add<MyFragment>()
```

Внедрить View и встроить его в другую панель:

```kotlin
val myView: MyView by inject()

init {
    root.add(myView)
}
```

## Внедрение зависимостей

`inject` лениво разрешает компоненты (View, Controller, Fragment) из реестра
компонентов фреймворка:

```kotlin
class MyController : Controller() {
    fun loadPeople() = listOf("Alice", "Bob")
}

class MainView : View() {
    val controller: MyController by inject()

    override val root = label(controller.loadPeople().first())
}
```

Controller — это подкласс `Controller`: обычный синглтон без UI.

## Смена View

Заменить текущий View (корень сцены или встроенный):

```kotlin
button("Go to next page") {
    action {
        replaceWith<PageTwo>(ViewTransition.Slide(0.3.seconds, Direction.LEFT))
    }
}
```

Открыть View во внутреннем окне поверх текущего графа сцены:

```kotlin
button("Open") {
    action {
        openInternalWindow<MyOtherView>()
    }
}
```

## Скоупы

Скоупы дают изолированные домены внедрения зависимостей — View, его фрагменты
и контролы внутри одного скоупа делят синглтоны, не просачиваясь в другие
скоупы:

```kotlin
Scope().apply {
    inject<MyView>()
}
```

Внутри компонента `scope` даёт доступ к его текущему скоупу, и компоненты,
созданные в этом скоупе, разрешают свои зависимости `inject` через него. На
этом построены паттерны [Workspace](workspace.md) и Wizard.
