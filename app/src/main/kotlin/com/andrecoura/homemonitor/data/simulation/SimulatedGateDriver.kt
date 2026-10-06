package com.andrecoura.homemonitor.data.simulation

import com.andrecoura.homemonitor.domain.model.GateCommand
import com.andrecoura.homemonitor.domain.model.GateState
import com.andrecoura.homemonitor.domain.port.DeviceUnreachableException
import com.andrecoura.homemonitor.domain.port.DiscoveredGate
import com.andrecoura.homemonitor.domain.port.GateDriver
import com.andrecoura.homemonitor.domain.port.GateReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

object SimulatedHome {
    const val GARAGE_GATE_ID = "gate-garage"
    const val FRONT_GATE_ID = "gate-front"
    const val INTERCOM_NAME = "Interfone da portaria"
}

/** A fake pair of gates: commands take time, travel takes time, some commands fail, and visitors come and go. */
class SimulatedGateDriver(
    private val scope: CoroutineScope,
    private val config: SimulationConfig,
    private val random: Random = Random.Default,
) : GateDriver {
    private val states =
        ConcurrentHashMap<String, GateState>().apply {
            put(SimulatedHome.GARAGE_GATE_ID, GateState.CLOSED)
            put(SimulatedHome.FRONT_GATE_ID, GateState.CLOSED)
        }
    private val travels = ConcurrentHashMap<String, Job>()
    private val reports = MutableSharedFlow<GateReport>(extraBufferCapacity = 64)

    override suspend fun discover(): List<DiscoveredGate> {
        pause()
        return listOf(
            DiscoveredGate(SimulatedHome.GARAGE_GATE_ID, "Portão da garagem", states.getValue(SimulatedHome.GARAGE_GATE_ID)),
            DiscoveredGate(SimulatedHome.FRONT_GATE_ID, "Portão social", states.getValue(SimulatedHome.FRONT_GATE_ID)),
        )
    }

    override fun observe(): Flow<GateReport> = merge(reports, visitorsAndOutages())

    override suspend fun send(
        gateId: String,
        command: GateCommand,
    ) {
        pause()
        val current = states[gateId] ?: throw DeviceUnreachableException("Unknown gate $gateId")
        if (current == GateState.OFFLINE || random.nextDouble() < config.commandFailureRate) {
            throw DeviceUnreachableException("Gate $gateId did not answer")
        }
        travel(gateId, if (command == GateCommand.OPEN) GateState.OPEN else GateState.CLOSED)
    }

    private fun travel(
        gateId: String,
        target: GateState,
    ) {
        val moving = if (target == GateState.OPEN) GateState.OPENING else GateState.CLOSING
        travels.remove(gateId)?.cancel()
        set(gateId, moving)
        travels[gateId] =
            scope.launch {
                delay(config.gateTravelMillis)
                set(gateId, target)
            }
    }

    private fun set(
        gateId: String,
        state: GateState,
    ) {
        states[gateId] = state
        reports.tryEmit(GateReport(gateId, state))
    }

    private fun visitorsAndOutages(): Flow<GateReport> =
        flow {
            val every = config.gateActivityEveryMillis ?: return@flow
            while (true) {
                delay(random.nextLong(every.first, every.last + 1))
                val gateId = states.keys.random(random)
                if (states[gateId] != GateState.CLOSED) continue
                if (random.nextInt(4) == 0) {
                    set(gateId, GateState.OFFLINE)
                    delay(config.gateOutageMillis)
                    set(gateId, GateState.CLOSED)
                } else {
                    travel(gateId, GateState.OPEN)
                    // One visitor in three forgets the gate open, which is what the "left open" alert is for.
                    if (random.nextInt(3) != 0) {
                        delay(config.gateTravelMillis + config.gateOpenForMillis)
                        travel(gateId, GateState.CLOSED)
                    }
                }
            }
        }

    private suspend fun pause() = delay(random.nextLong(config.latencyMillis.first, config.latencyMillis.last + 1))
}
