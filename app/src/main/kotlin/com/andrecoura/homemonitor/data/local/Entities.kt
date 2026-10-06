package com.andrecoura.homemonitor.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.andrecoura.homemonitor.domain.model.AlertType
import com.andrecoura.homemonitor.domain.model.CallState
import com.andrecoura.homemonitor.domain.model.ConnectionStatus
import com.andrecoura.homemonitor.domain.model.DeviceKind
import com.andrecoura.homemonitor.domain.model.GateState

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val room: String,
    val kind: DeviceKind,
    val address: String,
    val status: ConnectionStatus,
)

@Entity(tableName = "gates")
data class GateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val state: GateState,
    val stateSinceMillis: Long,
)

@Entity(tableName = "intercom_calls")
data class CallEntity(
    @PrimaryKey val id: String,
    val intercomName: String,
    val gateId: String,
    val state: CallState,
    val startedAtMillis: Long,
)

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: AlertType,
    val subject: String,
    val createdAtMillis: Long,
)
