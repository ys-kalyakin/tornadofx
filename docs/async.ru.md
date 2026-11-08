# Асинхронность

Долгие операции не должны выполняться в потоке JavaFX Application. TornadoFX
даёт небольшие хелперы, которые держат UI отзывчивым и автоматически
возвращают результат в UI-поток.

## runAsync / ui

`runAsync` выполняет блок в фоновом потоке; `ui` получает результат уже в
UI-потоке:

```kotlin
runAsync {
    controller.loadCustomers()          // фоновый поток
} ui {
    customerTable.items = it            // UI-поток
}
```

`runLater` планирует блок в UI-потоке (опционально с задержкой):

```kotlin
runLater(1.seconds) {
    statusLabel.text = "Done"
}
```

## Прогресс

Привяжите `TaskStatus` к индикатору прогресса и передайте его в `runAsync`:

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

`asyncItems` загружает элементы в list-контрол в фоновом потоке и
присваивает результат в UI-потоке — распространённая альтернатива явной паре
`runAsync/ui`:

```kotlin
customerTable.asyncItems { controller.loadCustomers() }
```

## Отмена и завершение

Асинхронные задачи возвращают `Task`; завершение цепочкой через
`success`/`fail`:

```kotlin
runAsync { heavyComputation() }
    .success { showResult(it) }
    .fail { showError(it) }
```

Фреймворк аккуратно останавливает свои пулы потоков при завершении
приложения (`ShutdownExecutor`) — управлять жизненным циклом экзекуторов в
обычных приложениях не нужно.
