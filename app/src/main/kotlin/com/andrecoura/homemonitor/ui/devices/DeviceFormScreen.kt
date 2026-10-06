package com.andrecoura.homemonitor.ui.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrecoura.homemonitor.R
import com.andrecoura.homemonitor.domain.model.DeviceError
import com.andrecoura.homemonitor.domain.model.DeviceKind
import com.andrecoura.homemonitor.ui.components.ConfirmDialog
import com.andrecoura.homemonitor.ui.components.label

@Composable
fun DeviceFormRoute(onDone: () -> Unit) {
    val viewModel: DeviceFormViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isDone) { if (state.isDone) onDone() }
    DeviceFormScreen(
        state,
        viewModel::onNameChange,
        viewModel::onRoomChange,
        viewModel::onAddressChange,
        viewModel::onKindChange,
        viewModel::onSave,
        viewModel::onDelete,
    )
}

@Composable
fun DeviceFormScreen(
    state: DeviceFormUiState,
    onNameChange: (String) -> Unit,
    onRoomChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onKindChange: (DeviceKind) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val draft = state.draft
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = draft.name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.form_name)) },
            isError = DeviceError.NAME_REQUIRED in state.errors || DeviceError.NAME_TOO_LONG in state.errors,
            supportingText = {
                when {
                    DeviceError.NAME_REQUIRED in state.errors -> Text(stringResource(R.string.form_error_name_required))
                    DeviceError.NAME_TOO_LONG in state.errors -> Text(stringResource(R.string.form_error_name_too_long))
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.room,
            onValueChange = onRoomChange,
            label = { Text(stringResource(R.string.form_room)) },
            isError = DeviceError.ROOM_REQUIRED in state.errors,
            supportingText = { if (DeviceError.ROOM_REQUIRED in state.errors) Text(stringResource(R.string.form_error_room_required)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.form_type), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeviceKind.entries.forEach { kind ->
                FilterChip(selected = draft.kind == kind, onClick = { onKindChange(kind) }, label = { Text(kind.label()) })
            }
        }
        val isCamera = draft.kind == DeviceKind.CAMERA
        OutlinedTextField(
            value = draft.address,
            onValueChange = onAddressChange,
            label = { Text(stringResource(if (isCamera) R.string.form_address_camera else R.string.form_address_alexa)) },
            isError = DeviceError.ADDRESS_INVALID in state.errors,
            supportingText = {
                if (DeviceError.ADDRESS_INVALID in state.errors) {
                    Text(stringResource(if (isCamera) R.string.form_error_address_camera else R.string.form_error_address_alexa))
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = if (isCamera) KeyboardType.Uri else KeyboardType.Ascii),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
        if (state.isEditing) {
            OutlinedButton(onClick = { confirmingDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.delete))
            }
        }
    }
    if (confirmingDelete) {
        ConfirmDialog(
            title = stringResource(R.string.form_delete_title),
            text = stringResource(R.string.form_delete_text),
            onConfirm = {
                confirmingDelete = false
                onDelete()
            },
            onDismiss = { confirmingDelete = false },
        )
    }
}
