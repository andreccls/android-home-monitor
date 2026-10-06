package com.andrecoura.homemonitor.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrecoura.homemonitor.domain.model.Alert
import com.andrecoura.homemonitor.domain.model.ConnectionStatus
import com.andrecoura.homemonitor.domain.model.Device
import com.andrecoura.homemonitor.domain.model.DeviceKind
import com.andrecoura.homemonitor.domain.model.Gate
import com.andrecoura.homemonitor.domain.model.IntercomCall
import com.andrecoura.homemonitor.domain.repository.AlertRepository
import com.andrecoura.homemonitor.domain.repository.DeviceRepository
import com.andrecoura.homemonitor.domain.repository.GateRepository
import com.andrecoura.homemonitor.domain.repository.IntercomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val devices: List<Device> = emptyList(),
    val gates: List<Gate> = emptyList(),
    val lastCall: IntercomCall? = null,
    val recentAlerts: List<Alert> = emptyList(),
) {
    val cameras: List<Device> get() = devices.filter { it.kind == DeviceKind.CAMERA }
    val alexas: List<Device> get() = devices.filter { it.kind == DeviceKind.ALEXA }
    val offlineCount: Int get() = devices.count { it.status == ConnectionStatus.OFFLINE }

    fun onlineCount(list: List<Device>): Int = list.count { it.status == ConnectionStatus.ONLINE }
}

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        devices: DeviceRepository,
        gates: GateRepository,
        intercom: IntercomRepository,
        alerts: AlertRepository,
    ) : ViewModel() {
        val uiState: StateFlow<HomeUiState> =
            combine(
                devices.observeAll(),
                gates.observeAll(),
                intercom.observeCalls(),
                alerts.observeRecent(RECENT_ALERTS),
            ) { deviceList, gateList, calls, alertList ->
                HomeUiState(false, deviceList, gateList, calls.firstOrNull(), alertList)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

        private companion object {
            const val RECENT_ALERTS = 3
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
