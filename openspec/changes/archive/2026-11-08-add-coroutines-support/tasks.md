# Tasks

## 1. Build setup

- [x] 1.1 Add `kotlinx.coroutines.version` property (stable 1.10.x line) and `kotlinx-coroutines-core`/`kotlinx-coroutines-javafx` as provided-scope dependencies to `pom.xml`; verify `./mvnw -Pheadless compile` resolves them from Maven Central and main sources stay warning-free

## 2. Coroutine bridge implementation

- [x] 2.1 Create `src/main/java/tornadofx/Coroutines.kt` with `suspend fun <T> Task<T>.await()` (suspendCancellableCoroutine, already-complete states resume immediately, failure rethrown, invokeOnCancellation cancels the task); verify with a headless test covering success, failure, and awaiting an already-completed task
- [x] 2.2 Make the body parameter of `task`/`runAsync` suspend-typed (single entry point, revised design D2 after overload ambiguity was proven empirically), running the body in `runBlocking` on the existing worker pools, mapping task cancellation to job cancellation, treating body `CancellationException` as cancellation rather than failure, and clearing the worker thread's interrupt status; verified with headless tests: body runs off the FX thread, task value is the body result, cancelling the running task cancels the body and lands the task in the cancelled state
- [x] 2.3 Add the suspend `FXTask.ui` member (withContext over the JavaFx dispatcher with inline execution on the FX thread, via an explicit `Platform.isFxApplicationThread()` check — `Dispatchers.JavaFx` always dispatches in kotlinx-coroutines 1.10.2); verified with headless tests: block executes on the FX application thread, caller resumes with the result, and calling from the FX thread itself completes without deadlock
- [x] 2.4 Verify overload resolution and compatibility: both `runAsync { }` (classic lambda) and `runAsync { suspend body }` compile and behave per their own signatures; existing chains `.ui`/`.fail`/`.cancel` and `TaskStatus` work on coroutine-backed tasks; run the full existing test suite to confirm no regression (`./mvnw -Pheadless test`, 385 tests green)

## 3. Documentation

- [x] 3.1 Add a coroutines section to the docs-site async page (EN + RU, keep EN/RU parity per `docs/check-parity.py`) covering `await`, suspend `runAsync`, `ui { }`, cancellation mapping, and the opt-in kotlinx-coroutines dependency; verify `check-parity.py` passes and the snippet compiles as written
- [x] 3.2 Update README feature list and add the kotlinx-coroutines opt-in note; add the CHANGELOG entry under the unreleased 2.0.1 section; verify the rendered docs pages exist in the mkdocs nav
