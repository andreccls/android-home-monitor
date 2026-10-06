package com.andrecoura.homemonitor.ui.intercom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrecoura.homemonitor.domain.model.CallState
import com.andrecoura.homemonitor.domain.model.GateCommand
import com.andrecoura.homemonitor.domain.model.IntercomCall
import com.andrecoura.homemonitor.domain.repository.GateRepository
import com.andrecoura.homemonitor.domain.repository.IntercomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class IntercomMessage { ANSWER_FAILED, GATE_OPENING, GATE_FAILED }

data class IntercomUiState(
    val isLoading: Boolean = true,
    val calls: List<IntercomCall> = emptyList(),
    /** The call in front of the user: a ringing one, or the one they just answered. */
    val activeCall: IntercomCall? = null,
    val confirmOpenGateFor: IntercomCall? = null,
    val isBusy: Boolean = false,
    val message: IntercomMessage? = null,
) {
    val history: List<IntercomCall> get() = calls.filter { it.id != activeCall?.id }
}

@HiltViewModel
class IntercomViewModel
    @Inject
    constructor(
        private val intercom: IntercomRepository,
        private val gates: GateRepository,
    ) : ViewModel() {
        private data class Local(
            val answeredCallId: String? = null,
            val confirmOpenGateFor: IntercomCall? = null,
            val busy: Boolean = false,
            val message: IntercomMessage? = null,
        )

        private val local = MutableStateFlow(Local())

        val uiState: StateFlow<IntercomUiState> =
            combine(intercom.observeCalls(), local) { calls, state ->
                val active =
                    calls.firstOrNull { it.state == CallState.RINGING }
                        ?: calls.firstOrNull { it.id == state.answeredCallId }
                IntercomUiState(false, calls, active, state.confirmOpenGateFor, state.busy, state.message)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), IntercomUiState())

        fun onAnswer(call: IntercomCall) {
            local.update { it.copy(busy = true) }
            viewModelScope.launch {
                val result = intercom.answer(call.id)
                local.update {
                    it.copy(
                        busy = false,
                        answeredCallId = if (result.isSuccess) call.id else it.answeredCallId,
                        message = if (result.isSuccess) null else IntercomMessage.ANSWER_FAILED,
                    )
                }
            }
        }

        fun onOpenGateRequested(call: IntercomCall) = local.update { it.copy(confirmOpenGateFor = call) }

        fun onDismissConfirmation() = local.update { it.copy(confirmOpenGateFor = null) }

        fun onConfirmOpenGate() {
            val call = local.value.confirmOpenGateFor ?: return
            local.update { it.copy(confirmOpenGateFor = null, busy = true) }
            viewModelScope.launch {
                val result = gates.send(call.gateId, GateCommand.OPEN)
                local.update {
                    it.copy(busy = false, message = if (result.isSuccess) IntercomMessage.GATE_OPENING else IntercomMessage.GATE_FAILED)
                }
            }
        }

        fun onEndCall() = local.update { it.copy(answeredCallId = null) }

        fun onMessageShown() = local.update { it.copy(message = null) }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
