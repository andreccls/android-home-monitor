package com.andrecoura.homemonitor.data.local

import com.andrecoura.homemonitor.domain.model.Alert
import com.andrecoura.homemonitor.domain.model.ConnectionStatus
import com.andrecoura.homemonitor.domain.model.Device
import com.andrecoura.homemonitor.domain.model.DeviceDraft
import com.andrecoura.homemonitor.domain.model.Gate
import com.andrecoura.homemonitor.domain.model.IntercomCall
import java.time.Instant

fun DeviceEntity.toDomain() = Device(id, name, room, kind, address, status)

fun DeviceDraft.toEntity(
    id: Long = 0,
    status: ConnectionStatus = ConnectionStatus.OFFLINE,
) = DeviceEntity(id, name.trim(), room.trim(), kind, address.trim(), status)

fun GateEntity.toDomain() = Gate(id, name, state, Instant.ofEpochMilli(stateSinceMillis))

fun CallEntity.toDomain() = IntercomCall(id, intercomName, gateId, state, Instant.ofEpochMilli(startedAtMillis))

fun IntercomCall.toEntity() = CallEntity(id, intercomName, gateId, state, startedAt.toEpochMilli())

fun AlertEntity.toDomain() = Alert(id, type, subject, Instant.ofEpochMilli(createdAtMillis))

fun Alert.toEntity() = AlertEntity(id, type, subject, createdAt.toEpochMilli())
