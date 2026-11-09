package tornadofx.tests

import javafx.application.Platform
import javafx.stage.Stage
import org.junit.jupiter.api.Test
import org.testfx.api.FxToolkit
import tornadofx.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventBusTest {
    val primaryStage: Stage = FxToolkit.registerPrimaryStage()
    private val success = AtomicBoolean(true)
    private val latch = Latch()

    init {
        FxToolkit.setupFixture {
            Thread.setDefaultUncaughtExceptionHandler { _, t ->
                t.printStackTrace()
                success.set(false)
                latch.countDown()
            }
        }
    }

    val iterations: Int = 100
    val count = AtomicInteger(0)
    val view = object : View() {

        init {
            for (i in 1..iterations) {
                subscribe<FXEvent>(times = i) {
                    callOnUndock()
                    print(" ${count.getAndIncrement()} ")
                    callOnDock()
                }
            }
        }

        override val root = vbox {}
    }

    @Test
    fun testUnsubscribe() {
        Platform.runLater {
            view.callOnDock()
            repeat(iterations / 2) {
                view.fire(FXEvent())
                view.fire(FXEvent(EventBus.RunOn.BackgroundThread))
            }
            latch.countDown()
        }
        latch.await()
        assertTrue(success.get())
    }

    // --- No-leak guarantees for the subscription/scope registries (upstream #1009) ---

    private class ScopeProbeEvent : FXEvent(EventBus.RunOn.BackgroundThread)
    private class OtherProbeEvent : FXEvent(EventBus.RunOn.BackgroundThread)

    @Test
    fun `repeated subscribe and invalidate cycles do not accumulate bookkeeping`() {
        val bus = EventBus()
        val registration = FXEventRegistration(ScopeProbeEvent::class, null, null, { })

        repeat(50) {
            bus.subscribe(ScopeProbeEvent::class, FX.defaultScope, registration)
            bus.unsubscribe(registration)
        }

        assertEquals(0, bus.scopeRegistrySize, "scope entries must not accumulate across cycles")
        assertEquals(0, bus.subscriptionCount(ScopeProbeEvent::class), "invalidated registrations must be dropped")
    }

    @Test
    fun `component dock and undock cycles keep bus bookkeeping bounded`() {
        val view = object : View() {
            init {
                subscribe<ScopeProbeEvent> { }
            }

            override val root = vbox {}
        }

        view.callOnDock()
        val scopeSizeWhileDocked = FX.eventbus.scopeRegistrySize
        val registrationsWhileDocked = FX.eventbus.subscriptionCount(ScopeProbeEvent::class)
        assertEquals(1, registrationsWhileDocked)

        repeat(25) {
            view.callOnUndock()
            view.callOnDock()
        }

        assertEquals(scopeSizeWhileDocked, FX.eventbus.scopeRegistrySize, "scope entries must stay bounded across dock/undock cycles")
        assertEquals(registrationsWhileDocked, FX.eventbus.subscriptionCount(ScopeProbeEvent::class))

        view.callOnUndock()
        assertEquals(0, FX.eventbus.subscriptionCount(ScopeProbeEvent::class))
    }

    @Test
    fun `event context unsubscribe during dispatch removes the scope entry`() {
        val bus = EventBus()
        var invocations = 0

        bus.subscribe(ScopeProbeEvent::class, FX.defaultScope, FXEventRegistration(ScopeProbeEvent::class, null, null, {
            unsubscribe()
            invocations++
        }))

        bus.fire(ScopeProbeEvent())

        assertEquals(1, invocations)
        assertEquals(0, bus.scopeRegistrySize)
        assertEquals(0, bus.subscriptionCount(ScopeProbeEvent::class))
    }

    @Test
    fun `scope entry survives while another event type still uses the registration`() {
        val bus = EventBus()
        val registration = FXEventRegistration(ScopeProbeEvent::class, null, null, { })

        bus.subscribe(ScopeProbeEvent::class, FX.defaultScope, registration)
        bus.subscribe(OtherProbeEvent::class, FX.defaultScope, registration)
        assertEquals(1, bus.scopeRegistrySize)

        // Unsubscribing from the registration's own event type must keep the scope
        // entry: the second event type still holds a valid registration for it
        bus.unsubscribe(registration)
        assertEquals(1, bus.scopeRegistrySize)

        bus.unsubscribe(OtherProbeEvent::class, registration.action)
        assertEquals(0, bus.scopeRegistrySize)
    }

    @Test
    fun `cleanup preserves delivery to valid subscribers and excludes invalidated ones`() {
        val bus = EventBus()
        val selfRemovingReceived = AtomicInteger(0)
        val receiverReceived = AtomicInteger(0)

        bus.subscribe(ScopeProbeEvent::class, FX.defaultScope, FXEventRegistration(ScopeProbeEvent::class, null, null, {
            unsubscribe()
            selfRemovingReceived.getAndIncrement()
        }))
        bus.subscribe(ScopeProbeEvent::class, FX.defaultScope, FXEventRegistration(ScopeProbeEvent::class, null, null, {
            receiverReceived.getAndIncrement()
        }))

        bus.fire(ScopeProbeEvent())
        assertEquals(1, selfRemovingReceived.get())
        assertEquals(1, receiverReceived.get())
        assertEquals(1, bus.subscriptionCount(ScopeProbeEvent::class), "the still-valid receiver must be retained")

        bus.fire(ScopeProbeEvent())
        assertEquals(2, receiverReceived.get(), "valid subscriber must be delivered exactly once per dispatch")
        assertEquals(1, selfRemovingReceived.get(), "invalidated subscriber must not be invoked again")
        assertEquals(1, bus.scopeRegistrySize, "the still-valid receiver's scope entry must be retained")
    }
}

