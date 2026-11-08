# coroutines-async Specification

## Purpose

Lets Kotlin coroutines interoperate with the TornadoFX async machinery: coroutines can await TornadoFX tasks, TornadoFX tasks can run coroutine bodies on the framework worker pools, blocks can be dispatched onto the JavaFX application thread, and cancellation propagates between the two worlds.

## Requirements

### Requirement: Await task completion from a coroutine

A coroutine SHALL be able to suspend until a TornadoFX task completes and resume with the task's value without blocking any thread. If the task fails, the failure SHALL be rethrown at the await site; if the task is already complete, `await` SHALL resume immediately with the known result.

#### Scenario: Successful task resumes with its value

- **WHEN** a coroutine awaits a task that completes successfully
- **THEN** the coroutine resumes with the task's value

#### Scenario: Failed task rethrows the failure

- **WHEN** a coroutine awaits a task that fails with an exception
- **THEN** the same exception is thrown at the await site

#### Scenario: Awaiting an already-completed task does not suspend

- **WHEN** a coroutine awaits a task whose state is already succeeded or failed
- **THEN** the result (or failure) is delivered without waiting for further task events

### Requirement: Cancelling the awaiting coroutine cancels the task

If the coroutine awaiting a running task is cancelled, the underlying task SHALL be cancelled so no orphaned background work survives the awaiting coroutine.

#### Scenario: Awaiting coroutine cancelled while task runs

- **WHEN** the coroutine awaiting a running task is cancelled
- **THEN** the task transitions to the cancelled state and its cancel handlers run

### Requirement: Cancelling a coroutine-backed task cancels its body

A task created from a suspend body SHALL propagate cancellation into the running coroutine: cancelling the task cancels the coroutine, which stops at its next suspension point or cancellation check, and the task reaches the cancelled state.

#### Scenario: Task cancelled while its suspend body is suspended

- **WHEN** a coroutine-backed task is cancelled while its body is in progress
- **THEN** the body's coroutine is cancelled and the task reports the cancelled state

### Requirement: Suspend bodies run inside the existing task machinery

`runAsync` and `task` SHALL accept suspend bodies executed as TornadoFX tasks on the framework worker pools: the body MUST NOT run on the JavaFX application thread, the returned task SHALL keep the existing `ui`/`fail`/`cancel` chains and `TaskStatus` binding working, and progress/message updates SHALL remain callable from inside the body. Non-suspending lambda bodies SHALL compile unchanged and run as before.

#### Scenario: Suspend body runs off the FX thread and yields a task value

- **WHEN** `runAsync` is called with a suspend body and the returned task completes
- **THEN** the body did not execute on the JavaFX application thread and the task's value is the body's result

#### Scenario: Existing result chains still receive the outcome

- **WHEN** the `ui`/`fail`/`cancel` chains are attached to a coroutine-backed task
- **THEN** they fire exactly as they do for classic tasks, on the FX application thread

#### Scenario: TaskStatus tracks a coroutine-backed task

- **WHEN** a `TaskStatus` is supplied to a coroutine-backed task
- **THEN** running/completed state is reported to that status like for classic tasks

### Requirement: Dispatch a block onto the FX application thread

A suspend helper SHALL run a block on the JavaFX application thread and resume the caller with the block's result after it completes. The helper SHALL be exposed as a member of `FXTask`, so a bare `ui { }` call inside a suspend task body resolves to it rather than to the `Task.ui` infix (which would defer the block to the task's success handler). Calling it while already on the FX application thread SHALL run the block without a thread hop and without deadlock.

#### Scenario: Block dispatched from a worker coroutine

- **WHEN** a suspend body calls the FX-thread helper with a block
- **THEN** the block runs on the JavaFX application thread and the caller resumes with its result

#### Scenario: Called from the FX thread itself

- **WHEN** the helper is called from code already running on the JavaFX application thread
- **THEN** the block runs without deadlock

### Requirement: Coroutine support is an opt-in dependency

The coroutine API SHALL be shipped in the core artifact with kotlinx-coroutines as a provided-scope dependency, so consumers who do not use the coroutine API are unaffected: classic async usage SHALL work without kotlinx-coroutines on the runtime classpath, and consumers using the new API declare kotlinx-coroutines themselves.

#### Scenario: Classic async usage without kotlinx-coroutines on the classpath

- **WHEN** a consumer uses only the classic `runAsync`/`ui`/`fail` API without declaring kotlinx-coroutines
- **THEN** no kotlinx-coroutines class is loaded and the behavior is unchanged

#### Scenario: Coroutine usage with kotlinx-coroutines declared

- **WHEN** a consumer declares kotlinx-coroutines and uses the coroutine API
- **THEN** the API works against the coroutines version the consumer provides

### Requirement: Classic lambda usage remains source-compatible

Widening the body parameter of `runAsync`/`task` to a suspend function type MUST keep every existing lambda-literal usage compiling and behaving identically: classic bodies run on the framework worker threads, failures land in the failed state, and cancellation reports the cancelled state. The `ui`/`fail`/`cancel` chains and `FXTask` itself keep their signatures and behavior.

#### Scenario: Existing lambda-literal code compiles and behaves identically

- **WHEN** code written against the pre-change async API using lambda literals is compiled and run against the new build
- **THEN** its behavior is identical to the previous release, as covered by the existing test suite

#### Scenario: Documented deltas apply to pre-existing classic usage

- **WHEN** the widened suspend body parameter meets pre-existing classic code
- **THEN** the accepted deltas apply: a `CancellationException` escaping the body maps to the cancelled state instead of failed; the worker thread's interrupt flag is cleared when a cancelled body finishes; the JVM descriptors of `task`/`runAsync` change (pre-typed classic body values and non-suspend function references must become lambda literals); `FXTask.func` holds the execution wrapper rather than the user's body
