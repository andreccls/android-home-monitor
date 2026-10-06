package com.andrecoura.homemonitor.ui.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.andrecoura.homemonitor.domain.model.Device
import com.andrecoura.homemonitor.domain.model.DeviceKind
import com.andrecoura.homemonitor.ui.components.EmptyState
import com.andrecoura.homemonitor.ui.components.IconBadge
import com.andrecoura.homemonitor.ui.components.InfoCard
import com.andrecoura.homemonitor.ui.components.StatusChip
import com.andrecoura.homemonitor.ui.components.label
import com.andrecoura.homemonitor.ui.components.tone
import com.andrecoura.homemonitor.ui.theme.AppIcons

@Composable
fun DeviceListRoute(
    onAdd: () -> Unit,
    onEdit: (Device) -> Unit,
    onWatch: (Device) -> Unit,
) {
    val viewModel: DeviceListViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DeviceListScreen(state, viewModel::onFilterSelected, onAdd, onEdit, onWatch)
}

@Composable
fun DeviceListScreen(
    state: DeviceListUiState,
    onFilterSelected: (DeviceFilter) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Device) -> Unit,
    onWatch: (Device) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeviceFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { onFilterSelected(filter) },
                        label = {
                            Text(
                                stringResource(
                                    when (filter) {
                                        DeviceFilter.ALL -> R.string.devices_filter_all
                                        DeviceFilter.CAMERAS -> R.string.devices_filter_cameras
                                        DeviceFilter.ALEXAS -> R.string.devices_filter_alexas
                                    },
                                ),
                            )
                        },
                    )
                }
            }
            if (!state.isLoading && state.devices.isEmpty()) {
                EmptyState(stringResource(R.string.devices_empty))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.devices, key = { it.id }) { DeviceRow(it, onEdit = { onEdit(it) }, onWatch = { onWatch(it) }) }
                }
            }
        }
        FloatingActionButton(onClick = onAdd, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.devices_add))
        }
    }
}

@Composable
private fun DeviceRow(
    device: Device,
    onEdit: () -> Unit,
    onWatch: () -> Unit,
) {
    val kind = device.kind.label()
    val status = device.status.label()
    val description = stringResource(R.string.device_item_description, device.name, kind, device.room, status)
    InfoCard(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.device_edit, device.name), onClick = onEdit)
            .semantics(mergeDescendants = false) { contentDescription = description },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(if (device.kind == DeviceKind.CAMERA) AppIcons.Videocam else AppIcons.Speaker)
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(device.name, style = MaterialTheme.typography.titleSmall)
                Text("$kind · ${device.room}", style = MaterialTheme.typography.bodySmall)
            }
            StatusChip(status, device.status.tone())
            if (device.kind == DeviceKind.CAMERA) {
                IconButton(onClick = onWatch) {
                    Icon(AppIcons.Videocam, contentDescription = stringResource(R.string.device_watch, device.name))
                }
            }
        }
    }
}
