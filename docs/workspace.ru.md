# Workspace и Wizard

## Workspace

`Workspace` — оболочка приложения с заголовком (навигация назад/вперёд,
кнопки save, create, delete, refresh и close), областью докнутого содержимого
и статус-баром. Сам он является `View`, поэтому участвует в DI и скоупах.

Быстрейший способ получить оболочку — `WorkspaceApp`, который докает
начальный View:

```kotlin
class MyApp : WorkspaceApp(MainView::class, Styles::class)
```

Для собственной оболочки наследуйтесь от `Workspace` и докайте View через
`dock()`:

```kotlin
class MainWorkspace : Workspace("My Application") {
    init {
        add(MainView::class)
    }
}

class MyApp : App(MainWorkspace::class, Styles::class)
```

Докнутые View управляют кнопками заголовка: `savableWhen { }` включает кнопку
save, а `whenSaved { }` выполняется по нажатию — аналогично
`creatableWhen/whenCreated`, `deletableWhen/whenDeleted`,
`refreshableWhen/whenRefreshed` и `closeableWhen`:

```kotlin
class PersonView : View("Person") {
    override val root = form { /* ... */ }

    init {
        savableWhen { model.dirty }
        whenSaved {
            model.commit()
        }
    }
}
```

View, докнутые в workspace, делят его `Scope` — их зависимости `inject`
разрешаются из скоупа workspace ([View и DI](views.md)).

## Wizard

`Wizard` показывает последовательность страниц (`pages`) в модальном окне с
кнопками назад/далее/завершить/отмена и сайдбаром шагов:

```kotlin
class RegistrationWizard : Wizard("Registration", "Create a new account") {
    init {
        pages.addAll(AccountStep(), ProfileStep())
    }
}
```

Каждая страница — `View` или `Fragment` со своим выражением `complete`;
кнопку «Далее» ведёт `canGoNext`, «Завершить» — `canFinish`.

```kotlin
button("Register") {
    action {
        find<RegistrationWizard>().openModal()
    }
}
```
