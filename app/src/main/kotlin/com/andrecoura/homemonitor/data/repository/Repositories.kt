package com.andrecoura.homemonitor.data.repository

import com.andrecoura.homemonitor.data.local.AlertDao
import com.andrecoura.homemonitor.data.local.CallDao
import com.andrecoura.homemonitor.data.local.DeviceDao
import com.andrecoura.homemonitor.data.local.GateDao
import com.andrecoura.homemonitor.data.local.toDomain
import com.andrecoura.homemonitor.data.local.toEntity
import com.andrecoura.homemonitor.domain.model.Alert
import com.andrecoura.homemonitor.domain.model.CallState
import com.andrecoura.homemonitor.domain.model.Device
import com.andrecoura.homemonitor.domain.model.DeviceDraft
import com.andrecoura.homemonitor.domain.model.Gate
import com.andrecoura.homemonitor.domain.model.GateCommand
import com.andrecoura.homemonitor.domain.model.IntercomCall
import com.andrecoura.homemonitor.domain.port.GateDriver
import com.andrecoura.homemonitor.domain.port.IntercomDriver
import com.andrecoura.homemonitor.domain.repository.AlertRepository
import com.andrecoura.homemonitor.domain.repository.DeviceRepository
import com.andrecoura.homemonitor.domain.repository.GateCommandRejectedException
import com.andrecoura.homemonitor.domain.repository.GateRepository
import com.andrecoura.homemonitor.domain.repository.IntercomRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Room is the source of truth: every read is a Flow over the database. */
class RoomDeviceRepository
    @Inject
    constructor(
        private val dao: DeviceDao,
    ) : DeviceRepository {
        override fun observeAll(): Flow<List<Device>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

        override fun observe(id: Long): Flow<Device?> = dao.observe(id).map { it?.toDomain() }

        override suspend fun get(id: Long): Device? = dao.get(id)?.toDomain()

        override suspend fun add(draft: DeviceDraft): Long = dao.insert(draft.toEntity())

        override suspend fun update(
            id: Long,
            draft: DeviceDraft,
        ) {
            val current = dao.get(id) ?: return
            dao.update(draft.toEntity(id, current.status))
        }

        override suspend fun delete(id: Long) = dao.delete(id)
    }

class RoomGateRepository
    @Inject
    constructor(
        private val dao: GateDao,
        private val driver: GateDriver,
    ) : GateRepository {
        override fun observeAll(): Flow<List<Gate>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

        override suspend fun send(
            gateId: String,
            command: GateCommand,
        ): Result<Unit> {
            val gate =
                dao.get(gateId)?.toDomain()
                    ?: return Result.failure(GateCommandRejectedException("Unknown gate $gateId"))
            if (!gate.allows(command)) {
                return Result.failure(GateCommandRejectedException("$command not allowed while ${gate.state}"))
            }
            return runCatchingCancellable { driver.send(gateId, command) }
        }
    }

class RoomIntercomRepository
    @Inject
    constructor(
        private val dao: CallDao,
        private val driver: IntercomDriver,
    ) : IntercomRepository {
        override fun observeCalls(): Flow<List<IntercomCall>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

        override suspend fun answer(callId: String): Result<Unit> =
            runCatchingCancellable {
                driver.answer(callId)
                dao.updateState(callId, CallState.ANSWERED)
            }
    }

class RoomAlertRepository
    @Inject
    constructor(
        private val dao: AlertDao,
    ) : AlertRepository {
        override fun observeRecent(limit: Int): Flow<List<Alert>> = dao.observeRecent(limit).map { list -> list.map { it.toDomain() } }
    }

/** Like [runCatching], but never swallows coroutine cancellation. */
internal inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
