# Diagnostics

Tools for finding out why the UI looks the way it does, and where errors go
when they happen.

## The layout debugger

Any running TornadoFX app opens the layout debugger with **J** pressed
together with Meta (the Windows/Command key) and Alt. It shows the component
tree of the current scene, the bounds of each node, and lets you edit live
properties — such as a label's text — directly from the debugger window.

To open it programmatically, hand it a scene:

```kotlin
button("Debug layout") {
    setOnAction {
        currentWindow?.scene?.let { LayoutDebugger.debug(it) }
    }
}
```

The shortcut is `FX.layoutDebuggerShortcut`; assign a new
`KeyCodeCombination` to rebind it, or `null` to disable:

```kotlin
FX.layoutDebuggerShortcut = KeyCodeCombination(
    KeyCode.D, KeyCodeCombination.CONTROL_DOWN, KeyCodeCombination.ALT_DOWN
)
FX.layoutDebuggerShortcut = null   // disable the debugger hotkey
```

## Dev mode command line flags

The application reads flags from its unnamed launch parameters:

| Flag | Effect |
|---|---|
| `--dev-mode` | enables all of the below |
| `--live-stylesheets` | stylesheets reload when a window regains focus |
| `--live-views` | views reload when a window regains focus |
| `--dump-stylesheets` | processed stylesheets are dumped on startup |

```kotlin
launch<MyApp>(args)   // run with: java -jar app.jar --dev-mode
```

## Error handling

At startup TornadoFX installs `DefaultErrorHandler` as the JVM's uncaught
exception handler (only if none is set). An exception that escapes any thread
is logged and shown in a resizable dialog with the full stack trace — REST
errors (`RestException`) additionally render the HTTP status and response
body in tabs.

Decide per exception whether the dialog should appear through
`DefaultErrorHandler.filter`; call `event.consume()` to swallow it:

```kotlin
DefaultErrorHandler.filter = { event ->
    if (event.error is MyExpectedException) {
        log.info("Expected failure: ${event.error.message}")
        event.consume()   // no dialog for expected failures
    }
}
```

If you install your own `Thread.setDefaultUncaughtExceptionHandler` before
the app starts, TornadoFX leaves it in place.

## About the event log

The upstream TornadoFX event log UI is not part of this fork — the class is
present only as a stub. Use the error handler above for failures, and the
[EventBus](events.md) for application-level diagnostics of your own.
