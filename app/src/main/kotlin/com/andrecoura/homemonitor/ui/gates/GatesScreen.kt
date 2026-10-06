package com.andrecoura.homemonitor.ui.gates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrecoura.homemonitor.R
import com.andrecoura.homemonitor.domain.model.Gate
import com.andrecoura.homemonitor.domain.model.GateCommand
import com.andrecoura.homemonitor.ui.components.ConfirmDialog
import com.andrecoura.homemonitor.ui.components.EmptyState
import com.andrecoura.homemonitor.ui.components.InfoCard
import com.andrecoura.homemonitor.ui.components.StatusChip
import com.andrecoura.homemonitor.ui.components.label
import com.andrecoura.homemonitor.ui.components.relativeTime
import com.andrecoura.homemonitor.ui.components.tone

@Composable
fun GatesRoute(snackbar: SnackbarHostState) {
    val viewModel: GatesViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sent = stringResource(R.string.gate_message_sent)
    val failed = stringResource(R.string.gate_message_failed)
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(if (message == GateMessage.COMMAND_SENT) sent else failed)
        viewModel.onMessageShown()
    }
    GatesScreen(state, viewModel::onCommandRequested, viewModel::onConfirm, viewModel::onDismissConfirmation)
}

@Composable
fun GatesScreen(
    state: GatesUiState,
    onCommandRequested: (Gate) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!state.isLoading && state.gates.isEmpty()) {
        EmptyState(stringResource(R.string.gates_empty))
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.gates, key = { it.id }) { gate ->
                GateCard(gate, busy = gate.id in state.busyGateIds, onClick = { onCommandRequested(gate) })
            }
        }
    }
    state.pending?.let { pending ->
        val opening = pending.command == GateCommand.OPEN
        ConfirmDialog(
            title = stringResource(if (opening) R.string.gate_confirm_open_title else R.string.gate_confirm_close_title),
            text = stringResource(R.string.gate_confirm_text, pending.gate.name),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun GateCard(
    gate: Gate,
    busy: Boolean,
    onClick: () -> Unit,
) {
    val command = gate.suggestedCommand
    InfoCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(gate.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.gate_since, relativeTime(gate.stateSince)),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                StatusChip(gate.state.label(), gate.state.tone())
            }
            val actionLabel =
                when (command) {
                    GateCommand.OPEN -> stringResource(R.string.gate_action_open)
                    GateCommand.CLOSE -> stringResource(R.string.gate_action_close)
                    null -> stringResource(R.string.gate_unavailable)
                }
            val actionDescription =
                when (command) {
                    GateCommand.OPEN -> stringResource(R.string.gate_action_open_named, gate.name)
                    GateCommand.CLOSE -> stringResource(R.string.gate_action_close_named, gate.name)
                    null -> actionLabel
                }
            Button(
                onClick = onClick,
                enabled = command != null && !busy,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = actionDescription },
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(actionLabel)
            }
        }
    }
}
