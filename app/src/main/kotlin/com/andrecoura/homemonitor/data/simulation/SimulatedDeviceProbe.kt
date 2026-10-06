package com.andrecoura.homemonitor.data.simulation

import com.andrecoura.homemonitor.domain.model.CameraFrame
import com.andrecoura.homemonitor.domain.model.ConnectionStatus
import com.andrecoura.homemonitor.domain.model.Device
import com.andrecoura.homemonitor.domain.port.CameraStream
import com.andrecoura.homemonitor.domain.port.DeviceProbe
import com.andrecoura.homemonitor.domain.port.StreamUnavailableException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/** By convention an address containing "offline" never answers, handy for demos and tests. */
internal fun Device.isForcedOffline() = address.contains("offline", ignoreCase = true)

/** A fake reachability check: devices answer after a delay and now and then drop for a while. */
class SimulatedDeviceProbe(
    private val config: SimulationConfig,
    private val clock: Clock,
    private val random: Random = Random.Default,
) : DeviceProbe {
    private val outageEndsAt = ConcurrentHashMap<Long, Long>()

    override suspend fun isReachable(device: Device): Boolean {
        delay(random.nextLong(config.latencyMillis.first, config.latencyMillis.last + 1))
        if (device.isForcedOffline()) return false
        val now = clock.millis()
        if ((outageEndsAt[device.id] ?: 0L) > now) return false
        if (random.nextDouble() < config.deviceOutageChance) {
            outageEndsAt[device.id] = now + config.deviceOutageMillis
            return false
        }
        return true
    }
}

/** A fake video feed: only frame counters; the UI paints something from them. */
class SimulatedCameraStream(
    private val config: SimulationConfig,
    private val clock: Clock,
) : CameraStream {
    override fun frames(device: Device): Flow<CameraFrame> =
        flow {
            if (device.isForcedOffline() || device.status == ConnectionStatus.OFFLINE) {
                throw StreamUnavailableException("No signal from ${device.name}")
            }
            var sequence = 0L
            while (true) {
                emit(CameraFrame(sequence++, clock.instant()))
                delay(config.frameIntervalMillis)
            }
        }
}
