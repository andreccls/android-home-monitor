package com.andrecoura.homemonitor.domain.model

/** Kinds of device the user registers by hand. Gates and intercoms are discovered, not registered. */
enum class DeviceKind { CAMERA, ALEXA }

enum class ConnectionStatus { ONLINE, OFFLINE }

data class Device(
    val id: Long,
    val name: String,
    val room: String,
    val kind: DeviceKind,
    val address: String,
    val status: ConnectionStatus,
)
