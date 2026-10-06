package com.andrecoura.homemonitor.data.simulation

import app.cash.turbine.test
import com.andrecoura.homemonitor.domain.model.ConnectionStatus
import com.andrecoura.homemonitor.domain.model.GateCommand
import com.andrecoura.homemonitor.domain.model.GateState
import com.andrecoura.homemonitor.domain.port.CallNoLongerRingingException
import com.andrecoura.homemonitor.domain.port.DeviceUnreachableException
import com.andrecoura.homemonitor.domain.port.GateReport
import com.andrecoura.homemonitor.domain.port.IntercomEvent
import com.andrecoura.homemonitor.domain.port.StreamUnavailableException
import com.andrecoura.homemonitor.support.MutableClock
import com.andrecoura.homemonitor.support.assertFailsWith
import com.andrecoura.homemonitor.support.device
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SimulatedGateDriverTest {
    private val config = SimulationConfig.Instant
    private val garage = SimulatedHome.GARAGE_GATE_ID

    @Test
    fun `discovers two closed gates`() =
        runTest {
            val gates = SimulatedGateDriver(this, config).discover()
            assertEquals(listOf(GateState.CLOSED, GateState.CLOSED), gates.map { it.state })
        }

    @Test
    fun `open command travels through opening to open`() =
        runTest {
            val driver = SimulatedGateDriver(this, config)
            driver.observe().test {
                driver.send(garage, GateCommand.OPEN)
                assertEquals(GateReport(garage, GateState.OPENING), awaitItem())
                assertEquals(GateReport(garage, GateState.OPEN), awaitItem())
                driver.send(garage, GateCommand.CLOSE)
                assertEquals(GateState.CLOSING, awaitItem().state)
                assertEquals(GateState.CLOSED, awaitItem().state)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `new command interrupts a travel in progress`() =
        runTest {
            val driver = SimulatedGateDriver(this, config)
            driver.observe().test {
                driver.send(garage, GateCommand.OPEN)
                assertEquals(GateState.OPENING, awaitItem().state)
                driver.send(garage, GateCommand.CLOSE)
                assertEquals(GateState.CLOSING, awaitItem().state)
                assertEquals(GateState.CLOSED, awaitItem().state)
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `failures and unknown gates surface as unreachable`() =
        runTest {
            val flaky = SimulatedGateDriver(this, config.copy(commandFailureRate = 1.0))
            assertFailsWith<DeviceUnreachableException> { flaky.send(garage, GateCommand.OPEN) }
            assertFailsWith<DeviceUnreachableException> { SimulatedGateDriver(this, config).send("nope", GateCommand.OPEN) }
        }

    @Test
    fun `visitors open the gate and outages take it offline on their own`() =
        runTest {
            val active =
                config.copy(
                    gateActivityEveryMillis = 1_000L..1_000L,
                    gateOpenForMillis = 500,
                    gateOutageMillis = 500,
                    gateTravelMillis = 100,
                )
            val driver = SimulatedGateDriver(this, active, Random(7))
            val seen =
                driver
                    .observe()
                    .take(40)
                    .toList()
                    .map { it.state }
                    .toSet()
            assertTrue(GateState.OPEN in seen)
            assertTrue(GateState.OFFLINE in seen)
            assertTrue(GateState.CLOSING in seen)
        }
}

class SimulatedIntercomDriverTest {
    private val clock = MutableClock()
    private val ringing = SimulationConfig.Instant.copy(callEveryMillis = 1_000L..1_000L, ringTimeoutMillis = 5_000)

    @Test
    fun `no calls when disabled`() =
        runTest {
            SimulatedIntercomDriver(SimulationConfig.Instant, clock).events().test {
                awaitComplete()
            }
        }

    @Test
    fun `unanswered call is reported missed after the timeout`() =
        runTest {
            SimulatedIntercomDriver(ringing, clock).events().test {
                val incoming = awaitItem() as IntercomEvent.Incoming
                assertEquals(SimulatedHome.FRONT_GATE_ID, incoming.gateId)
                assertEquals(IntercomEvent.Missed(incoming.callId), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `answered call is not reported missed and cannot be answered twice`() =
        runTest {
            val driver = SimulatedIntercomDriver(ringing, clock)
            driver.events().test {
                val incoming = awaitItem() as IntercomEvent.Incoming
                driver.answer(incoming.callId)
                assertEquals(IntercomEvent.Incoming::class, awaitItem()::class) // next call; no Missed in between
                cancelAndIgnoreRemainingEvents()
                assertFailsWith<CallNoLongerRingingException> { driver.answer(incoming.callId) }
            }
        }
}

class SimulatedProbeAndCameraTest {
    private val clock = MutableClock()

    @Test
    fun `healthy device answers, forced offline never does`() =
        runTest {
            val probe = SimulatedDeviceProbe(SimulationConfig.Instant, clock)
            assertTrue(probe.isReachable(device()))
            assertFalse(probe.isReachable(device(address = "rtsp://OFFLINE.local/x")))
        }

    @Test
    fun `outage lasts until its end and then the device comes back`() =
        runTest {
            val config = SimulationConfig.Instant.copy(deviceOutageChance = 1.0, deviceOutageMillis = 60_000)
            val probe = SimulatedDeviceProbe(config, clock)
            assertFalse(probe.isReachable(device()))
            assertFalse(probe.isReachable(device())) // still inside the outage
            clock.now = clock.now.plusSeconds(61)
            val recovered = SimulatedDeviceProbe(config.copy(deviceOutageChance = 0.0), clock)
            assertTrue(recovered.isReachable(device()))
        }

    @Test
    fun `camera streams numbered frames`() =
        runTest {
            val frames = SimulatedCameraStream(SimulationConfig.Instant, clock).frames(device()).take(3).toList()
            assertEquals(listOf(0L, 1L, 2L), frames.map { it.sequence })
        }

    @Test
    fun `camera without signal fails the stream`() =
        runTest {
            val stream = SimulatedCameraStream(SimulationConfig.Instant, clock)
            assertFailsWith<StreamUnavailableException> { stream.frames(device(status = ConnectionStatus.OFFLINE)).first() }
            assertFailsWith<StreamUnavailableException> { stream.frames(device(address = "rtsp://offline/x")).first() }
        }
}
