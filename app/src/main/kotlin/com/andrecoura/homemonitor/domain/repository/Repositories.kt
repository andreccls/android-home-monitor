package com.andrecoura.homemonitor.domain.repository

import com.andrecoura.homemonitor.domain.model.Alert
import com.andrecoura.homemonitor.domain.model.Device
import com.andrecoura.homemonitor.domain.model.DeviceDraft
import com.andrecoura.homemonitor.domain.model.Gate
import com.andrecoura.homemonitor.domain.model.GateCommand
import com.andrecoura.homemonitor.domain.model.IntercomCall
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun observeAll(): Flow<List<Device>>

    fun observe(id: Long): Flow<Device?>

    suspend fun get(id: Long): Device?

    /** Persists a new device (starts offline until the first probe) and returns its id. */
    suspend fun add(draft: DeviceDraft): Long

    suspend fun update(
        id: Long,
        draft: DeviceDraft,
    )

    suspend fun delete(id: Long)
}

class GateCommandRejectedException(
    message: String,
) : Exception(message)

interface GateRepository {
    fun observeAll(): Flow<List<Gate>>

    /** Validates against the gate state machine, then sends. State changes come back through [observeAll]. */
    suspend fun send(
        gateId: String,
        command: GateCommand,
    ): Result<Unit>
}

interface IntercomRepository {
    /** Newest first. */
    fun observeCalls(): Flow<List<IntercomCall>>

    suspend fun answer(callId: String): Result<Unit>
}

interface AlertRepository {
    /** Newest first. */
    fun observeRecent(limit: Int): Flow<List<Alert>>
}
