package com.andrecoura.homemonitor.data.monitoring

import com.andrecoura.homemonitor.data.local.AlertDao
import com.andrecoura.homemonitor.data.local.CallDao
import com.andrecoura.homemonitor.data.local.DeviceDao
import com.andrecoura.homemonitor.data.local.GateDao
import com.andrecoura.homemonitor.data.local.GateEntity
import com.andrecoura.homemonitor.data.local.toDomain
import com.andrecoura.homemonitor.data.local.toEntity
import com.andrecoura.homemonitor.domain.model.Alert
import com.andrecoura.homemonitor.domain.model.AlertType
import com.andrecoura.homemonitor.domain.model.CallState
import com.andrecoura.homemonitor.domain.model.ConnectionStatus
import com.andrecoura.homemonitor.domain.model.GateState
import com.andrecoura.homemonitor.domain.model.IntercomCall
import com.andrecoura.homemonitor.domain.port.DeviceProbe
import com.andrecoura.homemonitor.domain.port.GateDriver
import com.andrecoura.homemonitor.domain.port.GateReport
import com.andrecoura.homemonitor.domain.port.IntercomDriver
import com.andrecoura.homemonitor.domain.port.IntercomEvent
import com.andrecoura.homemonitor.domain.rules.AlertRules
import com.andrecoura.homemonitor.domain.rules.MonitoringPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * The only writer of "what the house is doing": it listens to the ports, writes the results to Room
 * and raises alerts through [AlertRules]. The UI never sees the ports, only Room (through repositories).
 */
class MonitoringService
    @Inject
    constructor(
        private val deviceDao: DeviceDao,
        private val gateDao: GateDao,
        private val callDao: CallDao,
        private val alertDao: AlertDao,
        private val gateDriver: GateDriver,
        private val intercomDriver: IntercomDriver,
        private val probe: DeviceProbe,
        private val clock: Clock,
        private val policy: MonitoringPolicy,
    ) {
        /** Gates that already raised "left open" during their current opening, to alert once per opening. */
        private val alertedOpenGates = ConcurrentHashMap.newKeySet<String>()

        fun start(scope: CoroutineScope): Job =
            scope.launch {
                syncGates()
                launch { gateDriver.observe().collect(::onGateReport) }
                launch { intercomDriver.events().collect(::onCallEvent) }
                launch { watchDevices() }
                launch { watchOpenGates() }
            }

        internal suspend fun syncGates() {
            val now = clock.millis()
            gateDriver.discover().forEach { gateDao.insertIfAbsent(GateEntity(it.id, it.name, it.state, now)) }
        }

        internal suspend fun onGateReport(report: GateReport) {
            val gate = gateDao.get(report.gateId) ?: return
            if (gate.state == report.state) return
            gateDao.updateState(gate.id, report.state, clock.millis())
            if (report.state != GateState.OPEN) alertedOpenGates -= gate.id
            if (report.state == GateState.OFFLINE) {
                alertDao.insert(Alert(0, AlertType.DEVICE_OFFLINE, gate.name, clock.instant()).toEntity())
            }
        }

        internal suspend fun onCallEvent(event: IntercomEvent) {
            when (event) {
                is IntercomEvent.Incoming -> {
                    callDao.upsert(IntercomCall(event.callId, event.intercomName, event.gateId, CallState.RINGING, event.at).toEntity())
                }

                is IntercomEvent.Missed -> {
                    val call = callDao.get(event.callId)?.toDomain() ?: return
                    if (call.state != CallState.RINGING) return
                    val missed = call.copy(state = CallState.MISSED)
                    callDao.updateState(call.id, CallState.MISSED)
                    AlertRules.missedCall(missed, clock.instant())?.let { alertDao.insert(it.toEntity()) }
                }
            }
        }

        /** Probes every device now, then again every interval; restarts when devices are added or re-addressed. */
        private suspend fun watchDevices() {
            deviceDao
                .observeAll()
                .map { devices -> devices.map { it.id to it.address } }
                .distinctUntilChanged()
                .collectLatest {
                    while (true) {
                        probeAll()
                        delay(policy.probeInterval.toMillis())
                    }
                }
        }

        internal suspend fun probeAll() {
            for (entity in deviceDao.getAll()) {
                val device = entity.toDomain()
                val status = if (probe.isReachable(device)) ConnectionStatus.ONLINE else ConnectionStatus.OFFLINE
                if (status == device.status) continue
                deviceDao.updateStatus(device.id, status)
                AlertRules
                    .deviceWentOffline(device.copy(status = status), device.status, clock.instant())
                    ?.let { alertDao.insert(it.toEntity()) }
            }
        }

        private suspend fun watchOpenGates() {
            while (true) {
                checkOpenGates()
                delay(policy.gateCheckInterval.toMillis())
            }
        }

        internal suspend fun checkOpenGates() {
            val now = clock.instant()
            for (gate in gateDao.getAll().map { it.toDomain() }) {
                val alert = AlertRules.gateLeftOpen(gate, now, policy.gateOpenLimit) ?: continue
                if (alertedOpenGates.add(gate.id)) alertDao.insert(alert.toEntity())
            }
        }
    }
