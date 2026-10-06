package com.andrecoura.homemonitor.support

import com.andrecoura.homemonitor.domain.model.Alert
import com.andrecoura.homemonitor.domain.model.CallState
import com.andrecoura.homemonitor.domain.model.CameraFrame
import com.andrecoura.homemonitor.domain.model.ConnectionStatus
import com.andrecoura.homemonitor.domain.model.Device
import com.andrecoura.homemonitor.domain.model.DeviceDraft
import com.andrecoura.homemonitor.domain.model.DeviceKind
import com.andrecoura.homemonitor.domain.model.Gate
import com.andrecoura.homemonitor.domain.model.GateCommand
import com.andrecoura.homemonitor.domain.model.GateState
import com.andrecoura.homemonitor.domain.model.IntercomCall
import com.andrecoura.homemonitor.domain.port.CameraStream
import com.andrecoura.homemonitor.domain.port.DeviceProbe
import com.andrecoura.homemonitor.domain.port.DeviceUnreachableException
import com.andrecoura.homemonitor.domain.port.DiscoveredGate
import com.andrecoura.homemonitor.domain.port.GateDriver
import com.andrecoura.homemonitor.domain.port.GateReport
import com.andrecoura.homemonitor.domain.port.IntercomDriver
import com.andrecoura.homemonitor.domain.port.IntercomEvent
import com.andrecoura.homemonitor.domain.repository.AlertRepository
import com.andrecoura.homemonitor.domain.repository.DeviceRepository
import com.andrecoura.homemonitor.domain.repository.GateRepository
import com.andrecoura.homemonitor.domain.repository.IntercomRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** A clock the test moves by hand. */
class MutableClock(
    var now: Instant = T0,
) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this

    override fun instant(): Instant = now

    companion object {
        val T0: Instant = Instant.parse("2026-10-06T12:00:00Z")
    }
}

fun device(
    id: Long = 1,
    name: String = "Câmera da entrada",
    kind: DeviceKind = DeviceKind.CAMERA,
    status: ConnectionStatus = ConnectionStatus.ONLINE,
    address: String = "rtsp://192.168.0.21/stream1",
    room: String = "Entrada",
) = Device(id, name, room, kind, address, status)

fun gate(
    id: String = "gate-1",
    name: String = "Portão",
    state: GateState = GateState.CLOSED,
    since: Instant = MutableClock.T0,
) = Gate(id, name, state, since)

fun call(
    id: String = "call-1",
    state: CallState = CallState.RINGING,
) = IntercomCall(id, "Interfone", "gate-1", state, MutableClock.T0)

class FakeDeviceRepository(
    initial: List<Device> = emptyList(),
) : DeviceRepository {
    val devices = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    override fun observeAll(): Flow<List<Device>> = devices

    override fun observe(id: Long): Flow<Device?> = devices.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun get(id: Long): Device? = devices.value.firstOrNull { it.id == id }

    override suspend fun add(draft: DeviceDraft): Long {
        val id = nextId++
        devices.update { it + Device(id, draft.name, draft.room, draft.kind, draft.address, ConnectionStatus.OFFLINE) }
        return id
    }

    override suspend fun update(
        id: Long,
        draft: DeviceDraft,
    ) = devices.update { list ->
        list.map { if (it.id == id) it.copy(name = draft.name, room = draft.room, kind = draft.kind, address = draft.address) else it }
    }

    override suspend fun delete(id: Long) = devices.update { list -> list.filterNot { it.id == id } }
}

class FakeGateRepository(
    initial: List<Gate> = emptyList(),
) : GateRepository {
    val gates = MutableStateFlow(initial)
    val sent = mutableListOf<Pair<String, GateCommand>>()
    var result: Result<Unit> = Result.success(Unit)

    override fun observeAll(): Flow<List<Gate>> = gates

    override suspend fun send(
        gateId: String,
        command: GateCommand,
    ): Result<Unit> {
        sent += gateId to command
        return result
    }
}

class FakeIntercomRepository(
    initial: List<IntercomCall> = emptyList(),
) : IntercomRepository {
    val calls = MutableStateFlow(initial)
    val answered = mutableListOf<String>()
    var result: Result<Unit> = Result.success(Unit)

    override fun observeCalls(): Flow<List<IntercomCall>> = calls

    override suspend fun answer(callId: String): Result<Unit> {
        answered += callId
        if (result.isSuccess) {
            calls.update { list ->
                list.map { if (it.id == callId) it.copy(state = CallState.ANSWERED) else it }
            }
        }
        return result
    }
}

class FakeAlertRepository(
    initial: List<Alert> = emptyList(),
) : AlertRepository {
    val alerts = MutableStateFlow(initial)

    override fun observeRecent(limit: Int): Flow<List<Alert>> = alerts.map { it.take(limit) }
}

class FakeCameraStream(
    private val frames: Flow<CameraFrame>,
) : CameraStream {
    override fun frames(device: Device): Flow<CameraFrame> = frames
}

class FakeGateDriver(
    private val gates: List<DiscoveredGate> = emptyList(),
) : GateDriver {
    val reports = MutableSharedFlow<GateReport>(extraBufferCapacity = 16)
    val sent = mutableListOf<Pair<String, GateCommand>>()
    var failure: Exception? = null

    override suspend fun discover(): List<DiscoveredGate> = gates

    override fun observe(): Flow<GateReport> = reports

    override suspend fun send(
        gateId: String,
        command: GateCommand,
    ) {
        failure?.let { throw it }
        sent += gateId to command
    }
}

class FakeIntercomDriver : IntercomDriver {
    val events = MutableSharedFlow<IntercomEvent>(extraBufferCapacity = 16)
    var failure: Exception? = null
    val answered = mutableListOf<String>()

    override fun events(): Flow<IntercomEvent> = events

    override suspend fun answer(callId: String) {
        failure?.let { throw it }
        answered += callId
    }
}

class FakeProbe(
    var reachable: Boolean = true,
) : DeviceProbe {
    override suspend fun isReachable(device: Device): Boolean = reachable
}

fun unreachable() = DeviceUnreachableException("no answer")
