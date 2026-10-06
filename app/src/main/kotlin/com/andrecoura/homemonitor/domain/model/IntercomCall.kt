package com.andrecoura.homemonitor.domain.model

import java.time.Instant

enum class CallState { RINGING, ANSWERED, MISSED }

data class IntercomCall(
    val id: String,
    val intercomName: String,
    /** The gate this intercom opens, so "open gate" can be offered straight from the call. */
    val gateId: String,
    val state: CallState,
    val startedAt: Instant,
)
