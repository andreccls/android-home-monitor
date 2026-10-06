package com.andrecoura.homemonitor.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrecoura.homemonitor.domain.model.Device
import com.andrecoura.homemonitor.domain.model.DeviceKind
import com.andrecoura.homemonitor.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class DeviceFilter(
    val kind: DeviceKind?,
) {
    ALL(null),
    CAMERAS(DeviceKind.CAMERA),
    ALEXAS(DeviceKind.ALEXA),
}

data class DeviceListUiState(
    val isLoading: Boolean = true,
    val filter: DeviceFilter = DeviceFilter.ALL,
    val devices: List<Device> = emptyList(),
)

@HiltViewModel
class DeviceListViewModel
    @Inject
    constructor(
        repository: DeviceRepository,
    ) : ViewModel() {
        private val filter = MutableStateFlow(DeviceFilter.ALL)

        val uiState: StateFlow<DeviceListUiState> =
            combine(repository.observeAll(), filter) { devices, selected ->
                DeviceListUiState(false, selected, devices.filter { selected.kind == null || it.kind == selected.kind })
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DeviceListUiState())

        fun onFilterSelected(value: DeviceFilter) = filter.update { value }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
