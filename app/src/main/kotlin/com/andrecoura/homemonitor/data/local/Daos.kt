package com.andrecoura.homemonitor.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.andrecoura.homemonitor.domain.model.CallState
import com.andrecoura.homemonitor.domain.model.ConnectionStatus
import com.andrecoura.homemonitor.domain.model.GateState
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY room, name")
    fun observeAll(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE id = :id")
    fun observe(id: Long): Flow<DeviceEntity?>

    @Query("SELECT * FROM devices WHERE id = :id")
    suspend fun get(id: Long): DeviceEntity?

    @Query("SELECT * FROM devices")
    suspend fun getAll(): List<DeviceEntity>

    @Insert
    suspend fun insert(device: DeviceEntity): Long

    @Update
    suspend fun update(device: DeviceEntity)

    @Query("UPDATE devices SET status = :status WHERE id = :id")
    suspend fun updateStatus(
        id: Long,
        status: ConnectionStatus,
    )

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM devices")
    suspend fun count(): Int
}

@Dao
interface GateDao {
    @Query("SELECT * FROM gates ORDER BY name")
    fun observeAll(): Flow<List<GateEntity>>

    @Query("SELECT * FROM gates")
    suspend fun getAll(): List<GateEntity>

    @Query("SELECT * FROM gates WHERE id = :id")
    suspend fun get(id: String): GateEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(gate: GateEntity)

    @Query("UPDATE gates SET state = :state, stateSinceMillis = :sinceMillis WHERE id = :id")
    suspend fun updateState(
        id: String,
        state: GateState,
        sinceMillis: Long,
    )
}

@Dao
interface CallDao {
    @Query("SELECT * FROM intercom_calls ORDER BY startedAtMillis DESC")
    fun observeAll(): Flow<List<CallEntity>>

    @Query("SELECT * FROM intercom_calls WHERE id = :id")
    suspend fun get(id: String): CallEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(call: CallEntity)

    @Query("UPDATE intercom_calls SET state = :state WHERE id = :id")
    suspend fun updateState(
        id: String,
        state: CallState,
    )

    @Query("SELECT COUNT(*) FROM intercom_calls")
    suspend fun count(): Int
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY createdAtMillis DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<AlertEntity>>

    @Insert
    suspend fun insert(alert: AlertEntity)
}
