# Workspace & Wizard

## Workspace

`Workspace` is an application shell with a header (back/forward navigation,
save, create, delete, refresh and close buttons), a docked content area and a
status bar. It is itself a `View`, so it participates in DI and scopes.

The quickest way to get one is `WorkspaceApp`, which docks your initial View:

```kotlin
class MyApp : WorkspaceApp(MainView::class, Styles::class)
```

For a custom shell, subclass `Workspace` and `dock()` views into it:

```kotlin
class MainWorkspace : Workspace("My Application") {
    init {
        add(MainView::class)
    }
}

class MyApp : App(MainWorkspace::class, Styles::class)
```

Docked views drive the header buttons: `savableWhen { }` enables the save
button and `whenSaved { }` runs on click — likewise `creatableWhen/whenCreated`,
`deletableWhen/whenDeleted`, `refreshableWhen/whenRefreshed` and
`closeableWhen`:

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

Views docked into a workspace share its `Scope` — their `inject` dependencies
resolve from the workspace's scope ([Views & DI](views.md)).

## Wizard

`Wizard` shows a sequence of pages in a modal window with back/next/finish/
cancel buttons and a steps sidebar:

```kotlin
class RegistrationWizard : Wizard("Registration", "Create a new account") {
    init {
        pages.addAll(AccountStep(), ProfileStep())
    }
}
```

Each page is a `View` or `Fragment` with its own `complete` expression; the
wizard's Next button is driven by `canGoNext` and Finish by `canFinish`.

```kotlin
button("Register") {
    action {
        find<RegistrationWizard>().openModal()
    }
}
```

`enterProgresses` makes Enter advance to the next page; `stepLinksCommits`
commits each page when its step link is clicked.
