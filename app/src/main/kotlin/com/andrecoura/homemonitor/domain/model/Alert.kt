package com.andrecoura.homemonitor.domain.model

import java.time.Instant

enum class AlertType { GATE_LEFT_OPEN, DEVICE_OFFLINE, MISSED_CALL }

data class Alert(
    val id: Long,
    val type: AlertType,
    /** Name of the gate, device or intercom the alert is about. */
    val subject: String,
    val createdAt: Instant,
)
