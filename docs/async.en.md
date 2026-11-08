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

## Coroutines

Task bodies are suspend functions, so the async machinery integrates with
`kotlinx.coroutines` directly. The coroutine support is opt-in: TornadoFX
compiles against `kotlinx-coroutines-core` and `kotlinx-coroutines-javafx`
as `provided` dependencies and never pulls them transitively — declare them
yourself when you use the coroutine API:

```xml
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-coroutines-core</artifactId>
    <version>1.10.2</version>
</dependency>
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-coroutines-javafx</artifactId>
    <version>1.10.2</version>
</dependency>
```

Classic `runAsync { } ui { }` chains keep working without these dependencies
on the classpath.

### Suspend task bodies

`runAsync` and `task` bodies may suspend: call other suspend functions, hop
to the UI thread with `ui { }` (a suspend member of the task), and report
progress with `updateProgress`/`updateMessage`:

```kotlin
runAsync(status) {
    updateMessage("Loading customers...")
    val customers = api.fetchCustomers()          // suspend call
    ui { customerTable.items = customers.asObservable() }
    customers                                     // becomes the task value
} ui {
    statusLabel.text = "Loaded ${it.size} customers"
}
```

Inside a suspend body, `ui { }` dispatches the block onto the JavaFX
Application Thread immediately and resumes the body with the block's result;
it runs inline, without a thread hop, when called from the UI thread itself.
Note the difference from the `... ui { }` chain: the chain attaches a success
callback to the task, while the suspend `ui { }` hops right now.

### Awaiting tasks

Any TornadoFX task — and any JavaFX `Task`/`Service` worker — can be awaited
from a coroutine without blocking a thread. The task's failure is rethrown at
the await site, and a cancelled task resumes the awaiting coroutine with a
`CancellationException`:

```kotlin
scope.launch {
    val customers = downloadTask.await()
    withContext(Dispatchers.JavaFx) { customerTable.items = customers }
}
```

### Cancellation mapping

Cancellation works in both directions:

- Cancelling the task cancels the running body: it stops at its next
  suspension point and the task reports the cancelled state.
- Cancelling the coroutine that awaits a running task cancels that task, so
  no background work survives the awaiting coroutine.
- A `CancellationException` escaping the body is treated as cancellation, not
  failure: the task lands in the cancelled state.

```kotlin
val download = runAsync(status) {
    updateMessage("Downloading...")
    api.download().await()
} ui { showResult(it) }

// From anywhere, later:
download.cancel()   // the body stops at its next suspension point
```
