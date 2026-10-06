package com.andrecoura.homemonitor.ui.components

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.andrecoura.homemonitor.R
import com.andrecoura.homemonitor.domain.model.AlertType
import com.andrecoura.homemonitor.domain.model.CallState
import com.andrecoura.homemonitor.domain.model.ConnectionStatus
import com.andrecoura.homemonitor.domain.model.DeviceKind
import com.andrecoura.homemonitor.domain.model.GateState
import java.time.Instant

/** "há 5 minutos" style text, relative to now. Not reactive: it refreshes whenever the screen recomposes. */
fun relativeTime(instant: Instant): String =
    DateUtils.getRelativeTimeSpanString(instant.toEpochMilli(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()

@Composable
fun GateState.label(): String =
    stringResource(
        when (this) {
            GateState.CLOSED -> R.string.gate_state_closed
            GateState.OPENING -> R.string.gate_state_opening
            GateState.OPEN -> R.string.gate_state_open
            GateState.CLOSING -> R.string.gate_state_closing
            GateState.OFFLINE -> R.string.gate_state_offline
        },
    )

@Composable
fun CallState.label(): String =
    stringResource(
        when (this) {
            CallState.RINGING -> R.string.call_state_ringing
            CallState.ANSWERED -> R.string.call_state_answered
            CallState.MISSED -> R.string.call_state_missed
        },
    )

@Composable
fun ConnectionStatus.label(): String =
    stringResource(
        if (this == ConnectionStatus.ONLINE) R.string.device_status_online else R.string.device_status_offline,
    )

@Composable
fun DeviceKind.label(): String =
    stringResource(
        if (this == DeviceKind.CAMERA) R.string.device_kind_camera else R.string.device_kind_alexa,
    )

@Composable
fun AlertType.text(subject: String): String =
    stringResource(
        when (this) {
            AlertType.GATE_LEFT_OPEN -> R.string.alert_gate_left_open
            AlertType.DEVICE_OFFLINE -> R.string.alert_device_offline
            AlertType.MISSED_CALL -> R.string.alert_missed_call
        },
        subject,
    )
