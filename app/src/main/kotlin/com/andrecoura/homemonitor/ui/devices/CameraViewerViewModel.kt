package com.andrecoura.homemonitor.ui.devices

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrecoura.homemonitor.domain.model.CameraFrame
import com.andrecoura.homemonitor.domain.model.Device
import com.andrecoura.homemonitor.domain.port.CameraStream
import com.andrecoura.homemonitor.domain.port.StreamUnavailableException
import com.andrecoura.homemonitor.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

sealed interface CameraViewerUiState {
    data object Connecting : CameraViewerUiState

    data class Live(
        val device: Device,
        val frame: CameraFrame,
    ) : CameraViewerUiState

    data class NoSignal(
        val device: Device,
    ) : CameraViewerUiState

    data object NotFound : CameraViewerUiState
}

@HiltViewModel
class CameraViewerViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        repository: DeviceRepository,
        private val stream: CameraStream,
    ) : ViewModel() {
        private val retries = MutableStateFlow(0)
        private val deviceId: Long = savedStateHandle.get<Long>(DeviceFormViewModel.DEVICE_ID_ARG) ?: 0L

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<CameraViewerUiState> =
            combine(repository.observe(deviceId).distinctUntilChanged(), retries) { device, _ -> device }
                .flatMapLatest { device -> if (device == null) flowOf(CameraViewerUiState.NotFound) else connect(device) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CameraViewerUiState.Connecting)

        fun onRetry() = retries.update { it + 1 }

        private fun connect(device: Device): Flow<CameraViewerUiState> =
            stream
                .frames(device)
                .map<CameraFrame, CameraViewerUiState> { CameraViewerUiState.Live(device, it) }
                .catch { error ->
                    if (error is StreamUnavailableException) emit(CameraViewerUiState.NoSignal(device)) else throw error
                }.onStart { emit(CameraViewerUiState.Connecting) }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
