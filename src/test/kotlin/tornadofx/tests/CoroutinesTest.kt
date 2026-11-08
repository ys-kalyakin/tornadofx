package tornadofx.tests

import javafx.application.Platform
import javafx.concurrent.Task
import javafx.concurrent.Worker
import javafx.stage.Stage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.testfx.api.FxToolkit
import tornadofx.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@Timeout(30)
class CoroutinesTest {
    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    // Task failure reports through the JVM default uncaught exception handler
    // on the FX thread (see task()/runAsync()). TestFX installs a global handler
    // that captures those and replays them inside whichever test runs next, so
    // our intentionally failing task bodies must be detached from it.
    private var savedUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null

    @BeforeEach
    fun detachTestFxExceptionHandler() {
        savedUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(null)
    }

    @AfterEach
    fun restoreTestFxExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(savedUncaughtExceptionHandler)
    }

    // -- await() ------------------------------------------------------------------

    @Test
    fun awaitResumesWithTaskValue() {
        val task = runAsync { 42 }
        assertEquals(42, runBlocking { task.await() })
    }

    @Test
    fun awaitRethrowsTaskFailure() {
        val task = runAsync<String> { throw IllegalStateException("boom") }
        val thrown = runBlocking { assertFailsWith<IllegalStateException> { task.await() } }
        assertEquals("boom", thrown.message)
    }

    @Test
    fun awaitOnAlreadySucceededTaskReturnsValue() {
        val task = runAsync { "done" }
        assertEquals(Worker.State.SUCCEEDED, awaitState(task, Worker.State.SUCCEEDED))
        assertEquals("done", runBlocking { task.await() })
    }

    @Test
    fun awaitOnAlreadyFailedTaskRethrows() {
        val task = runAsync<String> { throw IllegalStateException("early") }
        assertEquals(Worker.State.FAILED, awaitState(task, Worker.State.FAILED))
        assertFailsWith<IllegalStateException> { runBlocking { task.await() } }
    }

    @Test
    fun awaitOnCancelledTaskThrowsCancellationException() {
        val started = CountDownLatch(1)
        val task = runAsync { started.countDown(); delay(60_000); true }
        try {
            assertTrue(started.await(5, TimeUnit.SECONDS))
            task.cancel()
            assertEquals(Worker.State.CANCELLED, awaitState(task, Worker.State.CANCELLED))
            assertFailsWith<CancellationException> { runBlocking { task.await() } }
        } finally {
            task.cancel()
        }
    }

    @Test
    fun cancellingAwaitingCoroutineCancelsTask() {
        val started = CountDownLatch(1)
        val task = runAsync { started.countDown(); delay(60_000); true }
        try {
            assertTrue(started.await(5, TimeUnit.SECONDS))
            runBlocking {
                val awaiting = launch(Dispatchers.Default) { task.await() }
                delay(300) // let the awaiting coroutine reach its suspension point
                awaiting.cancelAndJoin()
            }
            assertEquals(Worker.State.CANCELLED, awaitState(task, Worker.State.CANCELLED))
        } finally {
            task.cancel()
        }
    }

    // -- suspend bodies inside task/runAsync --------------------------------------

    @Test
    fun suspendBodyRunsOffTheFxThreadAndYieldsValue() {
        var onFxThread = true
        val task = runAsync {
            onFxThread = Platform.isFxApplicationThread()
            "result"
        }
        assertEquals("result", runBlocking { task.await() })
        assertFalse(onFxThread)
    }

    @Test
    fun suspendBodyMaySuspendAndUseProgress() {
        val task = runAsync {
            updateMessage("working")
            updateProgress(1, 2)
            delay(50)
            updateMessage("done")
            updateProgress(2, 2)
            "ok"
        }
        assertEquals("ok", runBlocking { task.await() })
        val (message, progress) = onFx { task.message to task.progress }
        assertEquals("done", message)
        assertEquals(1.0, progress)
    }

    @Test
    fun cancellingTaskCancelsRunningSuspendBody() {
        val suspended = CountDownLatch(1)
        val cancelledInBody = AtomicBoolean(false)
        val task = runAsync {
            try {
                suspended.countDown()
                delay(60_000)
                true
            } catch (e: CancellationException) {
                cancelledInBody.set(true)
                throw e
            }
        }
        assertTrue(suspended.await(5, TimeUnit.SECONDS))
        runBlocking { delay(200) } // make sure the body reached its suspension point
        task.cancel()
        assertEquals(Worker.State.CANCELLED, awaitState(task, Worker.State.CANCELLED))
        // The CancellationException is delivered to the body asynchronously after
        // the state transition, so wait for the body to observe it.
        val deadline = System.currentTimeMillis() + 5000
        while (!cancelledInBody.get() && System.currentTimeMillis() < deadline) Thread.sleep(10)
        assertTrue(cancelledInBody.get())
    }

    @Test
    fun cancellationExceptionFromBodyLandsTaskInCancelledState() {
        val task = runAsync<String> { throw CancellationException("body cancelled itself") }
        assertEquals(Worker.State.CANCELLED, awaitState(task, Worker.State.CANCELLED))
    }

    @Test
    fun taskStatusTracksSuspendTask() {
        val status = TaskStatus()
        val task = runAsync(status = status) {
            val runningDuringBody = ui { status.item?.isRunning ?: false }
            if (runningDuringBody) "ok" else "not-running"
        }
        assertEquals("ok", runBlocking { task.await() })
        assertEquals(Worker.State.SUCCEEDED, awaitState(task, Worker.State.SUCCEEDED))
        assertTrue(onFx { status.completed.value })
        assertFalse(onFx { status.running.value })
    }

    @Test
    fun failChainReceivesFailureFromSuspendBody() {
        val failed = CountDownLatch(1)
        var caught: Throwable? = null
        runBlocking {
            runAsync<String> { throw IllegalStateException("boom") } fail { caught = it; failed.countDown() }
        }
        assertTrue(failed.await(5, TimeUnit.SECONDS))
        assertEquals("boom", caught?.message)
    }

    // -- classic lambda compatibility ----------------------------------------------

    @Test
    fun classicLambdaAndChainsStillWork() {
        val uiDone = CountDownLatch(1)
        var uiValue: Int? = null
        val task = runAsync { 21 * 2 } // non-suspending lambda against the suspend body parameter
        runBlocking { assertEquals(42, task.await()) }
        task ui { uiValue = it; uiDone.countDown() }
        assertTrue(uiDone.await(5, TimeUnit.SECONDS))
        assertEquals(42, uiValue)
    }

    // -- ui { } --------------------------------------------------------------------

    @Test
    fun uiRunsBlockOnFxThreadAndResumesWithResult() {
        val fxTask = FXTask(status = null, func = { Unit })
        val (onFxThread, value) = runBlocking {
            withContext(Dispatchers.Default) {
                fxTask.ui { Platform.isFxApplicationThread() to 42 }
            }
        }
        assertTrue(onFxThread)
        assertEquals(42, value)
    }

    @Test
    fun uiFromInsideSuspendBodyHopsToTheFxThreadImmediately() {
        var statementRanOnFx = false
        val task = runAsync {
            // Statement form: the FXTask.ui member wins over the Task.ui infix,
            // so this dispatches immediately onto the FX thread instead of
            // deferring to the task's success handler.
            ui { statementRanOnFx = Platform.isFxApplicationThread() }
            val (onFxThread, value) = ui { Platform.isFxApplicationThread() to "from-body" }
            statementRanOnFx && onFxThread && value == "from-body"
        }
        assertTrue(runBlocking { task.await() })
    }

    @Test
    fun uiFromTheFxThreadCompletesWithoutDeadlock() {
        val fxTask = FXTask(status = null, func = { Unit })
        val done = CountDownLatch(1)
        var result: Int? = null
        runLater {
            try {
                runBlocking { result = fxTask.ui { 7 } }
            } finally {
                done.countDown()
            }
        }
        assertTrue(done.await(5, TimeUnit.SECONDS))
        assertEquals(7, result)
    }

    // Task state/property getters are guarded by Task.checkThread and must run
    // on the FX application thread.
    private fun <T> onFx(op: () -> T): T {
        if (Platform.isFxApplicationThread()) return op()
        var result: Any? = null
        var error: Throwable? = null
        val latch = CountDownLatch(1)
        Platform.runLater {
            try {
                result = op()
            } catch (t: Throwable) {
                error = t
            } finally {
                latch.countDown()
            }
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS))
        error?.let { throw it }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    private fun awaitState(task: Task<*>, target: Worker.State): Worker.State {
        val deadline = System.currentTimeMillis() + 5000
        var state = onFx { task.state }
        while (state != target && System.currentTimeMillis() < deadline) {
            Thread.sleep(5)
            state = onFx { task.state }
        }
        return state
    }

}
