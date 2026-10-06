package com.andrecoura.homemonitor.domain.port

import com.andrecoura.homemonitor.domain.model.CameraFrame
import com.andrecoura.homemonitor.domain.model.Device
import com.andrecoura.homemonitor.domain.model.GateCommand
import com.andrecoura.homemonitor.domain.model.GateState
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Ports: everything that would talk to real hardware lives behind these interfaces.
 * Today the only implementations are simulators (data/simulation).
 */
class DeviceUnreachableException(
    message: String,
) : Exception(message)

data class DiscoveredGate(
    val id: String,
    val name: String,
    val state: GateState,
)

data class GateReport(
    val gateId: String,
    val state: GateState,
)

interface GateDriver {
    suspend fun discover(): List<DiscoveredGate>

    /** State changes pushed by the gates, including the ones nobody asked for. */
    fun observe(): Flow<GateReport>

    /** Fails with [DeviceUnreachableException] when the gate does not answer. */
    suspend fun send(
        gateId: String,
        command: GateCommand,
    )
}

sealed interface IntercomEvent {
    data class Incoming(
        val callId: String,
        val intercomName: String,
        val gateId: String,
        val at: Instant,
    ) : IntercomEvent

    data class Missed(
        val callId: String,
    ) : IntercomEvent
}

class CallNoLongerRingingException(
    callId: String,
) : Exception("Call $callId is not ringing anymore")

interface IntercomDriver {
    fun events(): Flow<IntercomEvent>

    /** Fails with [CallNoLongerRingingException] if the call was already answered or missed. */
    suspend fun answer(callId: String)
}

interface DeviceProbe {
    /** True when the device answers. May take a while; never throws for an unreachable device. */
    suspend fun isReachable(device: Device): Boolean
}

class StreamUnavailableException(
    message: String,
) : Exception(message)

interface CameraStream {
    /** Completes exceptionally with [StreamUnavailableException] when there is no signal. */
    fun frames(device: Device): Flow<CameraFrame>
}
