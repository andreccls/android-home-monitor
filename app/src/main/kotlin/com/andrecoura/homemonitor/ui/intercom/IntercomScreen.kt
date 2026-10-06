package com.andrecoura.homemonitor.ui.intercom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrecoura.homemonitor.R
import com.andrecoura.homemonitor.domain.model.CallState
import com.andrecoura.homemonitor.domain.model.IntercomCall
import com.andrecoura.homemonitor.ui.components.ConfirmDialog
import com.andrecoura.homemonitor.ui.components.InfoCard
import com.andrecoura.homemonitor.ui.components.SectionTitle
import com.andrecoura.homemonitor.ui.components.StatusChip
import com.andrecoura.homemonitor.ui.components.Tone
import com.andrecoura.homemonitor.ui.components.label
import com.andrecoura.homemonitor.ui.components.relativeTime

@Composable
fun IntercomRoute(snackbar: SnackbarHostState) {
    val viewModel: IntercomViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val answerFailed = stringResource(R.string.intercom_message_answer_failed)
    val gateOpening = stringResource(R.string.intercom_message_gate_opening)
    val gateFailed = stringResource(R.string.intercom_message_gate_failed)
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(
            when (message) {
                IntercomMessage.ANSWER_FAILED -> answerFailed
                IntercomMessage.GATE_OPENING -> gateOpening
                IntercomMessage.GATE_FAILED -> gateFailed
            },
        )
        viewModel.onMessageShown()
    }
    IntercomScreen(
        state,
        onAnswer = viewModel::onAnswer,
        onOpenGate = viewModel::onOpenGateRequested,
        onEndCall = viewModel::onEndCall,
        onConfirmOpenGate = viewModel::onConfirmOpenGate,
        onDismissConfirmation = viewModel::onDismissConfirmation,
    )
}

@Composable
fun IntercomScreen(
    state: IntercomUiState,
    onAnswer: (IntercomCall) -> Unit,
    onOpenGate: (IntercomCall) -> Unit,
    onEndCall: () -> Unit,
    onConfirmOpenGate: () -> Unit,
    onDismissConfirmation: () -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            val active = state.activeCall
            if (active == null) {
                InfoCard(Modifier.fillMaxWidth()) { Text(stringResource(R.string.intercom_waiting)) }
            } else {
                ActiveCall(active, state.isBusy, onAnswer, onOpenGate, onEndCall)
            }
        }
        item { SectionTitle(stringResource(R.string.intercom_history)) }
        if (state.history.isEmpty()) {
            item { Text(stringResource(R.string.intercom_history_empty), style = MaterialTheme.typography.bodyMedium) }
        }
        items(state.history, key = { it.id }) { CallRow(it) }
    }
    state.confirmOpenGateFor?.let { call ->
        ConfirmDialog(
            title = stringResource(R.string.intercom_confirm_title),
            text = stringResource(R.string.intercom_confirm_text, call.intercomName),
            onConfirm = onConfirmOpenGate,
            onDismiss = onDismissConfirmation,
        )
    }
}

@Composable
private fun ActiveCall(
    call: IntercomCall,
    busy: Boolean,
    onAnswer: (IntercomCall) -> Unit,
    onOpenGate: (IntercomCall) -> Unit,
    onEndCall: () -> Unit,
) {
    val ringing = call.state == CallState.RINGING
    InfoCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(if (ringing) R.string.intercom_active_ringing else R.string.intercom_active_answered),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(call.intercomName, style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (ringing) {
                    Button(onClick = { onAnswer(call) }, enabled = !busy) { Text(stringResource(R.string.intercom_answer)) }
                }
                OutlinedButton(onClick = { onOpenGate(call) }, enabled = !busy) { Text(stringResource(R.string.intercom_open_gate)) }
                if (!ringing) {
                    TextButton(onClick = onEndCall) { Text(stringResource(R.string.intercom_end_call)) }
                }
            }
        }
    }
}

@Composable
private fun CallRow(call: IntercomCall) {
    InfoCard(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(call.intercomName, style = MaterialTheme.typography.titleSmall)
                Text(relativeTime(call.startedAt), style = MaterialTheme.typography.bodySmall)
            }
            StatusChip(call.state.label(), if (call.state == CallState.MISSED) Tone.Bad else Tone.Good)
        }
    }
}
