package com.andrecoura.homemonitor.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.andrecoura.homemonitor.R
import com.andrecoura.homemonitor.domain.model.Alert
import com.andrecoura.homemonitor.domain.model.AlertType
import com.andrecoura.homemonitor.domain.model.ConnectionStatus
import com.andrecoura.homemonitor.domain.model.GateState

/** A colored pill with text. The label always carries the meaning; color only reinforces it. */
@Composable
fun StatusChip(
    text: String,
    tone: Tone,
    modifier: Modifier = Modifier,
) {
    val (container, content) =
        when (tone) {
            Tone.Good -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
            Tone.Warning -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
            Tone.Bad -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
            Tone.Neutral -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        }
    Surface(color = container, contentColor = content, shape = CircleShape, modifier = modifier) {
        Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
    }
}

enum class Tone { Good, Warning, Bad, Neutral }

fun ConnectionStatus.tone() = if (this == ConnectionStatus.ONLINE) Tone.Good else Tone.Bad

fun GateState.tone() =
    when (this) {
        GateState.CLOSED -> Tone.Good
        GateState.OPENING, GateState.CLOSING -> Tone.Warning
        GateState.OPEN -> Tone.Warning
        GateState.OFFLINE -> Tone.Bad
    }

fun AlertType.icon(): ImageVector =
    when (this) {
        AlertType.GATE_LEFT_OPEN, AlertType.DEVICE_OFFLINE -> Icons.Filled.Warning
        AlertType.MISSED_CALL -> Icons.Filled.Phone
    }

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier.padding(top = 8.dp))
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) { Box(Modifier.padding(16.dp)) { content() } }
}

@Composable
fun EmptyState(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
}

@Composable
fun AlertRow(
    alert: Alert,
    modifier: Modifier = Modifier,
) {
    val text = alert.type.text(alert.subject)
    InfoCard(modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(alert.type.icon(), MaterialTheme.colorScheme.tertiary)
            Column(Modifier.padding(start = 16.dp)) {
                Text(text, style = MaterialTheme.typography.bodyLarge)
                Text(relativeTime(alert.createdAt), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
