package tornadofx

import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.concurrent.Task
import javafx.concurrent.Worker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.javafx.JavaFx

/**
 * Suspend until this task completes and return its value.
 *
 * This is the coroutine-side bridge to the TornadoFX async machinery: any
 * [Task] (including [FXTask] and JavaFX [javafx.concurrent.Service] workers)
 * can be awaited from a coroutine without blocking any thread.
 *
 * - A task that fails rethrows its exception at the await site.
 * - A task that is cancelled resumes the awaiting coroutine with a
 *   [CancellationException].
 * - A task whose state is already terminal resumes immediately with the
 *   known result, without attaching any handlers.
 * - Cancelling the awaiting coroutine cancels the underlying task, so no
 *   orphaned background work survives the awaiting coroutine.
 *
 * ```kotlin
 * runAsync {
 *     val data = someTask.await()
 *     ui { label.text = data }
 * }
 * ```
 */
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    val attach: () -> Unit = {
        fun deliverResult() {
            when (state) {
                Worker.State.SUCCEEDED -> continuation.resumeWith(Result.success(value))
                Worker.State.FAILED -> continuation.resumeWith(Result.failure(exception ?: IllegalStateException("Task failed without an exception")))
                Worker.State.CANCELLED -> continuation.cancel(CancellationException("Task was cancelled"))
                else -> Unit // READY/SCHEDULED/RUNNING — keep waiting for a terminal state
            }
        }

        val stateListener = ChangeListener<Worker.State> { _, _, _ -> deliverResult() }
        stateProperty().addListener(stateListener)
        continuation.invokeOnCancellation {
            // Task state must be read on the FX thread, so detach there too.
            runLater { stateProperty().removeListener(stateListener) }
            cancel()
        }
        deliverResult()
    }

    // Task state can only be read on the FX application thread (checkThread),
    // so the whole attachment runs there, like the ui/fail/cancel chains do.
    // State transitions are also delivered on the FX thread, so the listener
    // is always in place before the first transition reaches it.
    if (Platform.isFxApplicationThread()) attach() else runLater(attach)
}

/**
 * Shared implementation behind [task]/[runAsync]: the body runs inside
 * [runBlocking] on a framework worker thread, exactly where a classic task
 * body would run, so the thread is blocked for the whole duration of the
 * task.
 *
 * The body's [kotlinx.coroutines.Job] is wired to the task lifecycle without
 * touching `setOnCancelled`, which belongs to the user's `.cancel { }` chain:
 * cancelling the task cancels the body's coroutine, which stops at its next
 * suspension point or cancellation check. A [CancellationException] escaping
 * the body is treated as cancellation, not as a failure: the task lands in
 * the cancelled state instead of failed.
 *
 * Cancelling the task delivers an interrupt to the worker thread; the flag is
 * cleared before the thread returns to the pool so subsequent tasks are not
 * poisoned by it.
 */
internal fun <T> coroutineTask(daemon: Boolean = false, status: TaskStatus? = null, func: suspend FXTask<*>.() -> T): Task<T> {
    val job = SupervisorJob()
    val fxTask = FXTask(status, func = {
        try {
            runBlocking(job) { func() }
        } catch (e: CancellationException) {
            // Cancellation escaping the body is cancellation, not failure:
            // land the task in the cancelled state (no-op if it was already
            // cancelled from the outside).
            cancel()
            throw e
        } finally {
            job.complete()
            // Cancelling the task delivers an interrupt to this worker thread;
            // clear it so the pooled thread is not poisoned for later tasks.
            Thread.interrupted()
        }
    })
    // Task state must be read on the FX thread, so attach there; transitions
    // are queued on the same FX event queue, so the listener is always in
    // place before a CANCELLED transition reaches it.
    val attachCancellationListener = {
        fxTask.stateProperty().addListener { _, _, newState ->
            if (newState == Worker.State.CANCELLED) job.cancel()
        }
    }
    if (Platform.isFxApplicationThread()) attachCancellationListener() else runLater(attachCancellationListener)
    fxTask.setOnFailed {
        (Thread.getDefaultUncaughtExceptionHandler() ?: dummyUncaughtExceptionHandler).uncaughtException(Thread.currentThread(), fxTask.exception)
    }
    if (daemon) {
        tfxDaemonThreadPool.execute(fxTask)
    } else {
        tfxThreadPool.execute(fxTask)
    }
    return fxTask
}

/**
 * Dispatch [block] onto the JavaFX Application Thread and resume the caller
 * with its result. Backs the [FXTask.ui] suspend member.
 *
 * Runs inline when already on the FX Application Thread: the JavaFx dispatcher
 * always goes through [Platform.runLater], so a plain `withContext` from the
 * FX thread would deadlock against the blocked event loop instead of running
 * inline.
 */
internal suspend fun <R> withFxThread(block: () -> R): R =
    if (Platform.isFxApplicationThread()) block()
    else withContext(Dispatchers.JavaFx) { block() }
