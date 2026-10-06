package com.andrecoura.homemonitor.ui.gates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrecoura.homemonitor.domain.model.Gate
import com.andrecoura.homemonitor.domain.model.GateCommand
import com.andrecoura.homemonitor.domain.repository.GateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A command waiting for the user's "yes". Nothing reaches the gate before the confirmation. */
data class PendingCommand(
    val gate: Gate,
    val command: GateCommand,
)

enum class GateMessage { COMMAND_SENT, COMMAND_FAILED }

data class GatesUiState(
    val isLoading: Boolean = true,
    val gates: List<Gate> = emptyList(),
    val pending: PendingCommand? = null,
    val busyGateIds: Set<String> = emptySet(),
    val message: GateMessage? = null,
)

@HiltViewModel
class GatesViewModel
    @Inject
    constructor(
        private val repository: GateRepository,
    ) : ViewModel() {
        private data class Local(
            val pending: PendingCommand? = null,
            val busy: Set<String> = emptySet(),
            val message: GateMessage? = null,
        )

        private val local = MutableStateFlow(Local())

        val uiState: StateFlow<GatesUiState> =
            combine(repository.observeAll(), local) { gates, state ->
                GatesUiState(false, gates, state.pending, state.busy, state.message)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GatesUiState())

        fun onCommandRequested(gate: Gate) {
            val command = gate.suggestedCommand ?: return
            local.update { it.copy(pending = PendingCommand(gate, command)) }
        }

        fun onDismissConfirmation() = local.update { it.copy(pending = null) }

        fun onConfirm() {
            val pending = local.value.pending ?: return
            val id = pending.gate.id
            local.update { it.copy(pending = null, busy = it.busy + id) }
            viewModelScope.launch {
                val result = repository.send(id, pending.command)
                local.update {
                    it.copy(
                        busy = it.busy - id,
                        message = if (result.isSuccess) GateMessage.COMMAND_SENT else GateMessage.COMMAND_FAILED,
                    )
                }
            }
        }

        fun onMessageShown() = local.update { it.copy(message = null) }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
