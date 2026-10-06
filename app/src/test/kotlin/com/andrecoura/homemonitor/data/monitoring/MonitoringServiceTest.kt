package com.andrecoura.homemonitor.data.monitoring

import com.andrecoura.homemonitor.data.local.AppDatabase
import com.andrecoura.homemonitor.data.local.CallEntity
import com.andrecoura.homemonitor.data.local.DeviceEntity
import com.andrecoura.homemonitor.data.local.GateEntity
import com.andrecoura.homemonitor.domain.model.AlertType
import com.andrecoura.homemonitor.domain.model.CallState
import com.andrecoura.homemonitor.domain.model.ConnectionStatus
import com.andrecoura.homemonitor.domain.model.DeviceKind
import com.andrecoura.homemonitor.domain.model.GateState
import com.andrecoura.homemonitor.domain.port.DiscoveredGate
import com.andrecoura.homemonitor.domain.port.GateReport
import com.andrecoura.homemonitor.domain.port.IntercomEvent
import com.andrecoura.homemonitor.domain.rules.MonitoringPolicy
import com.andrecoura.homemonitor.support.FakeGateDriver
import com.andrecoura.homemonitor.support.FakeIntercomDriver
import com.andrecoura.homemonitor.support.FakeProbe
import com.andrecoura.homemonitor.support.MutableClock
import com.andrecoura.homemonitor.support.inMemoryDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class MonitoringServiceTest {
    private lateinit var db: AppDatabase
    private val clock = MutableClock()
    private val gateDriver = FakeGateDriver(listOf(DiscoveredGate("g", "Portão", GateState.CLOSED)))
    private val intercom = FakeIntercomDriver()
    private val probe = FakeProbe()
    private lateinit var service: MonitoringService

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        service =
            MonitoringService(
                db.deviceDao(),
                db.gateDao(),
                db.callDao(),
                db.alertDao(),
                gateDriver,
                intercom,
                probe,
                clock,
                MonitoringPolicy(gateOpenLimit = Duration.ofMinutes(5)),
            )
    }

    @After
    fun tearDown() = db.close()

    private suspend fun alertTypes() =
        db
            .alertDao()
            .observeRecent(10)
            .first()
            .map { it.type }

    @Test
    fun `discovered gates are stored once and keep their later state`() =
        runTest {
            service.syncGates()
            service.onGateReport(GateReport("g", GateState.OPEN))
            service.syncGates()
            assertEquals(GateState.OPEN, db.gateDao().get("g")!!.state)
            assertEquals(1, db.gateDao().getAll().size)
        }

    @Test
    fun `gate reports update state and timestamp, unknown and unchanged reports are ignored`() =
        runTest {
            service.syncGates()
            clock.now = clock.now.plusSeconds(10)
            service.onGateReport(GateReport("g", GateState.OPENING))
            assertEquals(clock.now.toEpochMilli(), db.gateDao().get("g")!!.stateSinceMillis)
            clock.now = clock.now.plusSeconds(10)
            service.onGateReport(GateReport("g", GateState.OPENING))
            service.onGateReport(GateReport("ghost", GateState.OPEN))
            assertEquals(clock.now.minusSeconds(10).toEpochMilli(), db.gateDao().get("g")!!.stateSinceMillis)
            assertTrue(alertTypes().isEmpty())
        }

    @Test
    fun `gate going offline raises a device alert`() =
        runTest {
            service.syncGates()
            service.onGateReport(GateReport("g", GateState.OFFLINE))
            assertEquals(listOf(AlertType.DEVICE_OFFLINE), alertTypes())
        }

    @Test
    fun `gate left open alerts once per opening`() =
        runTest {
            service.syncGates()
            service.onGateReport(GateReport("g", GateState.OPEN))
            clock.now = clock.now.plusSeconds(299)
            service.checkOpenGates()
            assertTrue(alertTypes().isEmpty())
            clock.now = clock.now.plusSeconds(2)
            service.checkOpenGates()
            service.checkOpenGates()
            assertEquals(listOf(AlertType.GATE_LEFT_OPEN), alertTypes())

            service.onGateReport(GateReport("g", GateState.CLOSED))
            service.onGateReport(GateReport("g", GateState.OPEN))
            clock.now = clock.now.plusSeconds(301)
            service.checkOpenGates()
            assertEquals(2, alertTypes().size)
        }

    @Test
    fun `incoming call is stored ringing and becomes missed with an alert`() =
        runTest {
            service.onCallEvent(IntercomEvent.Incoming("c", "Interfone", "g", clock.instant()))
            assertEquals(CallState.RINGING, db.callDao().get("c")!!.state)
            service.onCallEvent(IntercomEvent.Missed("c"))
            assertEquals(CallState.MISSED, db.callDao().get("c")!!.state)
            assertEquals(listOf(AlertType.MISSED_CALL), alertTypes())
        }

    @Test
    fun `missed event is ignored for answered or unknown calls`() =
        runTest {
            db.callDao().upsert(CallEntity("c", "Interfone", "g", CallState.ANSWERED, 0))
            service.onCallEvent(IntercomEvent.Missed("c"))
            service.onCallEvent(IntercomEvent.Missed("unknown"))
            assertEquals(CallState.ANSWERED, db.callDao().get("c")!!.state)
            assertTrue(alertTypes().isEmpty())
        }

    @Test
    fun `probe marks new devices online without alert and raises one when they drop`() =
        runTest {
            val id =
                db.deviceDao().insert(
                    DeviceEntity(
                        name = "Cam",
                        room = "Sala",
                        kind = DeviceKind.CAMERA,
                        address = "rtsp://x/y",
                        status = ConnectionStatus.OFFLINE,
                    ),
                )
            service.probeAll()
            assertEquals(ConnectionStatus.ONLINE, db.deviceDao().get(id)!!.status)
            assertTrue(alertTypes().isEmpty())

            probe.reachable = false
            service.probeAll()
            service.probeAll()
            assertEquals(ConnectionStatus.OFFLINE, db.deviceDao().get(id)!!.status)
            assertEquals(listOf(AlertType.DEVICE_OFFLINE), alertTypes())
        }

    @Test
    fun `start wires the ports to Room`() =
        runBlocking {
            db.deviceDao().insert(
                DeviceEntity(
                    name = "Cam",
                    room = "Sala",
                    kind = DeviceKind.CAMERA,
                    address = "rtsp://x/y",
                    status = ConnectionStatus.OFFLINE,
                ),
            )
            val scope = CoroutineScope(Dispatchers.Default)
            service.start(scope)
            // Room runs its queries on its own threads, so wait in real time instead of virtual time.
            eventually {
                db.gateDao().getAll().size == 1 && db
                    .deviceDao()
                    .getAll()
                    .single()
                    .status == ConnectionStatus.ONLINE
            }
            gateDriver.reports.emit(GateReport("g", GateState.OPEN))
            intercom.events.emit(IntercomEvent.Incoming("c", "Interfone", "g", clock.instant()))
            eventually { db.gateDao().get("g")?.state == GateState.OPEN && db.callDao().get("c")?.state == CallState.RINGING }
            scope.cancel()
        }

    private suspend fun eventually(condition: suspend () -> Boolean) {
        withTimeout(10_000) { while (!condition()) delay(20) }
    }
}
