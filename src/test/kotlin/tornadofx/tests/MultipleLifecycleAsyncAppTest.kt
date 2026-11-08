package tornadofx.tests

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testfx.api.FxRobot
import org.testfx.api.FxToolkit
import tornadofx.*
import tornadofx.testapps.MultipleLifecycleAsyncView
import java.util.concurrent.CountDownLatch
import kotlin.test.assertEquals

//@RunWith(Parameterized::class)
class MultipleLifecycleAsyncAppTest(val round: Int) {
//
//    companion object {
//        @JvmStatic
//        @Parameterized.Parameters
//        fun data(): Collection<Array<Int>> {
//            return listOf(arrayOf(1), arrayOf(2))
//        }
//    }
//
//    lateinit var robot: FxRobot
//    lateinit var app: App
//
//    @BeforeEach
//    fun before() {
//        FxToolkit.registerPrimaryStage()
//        app = App(MultipleLifecycleAsyncView::class)
//        FxToolkit.setupApplication { app }
//        robot = FxRobot()
//
//        println("round: $round")
//    }
//
//    @AfterEach
//    fun after() {
//        FxToolkit.cleanupStages()
//        FxToolkit.cleanupApplication(app)
//    }
//
//    @Test(timeout = 20000)
//    fun itShouldSurviveRunAsyncMultipleTimes() {
//        val latch = CountDownLatch(2)
//        val view: MultipleLifecycleAsyncView = find()
//        view.counterProperty.onChange { latch.countDown() }
//
//        robot.clickOn(".button")
//
//        latch.await()
//        assertEquals(2, view.counter)
//    }
}