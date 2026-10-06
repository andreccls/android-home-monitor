package com.andrecoura.homemonitor.ui.devices

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrecoura.homemonitor.R
import com.andrecoura.homemonitor.domain.model.CameraFrame
import com.andrecoura.homemonitor.domain.model.Device
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CameraViewerRoute() {
    val viewModel: CameraViewerViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CameraViewerScreen(state, viewModel::onRetry)
}

@Composable
fun CameraViewerScreen(
    state: CameraViewerUiState,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when (state) {
            CameraViewerUiState.Connecting -> {
                val connecting = stringResource(R.string.camera_connecting)
                Placeholder {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.semantics { contentDescription = connecting })
                }
            }

            is CameraViewerUiState.Live -> {
                Text(state.device.name, style = MaterialTheme.typography.titleLarge)
                SimulatedFrame(state.device, state.frame)
            }

            is CameraViewerUiState.NoSignal -> {
                Text(state.device.name, style = MaterialTheme.typography.titleLarge)
                Placeholder {
                    Text(stringResource(R.string.camera_no_signal), color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
                Text(stringResource(R.string.camera_no_signal_hint), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onRetry) { Text(stringResource(R.string.camera_retry)) }
            }

            CameraViewerUiState.NotFound -> {
                Text(stringResource(R.string.camera_not_found))
            }
        }
    }
}

@Composable
private fun Placeholder(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(FRAME_RATIO).background(Color(0xFF111B1D)),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** There is no video: each "frame" is a generated picture whose moving block is driven by the frame counter. */
@Composable
private fun SimulatedFrame(
    device: Device,
    frame: CameraFrame,
) {
    val description = stringResource(R.string.camera_frame_description, device.name, frame.sequence)
    val clock = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault()).format(frame.capturedAt)
    Box(Modifier.fillMaxWidth().aspectRatio(FRAME_RATIO).semantics { contentDescription = description }) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF1E3A44), Color(0xFF0B1316))))
            val floorY = size.height * FLOOR_FRACTION
            drawRect(Color(0xFF16282D), topLeft = Offset(0f, floorY), size = Size(size.width, size.height - floorY))
            // A "person" walking back and forth, one step per frame.
            val travel = size.width * WALK_FRACTION
            val phase = (frame.sequence % WALK_PERIOD).toFloat() / WALK_PERIOD
            val x = size.width * WALK_START + travel * (if (phase < HALF) phase * 2 else (1 - phase) * 2)
            drawRect(
                Color(0xFFBEE6F0),
                topLeft = Offset(x, floorY - size.height * PERSON_HEIGHT),
                size =
                    Size(
                        size.width * PERSON_WIDTH,
                        size.height * PERSON_HEIGHT,
                    ),
            )
            val scanY = size.height * ((frame.sequence % SCAN_PERIOD).toFloat() / SCAN_PERIOD)
            drawRect(Color.White.copy(alpha = SCAN_ALPHA), topLeft = Offset(0f, scanY), size = Size(size.width, 2f))
        }
        Text(
            stringResource(R.string.camera_simulated) + " · " + clock,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Start,
            modifier =
                Modifier
                    .align(
                        Alignment.TopStart,
                    ).padding(12.dp)
                    .background(Color(0x99000000))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

private const val FRAME_RATIO = 16f / 9f
private const val FLOOR_FRACTION = 0.72f
private const val WALK_START = 0.1f
private const val WALK_FRACTION = 0.7f
private const val WALK_PERIOD = 60
private const val HALF = 0.5f
private const val PERSON_HEIGHT = 0.4f
private const val PERSON_WIDTH = 0.07f
private const val SCAN_PERIOD = 30
private const val SCAN_ALPHA = 0.08f
