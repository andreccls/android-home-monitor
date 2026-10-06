package com.andrecoura.homemonitor.ui.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrecoura.homemonitor.R
import com.andrecoura.homemonitor.ui.components.AlertRow
import com.andrecoura.homemonitor.ui.components.EmptyState

@Composable
fun AlertsRoute() {
    val viewModel: AlertsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AlertsScreen(state)
}

@Composable
fun AlertsScreen(state: AlertsUiState) {
    if (!state.isLoading && state.alerts.isEmpty()) {
        EmptyState(stringResource(R.string.alerts_empty))
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(state.alerts, key = { it.id }) { AlertRow(it) }
    }
}
