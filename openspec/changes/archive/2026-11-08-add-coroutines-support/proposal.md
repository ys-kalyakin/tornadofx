# Proposal

## Why

TornadoFX 2.0 modernized the platform stack but kept the 1.7-era async model: callback chains (`runAsync {} ui {}`) over hand-rolled thread pools. There is no coroutine support anywhere in the framework, which is the largest remaining gap between this fork and modern Kotlin practice — consumers currently hand-roll `runBlocking`/`runLater` bridges between coroutines and the FX thread. Kotlin UI frameworks today are expected to interoperate with `kotlinx.coroutines`; adding it makes the fork a credible "modern stack" choice for new projects.

## What Changes

- Add `kotlinx-coroutines-core` and `kotlinx-coroutines-javafx` as **provided-scope** dependencies (same pattern as `Rest`/HttpClient 5): consumers opt in by declaring kotlinx-coroutines themselves; the core artifact neither pulls them transitively nor requires them unless the new API is used.
- Add a coroutine bridge in the core artifact (new `tornadofx` source file, e.g. `Coroutines.kt`):
  - `Task<T>.await()`: suspend until a TornadoFX/JavaFX `Task` completes, returning its value; task failure is rethrown, cancellation is mapped to `CancellationException`.
  - `runAsync`/`task` accept suspend bodies: their body parameter is widened to `suspend FXTask<*>.() -> T` — a single entry point (suspend overloads proved unimplementable: Kotlin 2.4 reports overload-resolution ambiguity for every classic lambda call, so this direction was chosen with the maintainer; classic lambda literals compile and behave unchanged, pre-typed classic body values/references are the only source-incompatible form, and the JVM descriptors of `task`/`runAsync` change — accepted, the 2.x line has no binary-compatibility baseline to preserve). Coroutine code runs inside the existing `FXTask`/thread-pool machinery, so `TaskStatus`, `updateProgress`/`updateMessage`, and the `.ui`/`.fail`/`.cancel` chains keep working unchanged.
  - `ui { }` suspend helper exposed as a member of `FXTask` (runs a block on the FX application thread via `Dispatchers.JavaFx`, inline when already on the FX thread); the member form guarantees bare `ui { }` inside suspend bodies dispatches immediately instead of deferring to the task's success handler.
  - Cancellation mapped in both directions: cancelling the `Task` cancels the running coroutine; cancelling the awaiting coroutine cancels the task.
- Non-goals (explicitly out of scope for this change): suspend variants of the `Rest` client, component-lifecycle-bound coroutine scopes (UIComponent has no destroy hook today), Flow-based EventBus, replacing the `Async.kt` executor machinery.

## Capabilities

### New Capabilities

- `coroutines-async`: suspend/coroutine integration with the TornadoFX async machinery — awaiting TornadoFX tasks from coroutines, running coroutine bodies inside `FXTask`, dispatching onto the JavaFX application thread, and cancellation mapping between `Task` and coroutine `Job`.

### Modified Capabilities

<!-- None: existing specs (platform-compatibility, rest-client, ui-controls, release-publishing) are unaffected; the new API is additive and compiles warning-clean under the same baselines. -->

## Impact

- **Dependencies** (`pom.xml`): `kotlinx-coroutines-core` and `kotlinx-coroutines-javafx` (stable 1.10.x line) added with `provided` scope plus version properties; consumers who use the new API must declare kotlinx-coroutines themselves (documented).
- **Code**: new file `src/main/java/tornadofx/Coroutines.kt`; no modification of `Async.kt` behavior beyond additive overloads.
- **Tests**: new headless-compatible tests (existing Monocle/headless profile) covering await, suspend `runAsync`, FX-thread dispatch, and cancellation mapping in both directions.
- **Docs**: README feature list and docs-site async page gain a coroutines section; CHANGELOG entry under 2.0.1 (unreleased).
- **Compatibility**: existing lambda-literal async usage compiles and behaves identically (existing test suite stays green). Accepted, documented deltas: `task`/`runAsync` body parameters are now suspend-typed (JVM descriptors change; pre-typed classic body values and non-suspend function references must become lambda literals), a `CancellationException` escaping a body maps to the cancelled state, and `FXTask.func` holds the execution wrapper. Main sources stay warning-free.
