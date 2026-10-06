package com.andrecoura.homemonitor.domain.model

import java.time.Instant

/** One frame of a (simulated) stream. Real video would carry pixels; here only the counter matters. */
data class CameraFrame(
    val sequence: Long,
    val capturedAt: Instant,
)
