package com.andrecoura.homemonitor.data.repository

import com.andrecoura.homemonitor.data.local.AlertEntity
import com.andrecoura.homemonitor.data.local.AppDatabase
import com.andrecoura.homemonitor.data.local.CallEntity
import com.andrecoura.homemonitor.data.local.GateEntity
import com.andrecoura.homemonitor.domain.model.AlertType
import com.andrecoura.homemonitor.domain.model.CallState
import com.andrecoura.homemonitor.domain.model.ConnectionStatus
import com.andrecoura.homemonitor.domain.model.DeviceDraft
import com.andrecoura.homemonitor.domain.model.DeviceKind
import com.andrecoura.homemonitor.domain.model.GateCommand
import com.andrecoura.homemonitor.domain.model.GateState
import com.andrecoura.homemonitor.domain.port.CallNoLongerRingingException
import com.andrecoura.homemonitor.domain.repository.GateCommandRejectedException
import com.andrecoura.homemonitor.support.FakeGateDriver
import com.andrecoura.homemonitor.support.FakeIntercomDriver
import com.andrecoura.homemonitor.support.inMemoryDatabase
import com.andrecoura.homemonitor.support.unreachable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RepositoriesTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = inMemoryDatabase()
    }

    @After
    fun tearDown() = db.close()

    private val draft = DeviceDraft("  Câmera  ", " Sala ", DeviceKind.CAMERA, " rtsp://host/x ")

    @Test
    fun `device is stored trimmed, offline until probed, and observable`() =
        runTest {
            val repo = RoomDeviceRepository(db.deviceDao())
            val id = repo.add(draft)
            val stored = repo.get(id)!!
            assertEquals("Câmera", stored.name)
            assertEquals("rtsp://host/x", stored.address)
            assertEquals(ConnectionStatus.OFFLINE, stored.status)
            assertEquals(listOf(stored), repo.observeAll().first())
            assertEquals(stored, repo.observe(id).first())
        }

    @Test
    fun `update keeps the probed status and delete removes the device`() =
        runTest {
            val repo = RoomDeviceRepository(db.deviceDao())
            val id = repo.add(draft)
            db.deviceDao().updateStatus(id, ConnectionStatus.ONLINE)
            repo.update(id, draft.copy(name = "Nova"))
            assertEquals("Nova", repo.get(id)!!.name)
            assertEquals(ConnectionStatus.ONLINE, repo.get(id)!!.status)
            repo.update(999, draft) // unknown id is ignored
            repo.delete(id)
            assertNull(repo.get(id))
            assertNull(repo.observe(id).first())
        }

    @Test
    fun `gate command goes to the driver when the state machine allows it`() =
        runTest {
            db.gateDao().insertIfAbsent(GateEntity("g", "Portão", GateState.CLOSED, 0))
            val driver = FakeGateDriver()
            val repo = RoomGateRepository(db.gateDao(), driver)
            assertTrue(repo.send("g", GateCommand.OPEN).isSuccess)
            assertEquals(listOf("g" to GateCommand.OPEN), driver.sent)
            assertEquals(
                "Portão",
                repo
                    .observeAll()
                    .first()
                    .single()
                    .name,
            )
        }

    @Test
    fun `gate command is rejected for unknown gate, wrong state and unreachable driver`() =
        runTest {
            db.gateDao().insertIfAbsent(GateEntity("g", "Portão", GateState.CLOSED, 0))
            val driver = FakeGateDriver()
            val repo = RoomGateRepository(db.gateDao(), driver)
            assertTrue(repo.send("nope", GateCommand.OPEN).exceptionOrNull() is GateCommandRejectedException)
            assertTrue(repo.send("g", GateCommand.CLOSE).exceptionOrNull() is GateCommandRejectedException)
            driver.failure = unreachable()
            assertEquals("no answer", repo.send("g", GateCommand.OPEN).exceptionOrNull()?.message)
            assertTrue(driver.sent.isEmpty())
        }

    @Test
    fun `answering a call updates Room, and a failed answer leaves it ringing`() =
        runTest {
            db.callDao().upsert(CallEntity("c", "Interfone", "g", CallState.RINGING, 5))
            val driver = FakeIntercomDriver()
            val repo = RoomIntercomRepository(db.callDao(), driver)
            assertTrue(repo.answer("c").isSuccess)
            assertEquals(
                CallState.ANSWERED,
                repo
                    .observeCalls()
                    .first()
                    .single()
                    .state,
            )

            db.callDao().updateState("c", CallState.RINGING)
            driver.failure = CallNoLongerRingingException("c")
            assertTrue(repo.answer("c").isFailure)
            assertEquals(CallState.RINGING, db.callDao().get("c")!!.state)
        }

    @Test
    fun `alerts come back newest first and limited`() =
        runTest {
            (1L..4L).forEach { db.alertDao().insert(AlertEntity(type = AlertType.MISSED_CALL, subject = "s$it", createdAtMillis = it)) }
            val recent = RoomAlertRepository(db.alertDao()).observeRecent(3).first()
            assertEquals(listOf("s4", "s3", "s2"), recent.map { it.subject })
        }
}
