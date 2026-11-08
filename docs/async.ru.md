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

## Корутины

Тела задач — suspend-функции, поэтому асинхронная механика напрямую
интегрируется с `kotlinx.coroutines`. Поддержка корутин опциональна:
TornadoFX компилируется против `kotlinx-coroutines-core` и
`kotlinx-coroutines-javafx` со scope `provided` и никогда не тянет их
транзитивно — объявите их сами, если используете корутинное API:

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

Классические цепочки `runAsync { } ui { }` работают и без этих зависимостей
на classpath.

### Suspend-тела задач

Тела `runAsync` и `task` могут приостанавливаться: вызывайте другие suspend-
функции, переходите в UI-поток через `ui { }` (suspend-член задачи) и
отчитывайтесь о прогрессе через `updateProgress`/`updateMessage`:

```kotlin
runAsync(status) {
    updateMessage("Загружаю клиентов...")
    val customers = api.fetchCustomers()          // suspend-вызов
    ui { customerTable.items = customers.asObservable() }
    customers                                     // станет значением задачи
} ui {
    statusLabel.text = "Загружено клиентов: ${it.size}"
}
```

Внутри suspend-тела `ui { }` немедленно отправляет блок в JavaFX Application
Thread и возобновляет тело с результатом блока; если вызов происходит прямо
в UI-потоке, блок выполняется без перехода между потоками. Обратите внимание
на отличие от цепочки `... ui { }`: цепочка навешивает колбэк успеха на
задачу, а suspend-`ui { }` выполняет переход прямо сейчас.

### Ожидание задач

Любую TornadoFX-задачу — и любого воркера JavaFX `Task`/`Service` — можно
ожидать из корутины без блокировки потока. Ошибка задачи выбрасывается в
точке ожидания, а отменённая задача возобновляет ожидающую корутину с
`CancellationException`:

```kotlin
scope.launch {
    val customers = downloadTask.await()
    withContext(Dispatchers.JavaFx) { customerTable.items = customers }
}
```

### Маппинг отмены

Отмена работает в обоих направлениях:

- Отмена задачи отменяет выполняющееся тело: оно останавливается в ближайшей
  точке приостановки, а задача переходит в состояние cancelled.
- Отмена корутины, ожидающей запущенную задачу, отменяет эту задачу —
  фоновая работа не переживает ожидающую её корутину.
- `CancellationException`, вылетевший из тела, трактуется как отмена, а не
  как ошибка: задача переходит в состояние cancelled.

```kotlin
val download = runAsync(status) {
    updateMessage("Скачиваю...")
    api.download().await()
} ui { showResult(it) }

// Позже, откуда угодно:
download.cancel()   // тело остановится в ближайшей точке приостановки
```
