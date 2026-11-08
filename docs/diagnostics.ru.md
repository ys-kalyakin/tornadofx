# Диагностика

Инструменты, чтобы понять, почему UI выглядит именно так, и куда попадают
ошибки, когда они случаются.

## Отладчик layout

Любое запущенное приложение TornadoFX открывает отладчик layout по нажатию
**J** вместе с Meta (клавиша Windows/Command) и Alt. Он показывает дерево
компонентов текущей сцены, границы каждой ноды и позволяет править живые
свойства — например, текст лейбла — прямо из окна отладчика.

Чтобы открыть его программно, передайте сцену:

```kotlin
button("Debug layout") {
    setOnAction {
        currentWindow?.scene?.let { LayoutDebugger.debug(it) }
    }
}
```

Хоткей — это `FX.layoutDebuggerShortcut`; присвойте новую `KeyCodeCombination`,
чтобы переназначить, или `null`, чтобы отключить:

```kotlin
FX.layoutDebuggerShortcut = KeyCodeCombination(
    KeyCode.D, KeyCodeCombination.CONTROL_DOWN, KeyCodeCombination.ALT_DOWN
)
FX.layoutDebuggerShortcut = null   // отключить хоткей отладчика
```

## Флаги командной строки dev-режима

Приложение читает флаги из безымянных параметров запуска:

| Флаг | Эффект |
|---|---|
| `--dev-mode` | включает всё перечисленное ниже |
| `--live-stylesheets` | стили перезагружаются, когда окно возвращает фокус |
| `--live-views` | вью перезагружаются, когда окно возвращает фокус |
| `--dump-stylesheets` | обработанные стили выгружаются при старте |

```kotlin
launch<MyApp>(args)   // запуск: java -jar app.jar --dev-mode
```

## Обработка ошибок

При старте TornadoFX ставит `DefaultErrorHandler` хендлером необработанных
исключений JVM (только если своего еще нет). Исключение, вырвавшееся из
любого потока, логируется и показывается в растягиваемом диалоге с полным
stack trace — REST-ошибки (`RestException`) дополнительно отображают
HTTP-статус и тело ответа в табах.

Решать для каждого исключения, показывать ли диалог, можно через
`DefaultErrorHandler.filter`; вызов `event.consume()` глушит его:

```kotlin
DefaultErrorHandler.filter = { event ->
    if (event.error is MyExpectedException) {
        log.info("Expected failure: ${event.error.message}")
        event.consume()   // без диалога для ожидаемых сбоев
    }
}
```

Если поставить свой `Thread.setDefaultUncaughtExceptionHandler` до старта
приложения, TornadoFX оставит его в покое.

## Про журнал событий

Журнал событий (event log) из upstream TornadoFX в этот форк не входит —
класс остался только заглушкой. Для сбоев используйте обработчик ошибок выше,
а для собственной диагностики уровня приложения — [EventBus](events.md).
