package com.andrecoura.homemonitor.domain.rules

import com.andrecoura.homemonitor.domain.model.Alert
import com.andrecoura.homemonitor.domain.model.AlertType
import com.andrecoura.homemonitor.domain.model.CallState
import com.andrecoura.homemonitor.domain.model.ConnectionStatus
import com.andrecoura.homemonitor.domain.model.Device
import com.andrecoura.homemonitor.domain.model.Gate
import com.andrecoura.homemonitor.domain.model.GateState
import com.andrecoura.homemonitor.domain.model.IntercomCall
import java.time.Duration
import java.time.Instant

/** Pure functions that decide when something deserves an alert. Persistence is somebody else's job. */
object AlertRules {
    val DEFAULT_GATE_OPEN_LIMIT: Duration = Duration.ofMinutes(5)

    fun gateLeftOpen(
        gate: Gate,
        now: Instant,
        limit: Duration = DEFAULT_GATE_OPEN_LIMIT,
    ): Alert? =
        if (gate.state == GateState.OPEN && Duration.between(gate.stateSince, now) >= limit) {
            Alert(0, AlertType.GATE_LEFT_OPEN, gate.name, now)
        } else {
            null
        }

    fun deviceWentOffline(
        device: Device,
        previous: ConnectionStatus,
        now: Instant,
    ): Alert? =
        if (previous == ConnectionStatus.ONLINE && device.status == ConnectionStatus.OFFLINE) {
            Alert(0, AlertType.DEVICE_OFFLINE, device.name, now)
        } else {
            null
        }

    fun missedCall(
        call: IntercomCall,
        now: Instant,
    ): Alert? = if (call.state == CallState.MISSED) Alert(0, AlertType.MISSED_CALL, call.intercomName, now) else null
}
