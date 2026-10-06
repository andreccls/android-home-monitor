package com.andrecoura.homemonitor.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrecoura.homemonitor.domain.model.Alert
import com.andrecoura.homemonitor.domain.repository.AlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AlertsUiState(
    val isLoading: Boolean = true,
    val alerts: List<Alert> = emptyList(),
)

@HiltViewModel
class AlertsViewModel
    @Inject
    constructor(
        repository: AlertRepository,
    ) : ViewModel() {
        val uiState: StateFlow<AlertsUiState> =
            repository
                .observeRecent(LIMIT)
                .map { AlertsUiState(false, it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AlertsUiState())

        private companion object {
            const val LIMIT = 50
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
