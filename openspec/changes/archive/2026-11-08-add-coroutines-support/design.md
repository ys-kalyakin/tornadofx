# Design

## Context

The fork's async machinery (`src/main/java/tornadofx/Async.kt`) is built around `FXTask<T>` (extends `javafx.concurrent.Task`, runs `func: FXTask<*>.() -> T` on dedicated `tfxThreadPool`/`tfxDaemonThreadPool` executors) plus callback combinators (`ui`/`success`/`fail`/`cancel` infix chains). There is no coroutine usage anywhere in the codebase. The fork already established a pattern for optional integrations: `Rest` compiles against HttpClient 5 which is `provided` scope — consumers declare it themselves. Constraints inherited from existing specs: main sources stay warning-free under the pinned JDK 21 / Kotlin 2.x toolchain, no JDK-internal APIs, published bytecode is Java 17.

## Goals / Non-Goals

**Goals:**

- A thin, idiomatic bridge between `kotlinx.coroutines` and the existing task machinery, additive only.
- Cancellation that works in both directions (task ↔ coroutine) without changing task semantics.
- Zero impact for consumers who never touch the coroutine API.

**Non-Goals:**

- Suspend variants of the `Rest` client (natural follow-up, separate change).
- Component-lifecycle-bound scopes (UIComponent has no destroy hook today; introducing one is its own design).
- Flow-based EventBus, replacing the executor pools, or a Kotlin version floor bump.

## Decisions

### D1: Ship in the core artifact with provided-scope dependencies (no separate module)

`kotlinx-coroutines-core` and `kotlinx-coroutines-javafx` are added as `provided` dependencies; the bridge lives in a new file `src/main/java/tornadofx/Coroutines.kt` in package `tornadofx`.

- *Why*: this is exactly the precedent `Rest`/HttpClient 5 set — opt-in dependency, no transitive leakage, single-artifact release/publishing/dokka pipeline untouched. A separate `tornadofx-coroutines` module would force a multi-module restructure (parent pom, per-artifact signing/javadoc/central publishing, release-spec changes) disproportionate to a bridge of this size.
- *Alternatives considered*: (a) separate Maven module — rejected for build complexity; revisitable if the coroutines surface grows; (b) optional (non-transitive) compile dependency — weaker guarantee than `provided` and diverges from the established pattern.

### D2: API surface

```kotlin
// interop out: any Task -> coroutine
suspend fun <T> Task<T>.await(): T

// interop in: coroutine body -> task; the body parameter is widened to the
// suspend function type in place — a single entry point, no overloads
fun <T> task(daemon: Boolean = false, taskStatus: TaskStatus? = null, func: suspend FXTask<*>.() -> T): Task<T>
fun <T> runAsync(daemon: Boolean = false, status: TaskStatus? = null, func: suspend FXTask<*>.() -> T): Task<T>

// FX-thread dispatch: a suspend member of FXTask, mirroring the Task.ui naming
class FXTask<T> { suspend fun <R> ui(block: () -> R): R }
```

- *Why the body parameter is widened instead of adding suspend overloads*: D2 originally prescribed overloads of `task`/`runAsync` accepting `suspend FXTask<*>.() -> T`. Empirically (Kotlin 2.4.21 K2) that is unimplementable without breaking consumers: a non-suspend lambda converts to a suspend function type, so every classic `task { }`/`runAsync { }` call became an overload-resolution ambiguity — 110 compile errors over 17 internal call sites plus the whole existing test suite. Widening the body parameter in place keeps every lambda-literal call site compiling unchanged. Accepted trade-offs (user decision 2026-11-08, the 2.x line has no binary-compatibility baseline to preserve): the JVM descriptors of `task`/`runAsync` change; pre-typed classic body values and non-suspend function references no longer convert (lambda literals do); a `CancellationException` escaping the body maps to the cancelled state (previously: failed); `FXTask.func` holds the runBlocking wrapper.
- *Alternatives considered*: separate `taskSuspend`/`runAsyncSuspend` entry points (rejected: two names for one mechanism); `CoroutineScope`-scoped overloads (rejected: cannot create tasks from plain UI code, and `runAsync` inside `runBlocking` still became ambiguous).
- *Why `ui` is a member of FXTask*: a bare `ui { }` inside a suspend body is applicable to the `Task.ui` infix through the implicit FXTask receiver. K2 resolution cannot be steered away from it (top-level suspend `ui` silently loses; an `FXTask`-receiver suspend extension resolves ambiguously), so the suspend helper is a member — members always beat extensions in Kotlin, which guarantees immediate FX-thread dispatch inside bodies while classic `runAsync { } ui { }` chains (static receiver type `Task<T>`) keep using the infix. No second helper name (`withUi`) is needed.
- *Alternatives considered*: top-level `suspend fun ui(block)` (silently shadowed by `Task.ui` inside bodies — deferred-behavior trap); `FXTask`-receiver suspend extension `ui` (overload-resolution ambiguity with `Task.ui`).

### D3: Execution and cancellation mechanics

- `task`/`runAsync` delegate to a shared internal `coroutineTask`: an `FXTask` whose `call()` runs the body inside `runBlocking` on the framework worker thread — blocking that thread is correct here: classic tasks occupy the worker for their whole duration anyway. Classic and coroutine bodies take the same path.
- The body's `SupervisorJob` is wired to the task lifecycle: cancelling the task cancels the job (the suspended body resumes with `CancellationException`, `runBlocking` returns); a body that completes normally sets the value, a body that throws sets the failure — `CancellationException` from the body is treated as cancellation, not failure. The wiring attaches a listener to `stateProperty()`; task state must be read on the FX application thread (`Task.checkThread`), so the listener attaches there (via `runLater` when the task is created off the FX thread). Transitions are queued on the same FX event queue, so the listener is always attached before a `CANCELLED` transition is delivered.
- `await()` is implemented with `suspendCancellableCoroutine`: the state listener is attached on the FX application thread (`Task.checkThread` guards `getState`/`stateProperty`), terminal states resume the continuation; `invokeOnCancellation` detaches the listener (on the FX thread) and cancels the task, so a cancelled awaiting coroutine takes its task down. Already-terminal states deliver immediately on the FX thread without waiting for further task events.
- The bridge clears the worker thread's interrupt flag before returning from `call()` — `Task.cancel` delivers an interrupt to the pooled thread, and leaving it set would poison subsequent tasks.
- `ui { }` (the `FXTask` member) is `withContext(Dispatchers.JavaFx) { block() }` off the FX thread. It runs the block inline when already on the FX application thread via an explicit `Platform.isFxApplicationThread()` check: kotlinx-coroutines-javafx 1.10.2's `Dispatchers.JavaFx` always dispatches through `Platform.runLater` (its immediate variant is only reachable as `Dispatchers.Main.immediate`), so a plain `withContext` from the FX thread would deadlock against the blocked event loop.

### D4: Dependency versions

Pin `kotlinx-coroutines-core`/`kotlinx-coroutines-javafx` to the stable 1.10.x line (1.10.2 as of late 2026; 1.11 is still RC) via Maven properties, mirroring the pom's version-property style. kotlinx-coroutines guarantees strict binary compatibility, so consumers on newer 1.x versions work with a build compiled against this floor.

### D5: Verification

New tests in the existing headless (Monocle) profile: await success/failure/already-complete; await-cancel cancels the task; task-cancel cancels the body; suspend `runAsync` runs off the FX thread, feeds `.ui`/`.fail` chains and `TaskStatus`; `ui { }` from a worker coroutine and from the FX thread itself; classic API regression is covered by the untouched existing suite. New code must compile warning-free (platform-compatibility spec).

## Risks / Trade-offs

- [Overload ambiguity between classic and suspend `runAsync`] → the lambda types (`Function1` vs `KSuspendFunction1`) resolve distinctly at compile time; add compile-and-run tests for both call styles.
- [`runBlocking` occupies a worker thread per coroutine task] → same occupancy contract as classic tasks; document that long suspend waits should use plain coroutines rather than long-lived coroutine-backed tasks.
- [Cancellation leaves the worker thread's interrupt flag set] → the bridge clears/restores interrupt status before returning from `call()`.
- [kotlinx-coroutines absence at runtime when the API is touched] → fails fast with `NoClassDefFoundError` at the call site, matching the `Rest`/HttpClient 5 behavior; documented in README and the docs-site async page.
- [JavaFx dispatcher needs an initialized FX toolkit] → same precondition as the whole framework; tests already run against a started toolkit in the headless profile.

## Migration Plan

Additive change; no migration. Rollback is reverting the commit — pom gains two provided dependencies and one new source file, no existing API or behavior is touched. Docs updates (README feature list, docs-site async page EN/RU, CHANGELOG under unreleased 2.0.1) ship with the change.

## Open Questions

None — signature details (parameter ordering/naming polish) are implementation-level and covered by D2/D5.
