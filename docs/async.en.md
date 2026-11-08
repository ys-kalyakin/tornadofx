# Async

Long-running work must not run on the JavaFX Application Thread. TornadoFX
gives you small helpers that keep the UI responsive and move results back to
the UI thread automatically.

## runAsync / ui

`runAsync` executes a block on a background thread; `ui` receives its result
on the UI thread:

```kotlin
runAsync {
    controller.loadCustomers()          // background
} ui {
    customerTable.items = it            // UI thread
}
```

`runLater` schedules a block on the UI thread (optionally after a delay):

```kotlin
runLater(1.seconds) {
    statusLabel.text = "Done"
}
```

## Progress

Bind a `TaskStatus` to a progress indicator and pass it to `runAsync`:

```kotlin
class MyView : View() {
    val status = TaskStatus()

    override val root = vbox {
        progressbar(status.progress)
    }

    init {
        runAsync(status) {
            downloadAll()
        } ui {
            updateList(it)
        }
    }
}
```

## asyncItems

`asyncItems` loads items into a list-backed control on a background thread
and assigns the result on the UI thread — a common alternative to the explicit
`runAsync/ui` pair:

```kotlin
customerTable.asyncItems { controller.loadCustomers() }
```

## Cancellation and completion

Async tasks return a `Task`; chain completion with `success`/`fail`:

```kotlin
runAsync { heavyComputation() }
    .success { showResult(it) }
    .fail { showError(it) }
```

The framework shuts its thread pools down cleanly when the application stops
(`ShutdownExecutor`), so you do not need to manage executor lifecycles in
normal applications.
