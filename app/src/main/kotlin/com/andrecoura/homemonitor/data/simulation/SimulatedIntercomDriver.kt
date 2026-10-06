package com.andrecoura.homemonitor.data.simulation

import com.andrecoura.homemonitor.domain.port.CallNoLongerRingingException
import com.andrecoura.homemonitor.domain.port.IntercomDriver
import com.andrecoura.homemonitor.domain.port.IntercomEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/** A fake doorbell: rings now and then, and turns into a missed call if nobody answers in time. */
class SimulatedIntercomDriver(
    private val config: SimulationConfig,
    private val clock: Clock,
    private val random: Random = Random.Default,
) : IntercomDriver {
    private val ringing = ConcurrentHashMap.newKeySet<String>()

    override fun events(): Flow<IntercomEvent> =
        flow {
            val every = config.callEveryMillis ?: return@flow
            while (true) {
                delay(random.nextLong(every.first, every.last + 1))
                val callId = UUID.nameUUIDFromBytes(random.nextBytes(8)).toString()
                ringing += callId
                emit(IntercomEvent.Incoming(callId, SimulatedHome.INTERCOM_NAME, SimulatedHome.FRONT_GATE_ID, clock.instant()))
                delay(config.ringTimeoutMillis)
                if (ringing.remove(callId)) emit(IntercomEvent.Missed(callId))
            }
        }

    override suspend fun answer(callId: String) {
        delay(random.nextLong(config.latencyMillis.first, config.latencyMillis.last + 1))
        if (!ringing.remove(callId)) throw CallNoLongerRingingException(callId)
    }
}
