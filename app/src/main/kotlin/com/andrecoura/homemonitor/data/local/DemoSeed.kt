package com.andrecoura.homemonitor.data.local

import android.content.ContentValues
import androidx.sqlite.db.SupportSQLiteDatabase
import com.andrecoura.homemonitor.domain.model.AlertType
import com.andrecoura.homemonitor.domain.model.CallState
import com.andrecoura.homemonitor.domain.model.ConnectionStatus
import com.andrecoura.homemonitor.domain.model.DeviceKind

/** Fictional data inserted once, when the database is created, so a fresh install is not an empty screen. */
object DemoSeed {
    private const val MINUTE = 60_000L

    fun populate(
        db: SupportSQLiteDatabase,
        nowMillis: Long,
    ) {
        device(db, "Câmera da entrada", "Entrada", DeviceKind.CAMERA, "rtsp://192.168.0.21/stream1", ConnectionStatus.ONLINE)
        device(db, "Câmera do quintal", "Quintal", DeviceKind.CAMERA, "rtsp://192.168.0.22/stream1", ConnectionStatus.ONLINE)
        device(db, "Câmera da garagem", "Garagem", DeviceKind.CAMERA, "rtsp://offline.local/garage", ConnectionStatus.OFFLINE)
        device(db, "Echo da sala", "Sala", DeviceKind.ALEXA, "G090LF1234567890", ConnectionStatus.ONLINE)
        device(db, "Echo Dot do quarto", "Quarto", DeviceKind.ALEXA, "G090LF0987654321", ConnectionStatus.ONLINE)

        call(db, "demo-call-1", CallState.ANSWERED, nowMillis - 3 * 60 * MINUTE)
        call(db, "demo-call-2", CallState.MISSED, nowMillis - 26 * 60 * MINUTE)
        call(db, "demo-call-3", CallState.ANSWERED, nowMillis - 50 * 60 * MINUTE)

        alert(db, AlertType.DEVICE_OFFLINE, "Câmera da garagem", nowMillis - 25 * MINUTE)
        alert(db, AlertType.MISSED_CALL, "Interfone da portaria", nowMillis - 26 * 60 * MINUTE)
    }

    private fun device(
        db: SupportSQLiteDatabase,
        name: String,
        room: String,
        kind: DeviceKind,
        address: String,
        status: ConnectionStatus,
    ) {
        val values =
            ContentValues().apply {
                put("name", name)
                put("room", room)
                put("kind", kind.name)
                put("address", address)
                put("status", status.name)
            }
        db.insert("devices", 0, values)
    }

    private fun call(
        db: SupportSQLiteDatabase,
        id: String,
        state: CallState,
        startedAt: Long,
    ) {
        val values =
            ContentValues().apply {
                put("id", id)
                put("intercomName", "Interfone da portaria")
                put("gateId", "gate-front")
                put("state", state.name)
                put("startedAtMillis", startedAt)
            }
        db.insert("intercom_calls", 0, values)
    }

    private fun alert(
        db: SupportSQLiteDatabase,
        type: AlertType,
        subject: String,
        at: Long,
    ) {
        val values =
            ContentValues().apply {
                put("type", type.name)
                put("subject", subject)
                put("createdAtMillis", at)
            }
        db.insert("alerts", 0, values)
    }
}
