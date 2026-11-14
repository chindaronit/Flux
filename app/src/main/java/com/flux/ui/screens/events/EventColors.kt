package com.flux.ui.screens.events

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flux.R

data class EventColorPreset(
    @StringRes val label: Int,
    val argb: Int?
)

val eventColorPresets = listOf(
    EventColorPreset(R.string.event_color_default, null),
    EventColorPreset(R.string.event_color_red, 0xFFE57373.toInt()),
    EventColorPreset(R.string.event_color_orange, 0xFFFFB74D.toInt()),
    EventColorPreset(R.string.event_color_yellow, 0xFFFFF176.toInt()),
    EventColorPreset(R.string.event_color_green, 0xFFA5D6A7.toInt()),
    EventColorPreset(R.string.event_color_teal, 0xFF80CBC4.toInt()),
    EventColorPreset(R.string.event_color_blue, 0xFF90CAF9.toInt()),
    EventColorPreset(R.string.event_color_purple, 0xFFB39DDB.toInt()),
    EventColorPreset(R.string.event_color_pink, 0xFFF48FB1.toInt()),
    EventColorPreset(R.string.event_color_gray, 0xFFB0BEC5.toInt())
)

@Composable
fun EventColorSelectionDialog(
    selectedColorArgb: Int?,
    @StringRes titleRes: Int = R.string.event_color_title,
    onColorSelected: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            androidx.compose.foundation.layout.Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                eventColorPresets.forEach { preset ->
                    val isSelected = preset.argb == selectedColorArgb
                    TextButton(
                        onClick = { onColorSelected(preset.argb) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            val swatch = preset.argb?.let(::Color)
                                ?: MaterialTheme.colorScheme.surfaceVariant
                            Spacer(
                                Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        CircleShape
                                    )
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                stringResource(preset.label),
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.Cancel))
            }
        }
    )
}

@Composable
fun EventColorScopeDialog(
    onThisEvent: () -> Unit,
    onSeries: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.event_color_scope_title)) },
        text = { Text(stringResource(R.string.event_color_scope_message)) },
        confirmButton = {
            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onThisEvent) {
                    Text(stringResource(R.string.event_scope_this))
                }
                TextButton(onClick = onSeries) {
                    Text(stringResource(R.string.event_scope_series))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.Cancel))
                }
            }
        }
    )
}

fun eventTextColor(backgroundArgb: Int): Color =
    if (Color(backgroundArgb).luminance() > 0.179f) Color(0xFF1B1B1B) else Color.White
