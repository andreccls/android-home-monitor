package com.andrecoura.homemonitor.domain

import com.andrecoura.homemonitor.domain.model.AlertType
import com.andrecoura.homemonitor.domain.model.CallState
import com.andrecoura.homemonitor.domain.model.ConnectionStatus
import com.andrecoura.homemonitor.domain.model.DeviceDraft
import com.andrecoura.homemonitor.domain.model.DeviceError
import com.andrecoura.homemonitor.domain.model.DeviceKind
import com.andrecoura.homemonitor.domain.model.GateCommand
import com.andrecoura.homemonitor.domain.model.GateState
import com.andrecoura.homemonitor.domain.rules.AlertRules
import com.andrecoura.homemonitor.support.MutableClock
import com.andrecoura.homemonitor.support.call
import com.andrecoura.homemonitor.support.device
import com.andrecoura.homemonitor.support.gate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class GateStateMachineTest {
    @Test
    fun `closed gate can only be opened`() {
        val closed = gate(state = GateState.CLOSED)
        assertTrue(closed.allows(GateCommand.OPEN))
        assertFalse(closed.allows(GateCommand.CLOSE))
        assertEquals(GateCommand.OPEN, closed.suggestedCommand)
    }

    @Test
    fun `open gate can only be closed`() {
        val open = gate(state = GateState.OPEN)
        assertTrue(open.allows(GateCommand.CLOSE))
        assertFalse(open.allows(GateCommand.OPEN))
    }

    @Test
    fun `a moving gate can be reversed`() {
        assertEquals(GateCommand.CLOSE, gate(state = GateState.OPENING).suggestedCommand)
        assertEquals(GateCommand.OPEN, gate(state = GateState.CLOSING).suggestedCommand)
    }

    @Test
    fun `offline gate accepts nothing`() {
        val offline = gate(state = GateState.OFFLINE)
        assertNull(offline.suggestedCommand)
        GateCommand.entries.forEach { assertFalse(offline.allows(it)) }
    }
}

class DeviceDraftTest {
    private val validCamera = DeviceDraft("Cam", "Sala", DeviceKind.CAMERA, "rtsp://192.168.0.5/stream")

    @Test
    fun `valid camera and alexa drafts have no errors`() {
        assertTrue(validCamera.validate().isEmpty())
        assertTrue(DeviceDraft("Echo", "Quarto", DeviceKind.ALEXA, "G090LF1234567890").validate().isEmpty())
    }

    @Test
    fun `blank name and room are required`() {
        val errors = DeviceDraft("  ", "", DeviceKind.CAMERA, "rtsp://host/x").validate()
        assertEquals(setOf(DeviceError.NAME_REQUIRED, DeviceError.ROOM_REQUIRED), errors)
    }

    @Test
    fun `name has a maximum length`() {
        assertEquals(setOf(DeviceError.NAME_TOO_LONG), validCamera.copy(name = "x".repeat(41)).validate())
        assertTrue(validCamera.copy(name = "x".repeat(40)).validate().isEmpty())
    }

    @Test
    fun `camera address must be a stream url`() {
        listOf("", "192.168.0.5", "ftp://host/x", "rtsp://", "rtsp://host with space").forEach {
            assertEquals(it, setOf(DeviceError.ADDRESS_INVALID), validCamera.copy(address = it).validate())
        }
        assertTrue(validCamera.copy(address = "HTTPS://cam.local:8080/live").validate().isEmpty())
    }

    @Test
    fun `alexa identifier must be a serial`() {
        val alexa = DeviceDraft("Echo", "Sala", DeviceKind.ALEXA, "")
        listOf("", "abc", "has space 123456", "rtsp://host").forEach {
            assertEquals(it, setOf(DeviceError.ADDRESS_INVALID), alexa.copy(address = it).validate())
        }
    }
}

class AlertRulesTest {
    private val now = MutableClock.T0.plusSeconds(600)

    @Test
    fun `open gate raises an alert only after the limit`() {
        val open = gate(state = GateState.OPEN, since = MutableClock.T0)
        assertNull(AlertRules.gateLeftOpen(open, MutableClock.T0.plusSeconds(299)))
        val alert = AlertRules.gateLeftOpen(open, MutableClock.T0.plusSeconds(300))
        assertEquals(AlertType.GATE_LEFT_OPEN, alert?.type)
        assertEquals("Portão", alert?.subject)
    }

    @Test
    fun `limit is configurable and closed gates never alert`() {
        val open = gate(state = GateState.OPEN, since = MutableClock.T0)
        assertNotNull(AlertRules.gateLeftOpen(open, now, Duration.ofMinutes(1)))
        assertNull(AlertRules.gateLeftOpen(gate(state = GateState.CLOSED, since = MutableClock.T0), now))
    }

    @Test
    fun `device alert only on the online to offline transition`() {
        val offline = device(status = ConnectionStatus.OFFLINE)
        assertEquals(AlertType.DEVICE_OFFLINE, AlertRules.deviceWentOffline(offline, ConnectionStatus.ONLINE, now)?.type)
        assertNull(AlertRules.deviceWentOffline(offline, ConnectionStatus.OFFLINE, now))
        assertNull(AlertRules.deviceWentOffline(device(status = ConnectionStatus.ONLINE), ConnectionStatus.OFFLINE, now))
    }

    @Test
    fun `only missed calls raise an alert`() {
        assertEquals(AlertType.MISSED_CALL, AlertRules.missedCall(call(state = CallState.MISSED), now)?.type)
        assertNull(AlertRules.missedCall(call(state = CallState.ANSWERED), now))
        assertNull(AlertRules.missedCall(call(state = CallState.RINGING), now))
    }
}
