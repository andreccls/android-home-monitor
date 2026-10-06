package com.andrecoura.homemonitor.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.andrecoura.homemonitor.domain.model.Alert
import com.andrecoura.homemonitor.domain.model.AlertType
import com.andrecoura.homemonitor.domain.model.CallState
import com.andrecoura.homemonitor.domain.model.CameraFrame
import com.andrecoura.homemonitor.domain.model.ConnectionStatus
import com.andrecoura.homemonitor.domain.model.DeviceError
import com.andrecoura.homemonitor.domain.model.DeviceKind
import com.andrecoura.homemonitor.domain.model.GateCommand
import com.andrecoura.homemonitor.domain.model.GateState
import com.andrecoura.homemonitor.domain.port.StreamUnavailableException
import com.andrecoura.homemonitor.support.FakeAlertRepository
import com.andrecoura.homemonitor.support.FakeCameraStream
import com.andrecoura.homemonitor.support.FakeDeviceRepository
import com.andrecoura.homemonitor.support.FakeGateRepository
import com.andrecoura.homemonitor.support.FakeIntercomRepository
import com.andrecoura.homemonitor.support.MainDispatcherRule
import com.andrecoura.homemonitor.support.MutableClock
import com.andrecoura.homemonitor.support.call
import com.andrecoura.homemonitor.support.collectInBackground
import com.andrecoura.homemonitor.support.device
import com.andrecoura.homemonitor.support.gate
import com.andrecoura.homemonitor.ui.alerts.AlertsViewModel
import com.andrecoura.homemonitor.ui.devices.CameraViewerUiState
import com.andrecoura.homemonitor.ui.devices.CameraViewerViewModel
import com.andrecoura.homemonitor.ui.devices.DeviceFilter
import com.andrecoura.homemonitor.ui.devices.DeviceFormViewModel
import com.andrecoura.homemonitor.ui.devices.DeviceListViewModel
import com.andrecoura.homemonitor.ui.gates.GateMessage
import com.andrecoura.homemonitor.ui.gates.GatesViewModel
import com.andrecoura.homemonitor.ui.home.HomeViewModel
import com.andrecoura.homemonitor.ui.intercom.IntercomMessage
import com.andrecoura.homemonitor.ui.intercom.IntercomViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `combines devices, gates, last call and recent alerts`() =
        runTest {
            val devices =
                FakeDeviceRepository(
                    listOf(
                        device(1),
                        device(2, status = ConnectionStatus.OFFLINE),
                        device(3, kind = DeviceKind.ALEXA),
                    ),
                )
            val alert = Alert(1, AlertType.MISSED_CALL, "Interfone", MutableClock.T0)
            val vm =
                HomeViewModel(
                    devices,
                    FakeGateRepository(listOf(gate())),
                    FakeIntercomRepository(listOf(call(), call("old"))),
                    FakeAlertRepository(listOf(alert)),
                )
            vm.uiState.test {
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()
                assertEquals(2, state.cameras.size)
                assertEquals(1, state.alexas.size)
                assertEquals(1, state.onlineCount(state.cameras))
                assertEquals(1, state.offlineCount)
                assertEquals("call-1", state.lastCall?.id)
                assertEquals(listOf(alert), state.recentAlerts)
                assertEquals(1, state.gates.size)
            }
        }
}

class DeviceListViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `filter narrows the list by kind`() =
        runTest {
            val repo = FakeDeviceRepository(listOf(device(1), device(2, kind = DeviceKind.ALEXA, address = "G090LF1234567890")))
            val vm = DeviceListViewModel(repo)
            collectInBackground(vm.uiState)
            assertEquals(2, vm.uiState.value.devices.size)
            vm.onFilterSelected(DeviceFilter.ALEXAS)
            assertEquals(
                listOf(2L),
                vm.uiState.value.devices
                    .map { it.id },
            )
            vm.onFilterSelected(DeviceFilter.CAMERAS)
            assertEquals(
                listOf(1L),
                vm.uiState.value.devices
                    .map { it.id },
            )
            assertEquals(DeviceFilter.CAMERAS, vm.uiState.value.filter)
        }
}

class DeviceFormViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private fun vm(
        repo: FakeDeviceRepository,
        id: Long? = null,
    ) = DeviceFormViewModel(SavedStateHandle(if (id == null) emptyMap() else mapOf(DeviceFormViewModel.DEVICE_ID_ARG to id)), repo)

    @Test
    fun `invalid draft shows errors, keeps them live and does not save`() =
        runTest {
            val repo = FakeDeviceRepository()
            val vm = vm(repo)
            vm.onSave()
            assertEquals(
                setOf(DeviceError.NAME_REQUIRED, DeviceError.ROOM_REQUIRED, DeviceError.ADDRESS_INVALID),
                vm.uiState.value.errors,
            )
            vm.onNameChange("Cam")
            assertFalse(DeviceError.NAME_REQUIRED in vm.uiState.value.errors)
            assertTrue(repo.devices.value.isEmpty())
            assertFalse(vm.uiState.value.isDone)
        }

    @Test
    fun `valid camera is created`() =
        runTest {
            val repo = FakeDeviceRepository()
            val vm = vm(repo)
            vm.onNameChange("Câmera")
            vm.onRoomChange("Sala")
            vm.onKindChange(DeviceKind.CAMERA)
            vm.onAddressChange("rtsp://192.168.0.9/s")
            vm.onSave()
            assertEquals(
                "Câmera",
                repo.devices.value
                    .single()
                    .name,
            )
            assertTrue(vm.uiState.value.isDone)
        }

    @Test
    fun `valid alexa is created`() =
        runTest {
            val repo = FakeDeviceRepository()
            val vm = vm(repo)
            vm.onNameChange("Echo")
            vm.onRoomChange("Quarto")
            vm.onKindChange(DeviceKind.ALEXA)
            vm.onAddressChange("G090LF1234567890")
            vm.onSave()
            assertEquals(
                DeviceKind.ALEXA,
                repo.devices.value
                    .single()
                    .kind,
            )
        }

    @Test
    fun `editing loads the device, updates it and can delete it`() =
        runTest {
            val repo = FakeDeviceRepository(listOf(device(5, name = "Antiga")))
            val vm = vm(repo, 5)
            assertTrue(vm.uiState.value.isEditing)
            assertEquals("Antiga", vm.uiState.value.draft.name)
            vm.onNameChange("Nova")
            vm.onSave()
            assertEquals(
                "Nova",
                repo.devices.value
                    .single()
                    .name,
            )
            assertTrue(vm.uiState.value.isDone)

            val again = vm(repo, 5)
            again.onDelete()
            assertTrue(repo.devices.value.isEmpty())
        }

    @Test
    fun `delete does nothing for a new device and missing device is tolerated`() =
        runTest {
            val vm = vm(FakeDeviceRepository())
            vm.onDelete()
            assertFalse(vm.uiState.value.isDone)
            assertEquals(
                "",
                vm(FakeDeviceRepository(), 99)
                    .uiState.value.draft.name,
            )
        }
}

class CameraViewerViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private fun vm(
        repo: FakeDeviceRepository,
        stream: FakeCameraStream,
        id: Long = 1,
    ) = CameraViewerViewModel(SavedStateHandle(mapOf(DeviceFormViewModel.DEVICE_ID_ARG to id)), repo, stream)

    @Test
    fun `shows frames while the stream is live`() =
        runTest {
            val frame = CameraFrame(1, MutableClock.T0)
            val vm = vm(FakeDeviceRepository(listOf(device())), FakeCameraStream(flowOf(frame)))
            vm.uiState.test {
                var item = awaitItem()
                while (item !is CameraViewerUiState.Live) item = awaitItem()
                assertEquals(frame, item.frame)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `no signal can be retried`() =
        runTest {
            var attempts = 0
            val stream =
                FakeCameraStream(
                    flow {
                        attempts++
                        throw StreamUnavailableException("none")
                    },
                )
            val vm = vm(FakeDeviceRepository(listOf(device())), stream)
            collectInBackground(vm.uiState)
            assertTrue(vm.uiState.value is CameraViewerUiState.NoSignal)
            vm.onRetry()
            assertEquals(2, attempts)
        }

    @Test
    fun `unknown camera is reported`() =
        runTest {
            val vm = vm(FakeDeviceRepository(), FakeCameraStream(flowOf()), id = 42)
            collectInBackground(vm.uiState)
            assertEquals(CameraViewerUiState.NotFound, vm.uiState.value)
        }
}

class GatesViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private fun setup(
        state: GateState = GateState.CLOSED,
    ): Triple<FakeGateRepository, GatesViewModel, com.andrecoura.homemonitor.domain.model.Gate> {
        val g = gate(state = state)
        val repo = FakeGateRepository(listOf(g))
        return Triple(repo, GatesViewModel(repo), g)
    }

    @Test
    fun `nothing is sent before the user confirms`() =
        runTest {
            val (repo, vm, g) = setup()
            collectInBackground(vm.uiState)
            vm.onCommandRequested(g)
            assertEquals(
                GateCommand.OPEN,
                vm.uiState.value.pending
                    ?.command,
            )
            assertTrue(repo.sent.isEmpty())
            vm.onDismissConfirmation()
            assertNull(vm.uiState.value.pending)
            assertTrue(repo.sent.isEmpty())
        }

    @Test
    fun `confirming sends the command and reports success`() =
        runTest {
            val (repo, vm, g) = setup()
            collectInBackground(vm.uiState)
            vm.onCommandRequested(g)
            vm.onConfirm()
            assertEquals(listOf("gate-1" to GateCommand.OPEN), repo.sent)
            assertEquals(GateMessage.COMMAND_SENT, vm.uiState.value.message)
            assertTrue(
                vm.uiState.value.busyGateIds
                    .isEmpty(),
            )
            vm.onMessageShown()
            assertNull(vm.uiState.value.message)
        }

    @Test
    fun `failure is reported`() =
        runTest {
            val (repo, vm, g) = setup(GateState.OPEN)
            repo.result = Result.failure(IllegalStateException())
            collectInBackground(vm.uiState)
            vm.onCommandRequested(g)
            vm.onConfirm()
            assertEquals(GateCommand.CLOSE, repo.sent.single().second)
            assertEquals(GateMessage.COMMAND_FAILED, vm.uiState.value.message)
        }

    @Test
    fun `offline gate cannot be commanded and confirm without request is a no-op`() =
        runTest {
            val (repo, vm, g) = setup(GateState.OFFLINE)
            collectInBackground(vm.uiState)
            vm.onCommandRequested(g)
            vm.onConfirm()
            assertNull(vm.uiState.value.pending)
            assertTrue(repo.sent.isEmpty())
        }
}

class IntercomViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private fun setup(calls: List<com.andrecoura.homemonitor.domain.model.IntercomCall>) =
        Triple(FakeIntercomRepository(calls), FakeGateRepository(listOf(gate())), Unit)

    @Test
    fun `ringing call is the active one and the rest is history`() =
        runTest {
            val (intercom, gates) = setup(listOf(call("new"), call("old", CallState.MISSED))).let { it.first to it.second }
            val vm = IntercomViewModel(intercom, gates)
            collectInBackground(vm.uiState)
            assertEquals(
                "new",
                vm.uiState.value.activeCall
                    ?.id,
            )
            assertEquals(
                listOf("old"),
                vm.uiState.value.history
                    .map { it.id },
            )
        }

    @Test
    fun `answering keeps the call active until it is ended`() =
        runTest {
            val (intercom, gates) = setup(listOf(call("c"))).let { it.first to it.second }
            val vm = IntercomViewModel(intercom, gates)
            collectInBackground(vm.uiState)
            vm.onAnswer(call("c"))
            assertEquals(listOf("c"), intercom.answered)
            assertEquals(
                CallState.ANSWERED,
                vm.uiState.value.activeCall
                    ?.state,
            )
            vm.onEndCall()
            assertNull(vm.uiState.value.activeCall)
        }

    @Test
    fun `failed answer shows a message`() =
        runTest {
            val (intercom, gates) = setup(listOf(call("c"))).let { it.first to it.second }
            intercom.result = Result.failure(IllegalStateException())
            val vm = IntercomViewModel(intercom, gates)
            collectInBackground(vm.uiState)
            vm.onAnswer(call("c"))
            assertEquals(IntercomMessage.ANSWER_FAILED, vm.uiState.value.message)
            vm.onMessageShown()
            assertNull(vm.uiState.value.message)
        }

    @Test
    fun `opening the gate from a call needs confirmation and targets the call's gate`() =
        runTest {
            val (intercom, gates) = setup(listOf(call("c"))).let { it.first to it.second }
            val vm = IntercomViewModel(intercom, gates)
            collectInBackground(vm.uiState)
            vm.onOpenGateRequested(call("c"))
            assertTrue(gates.sent.isEmpty())
            vm.onDismissConfirmation()
            vm.onConfirmOpenGate() // nothing pending anymore
            assertTrue(gates.sent.isEmpty())

            vm.onOpenGateRequested(call("c"))
            vm.onConfirmOpenGate()
            assertEquals(listOf("gate-1" to GateCommand.OPEN), gates.sent)
            assertEquals(IntercomMessage.GATE_OPENING, vm.uiState.value.message)
        }

    @Test
    fun `gate failure from a call is reported`() =
        runTest {
            val (intercom, gates) = setup(listOf(call("c"))).let { it.first to it.second }
            gates.result = Result.failure(IllegalStateException())
            val vm = IntercomViewModel(intercom, gates)
            collectInBackground(vm.uiState)
            vm.onOpenGateRequested(call("c"))
            vm.onConfirmOpenGate()
            assertEquals(IntercomMessage.GATE_FAILED, vm.uiState.value.message)
        }
}

class AlertsViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `exposes alerts newest first from the repository`() =
        runTest {
            val alerts =
                listOf(Alert(2, AlertType.DEVICE_OFFLINE, "Cam", MutableClock.T0), Alert(1, AlertType.MISSED_CALL, "Int", MutableClock.T0))
            val vm = AlertsViewModel(FakeAlertRepository(alerts))
            collectInBackground(vm.uiState)
            assertEquals(alerts, vm.uiState.first { !it.isLoading }.alerts)
        }
}
