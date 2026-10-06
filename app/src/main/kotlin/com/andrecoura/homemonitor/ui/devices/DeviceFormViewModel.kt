package com.andrecoura.homemonitor.ui.devices

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrecoura.homemonitor.domain.model.DeviceDraft
import com.andrecoura.homemonitor.domain.model.DeviceError
import com.andrecoura.homemonitor.domain.model.DeviceKind
import com.andrecoura.homemonitor.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceFormUiState(
    val draft: DeviceDraft = DeviceDraft(),
    val isEditing: Boolean = false,
    /** Errors appear only after the first save attempt, so the form does not shout while being filled. */
    val errors: Set<DeviceError> = emptySet(),
    /** Set after a successful save or delete; the screen reacts by navigating back. */
    val isDone: Boolean = false,
)

@HiltViewModel
class DeviceFormViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: DeviceRepository,
    ) : ViewModel() {
        private val deviceId: Long? = savedStateHandle.get<Long>(DEVICE_ID_ARG)?.takeIf { it > 0 }

        private val _uiState = MutableStateFlow(DeviceFormUiState(isEditing = deviceId != null))
        val uiState: StateFlow<DeviceFormUiState> = _uiState.asStateFlow()

        init {
            if (deviceId != null) {
                viewModelScope.launch {
                    repository.get(deviceId)?.let { device ->
                        _uiState.update { it.copy(draft = DeviceDraft(device.name, device.room, device.kind, device.address)) }
                    }
                }
            }
        }

        fun onNameChange(value: String) = edit { it.copy(name = value) }

        fun onRoomChange(value: String) = edit { it.copy(room = value) }

        fun onAddressChange(value: String) = edit { it.copy(address = value) }

        fun onKindChange(value: DeviceKind) = edit { it.copy(kind = value) }

        fun onSave() {
            val draft = _uiState.value.draft
            val errors = draft.validate()
            if (errors.isNotEmpty()) {
                _uiState.update { it.copy(errors = errors) }
                return
            }
            viewModelScope.launch {
                if (deviceId == null) repository.add(draft) else repository.update(deviceId, draft)
                _uiState.update { it.copy(isDone = true) }
            }
        }

        fun onDelete() {
            val id = deviceId ?: return
            viewModelScope.launch {
                repository.delete(id)
                _uiState.update { it.copy(isDone = true) }
            }
        }

        private fun edit(change: (DeviceDraft) -> DeviceDraft) =
            _uiState.update { state ->
                val draft = change(state.draft)
                // Once errors are showing, keep them in sync while the user fixes the fields.
                state.copy(draft = draft, errors = if (state.errors.isEmpty()) state.errors else draft.validate())
            }

        companion object {
            const val DEVICE_ID_ARG = "deviceId"
        }
    }
