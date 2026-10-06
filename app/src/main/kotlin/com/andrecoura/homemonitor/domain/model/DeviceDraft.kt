package com.andrecoura.homemonitor.domain.model

enum class DeviceError { NAME_REQUIRED, NAME_TOO_LONG, ROOM_REQUIRED, ADDRESS_INVALID }

/** What the user types in the form, before the device exists. Holds the validation rules. */
data class DeviceDraft(
    val name: String = "",
    val room: String = "",
    val kind: DeviceKind = DeviceKind.CAMERA,
    val address: String = "",
) {
    fun validate(): Set<DeviceError> =
        buildSet {
            if (name.isBlank()) add(DeviceError.NAME_REQUIRED)
            if (name.trim().length > MAX_NAME) add(DeviceError.NAME_TOO_LONG)
            if (room.isBlank()) add(DeviceError.ROOM_REQUIRED)
            if (!addressIsValid()) add(DeviceError.ADDRESS_INVALID)
        }

    private fun addressIsValid(): Boolean {
        val value = address.trim()
        return when (kind) {
            DeviceKind.CAMERA -> CAMERA_URL.matches(value)
            DeviceKind.ALEXA -> ALEXA_ID.matches(value)
        }
    }

    companion object {
        const val MAX_NAME = 40
        private val CAMERA_URL = Regex("^(rtsp|http|https)://[^\\s/]+(/\\S*)?$", RegexOption.IGNORE_CASE)
        private val ALEXA_ID = Regex("^[A-Za-z0-9-]{6,32}$")
    }
}
