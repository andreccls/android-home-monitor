package com.andrecoura.homemonitor.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.andrecoura.homemonitor.domain.model.Alert
import com.andrecoura.homemonitor.domain.model.CallState
import com.andrecoura.homemonitor.domain.model.Gate
import com.andrecoura.homemonitor.domain.model.IntercomCall
import com.andrecoura.homemonitor.ui.components.AlertRow
import com.andrecoura.homemonitor.ui.components.InfoCard
import com.andrecoura.homemonitor.ui.components.SectionTitle
import com.andrecoura.homemonitor.ui.components.StatusChip
import com.andrecoura.homemonitor.ui.components.Tone
import com.andrecoura.homemonitor.ui.components.label
import com.andrecoura.homemonitor.ui.components.relativeTime
import com.andrecoura.homemonitor.ui.components.tone

@Composable
fun HomeRoute(
    onOpenAlerts: () -> Unit,
    onOpenGates: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenIntercom: () -> Unit,
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state, onOpenAlerts, onOpenGates, onOpenDevices, onOpenIntercom)
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenAlerts: () -> Unit,
    onOpenGates: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenIntercom: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(stringResource(R.string.home_simulated_notice), style = MaterialTheme.typography.bodySmall) }
        item { SectionTitle(stringResource(R.string.home_section_devices)) }
        item {
            val cameras = state.cameras
            val alexas = state.alexas
            Row(Modifier.fillMaxWidth().clickable(onClick = onOpenDevices), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(stringResource(R.string.home_cameras_online), state.onlineCount(cameras), cameras.size, Modifier.weight(1f))
                SummaryCard(stringResource(R.string.home_alexas_online), state.onlineCount(alexas), alexas.size, Modifier.weight(1f))
            }
        }
        item {
            InfoCard(Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.home_offline), style = MaterialTheme.typography.titleSmall)
                    StatusChip(state.offlineCount.toString(), if (state.offlineCount == 0) Tone.Good else Tone.Bad)
                }
            }
        }
        item { SectionTitle(stringResource(R.string.home_section_gates)) }
        items(state.gates, key = { it.id }) { gate -> GateSummary(gate, onOpenGates) }
        item { SectionTitle(stringResource(R.string.home_section_intercom)) }
        item { LastCall(state.lastCall, onOpenIntercom) }
        item { SectionTitle(stringResource(R.string.home_section_alerts)) }
        if (state.recentAlerts.isEmpty()) {
            item { Text(stringResource(R.string.home_no_alerts), style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(state.recentAlerts, key = { it.id }) { alert -> AlertRow(alert) }
            item { TextButton(onClick = onOpenAlerts) { Text(stringResource(R.string.home_see_all)) } }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    online: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val description = "$title: " + stringResource(R.string.home_count_of, online, total)
    InfoCard(modifier.semantics(mergeDescendants = true) { contentDescription = description }) {
        Column {
            Text(online.toString(), style = MaterialTheme.typography.headlineMedium)
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.home_count_of, online, total), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun GateSummary(
    gate: Gate,
    onClick: () -> Unit,
) {
    InfoCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(gate.name, style = MaterialTheme.typography.titleSmall)
            StatusChip(gate.state.label(), gate.state.tone())
        }
    }
}

@Composable
private fun LastCall(
    call: IntercomCall?,
    onClick: () -> Unit,
) {
    InfoCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        if (call == null) {
            Text(stringResource(R.string.home_no_calls))
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(call.intercomName, style = MaterialTheme.typography.titleSmall)
                    Text(relativeTime(call.startedAt), style = MaterialTheme.typography.bodySmall)
                }
                StatusChip(call.state.label(), if (call.state == CallState.MISSED) Tone.Bad else Tone.Good)
            }
        }
    }
}
