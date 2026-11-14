package com.flux.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.navigation.NavRoutes
import com.flux.ui.common.BasicScaffold
import com.flux.ui.events.SettingEvents
import com.flux.ui.screens.events.EventColorSelectionDialog
import com.flux.ui.screens.events.eventColorPresets
import com.flux.ui.state.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventSettings(
    navController: NavController,
    settings: Settings,
    onSettingsEvents: (SettingEvents) -> Unit
) {
    var showCompletedColorDialog by remember { mutableStateOf(false) }
    var showDefaultEventColorDialog by remember { mutableStateOf(false) }

    if (showCompletedColorDialog) {
        EventColorSelectionDialog(
            selectedColorArgb = settings.data.completedEventColorArgb,
            titleRes = R.string.completed_event_color_title,
            onColorSelected = { colorArgb ->
                onSettingsEvents(
                    SettingEvents.UpdateSettings(
                        settings.data.copy(completedEventColorArgb = colorArgb)
                    )
                )
                showCompletedColorDialog = false
            },
            onDismiss = { showCompletedColorDialog = false }
        )
    }

    if (showDefaultEventColorDialog) {
        EventColorSelectionDialog(
            selectedColorArgb = settings.data.defaultNewEventColorArgb,
            titleRes = R.string.default_new_event_color_title,
            onColorSelected = { colorArgb ->
                onSettingsEvents(
                    SettingEvents.UpdateSettings(
                        settings.data.copy(defaultNewEventColorArgb = colorArgb)
                    )
                )
                showDefaultEventColorDialog = false
            },
            onDismiss = { showDefaultEventColorDialog = false }
        )
    }

    BasicScaffold(
        title = stringResource(R.string.event_settings_title),
        onBackClicked = { navController.popBackStack() }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp, 8.dp, 16.dp, 16.dp)
        ) {
            item {
                Spacer(Modifier.height(12.dp))
                val defaultEventColorLabel = eventColorPresets
                    .firstOrNull { it.argb == settings.data.defaultNewEventColorArgb }
                    ?.label ?: R.string.event_color_default
                SettingOption(
                    title = stringResource(R.string.default_new_event_color_title),
                    description = "${stringResource(R.string.default_new_event_color_description)} • ${stringResource(defaultEventColorLabel)}",
                    icon = Icons.Default.Palette,
                    radius = shapeManager(radius = settings.data.cornerRadius, isBoth = true),
                    actionType = ActionType.CUSTOM,
                    onCustomClick = { showDefaultEventColorDialog = true }
                )
            }

            item {
                Spacer(Modifier.height(12.dp))
                SettingOption(
                    title = stringResource(R.string.show_completed_events_title),
                    description = stringResource(R.string.show_completed_events_description),
                    icon = Icons.Default.Event,
                    radius = shapeManager(radius = settings.data.cornerRadius, isBoth = true),
                    actionType = ActionType.SWITCH,
                    variable = settings.data.showCompletedEvents,
                    switchEnabled = {
                        onSettingsEvents(
                            SettingEvents.UpdateSettings(
                                settings.data.copy(showCompletedEvents = it)
                            )
                        )
                    }
                )
            }

            item {
                Spacer(Modifier.height(12.dp))
                val completedColorLabel = eventColorPresets
                    .firstOrNull { it.argb == settings.data.completedEventColorArgb }
                    ?.label ?: R.string.event_color_default
                SettingOption(
                    title = stringResource(R.string.completed_event_color_title),
                    description = "${stringResource(R.string.completed_event_color_description)} • ${stringResource(completedColorLabel)}",
                    icon = Icons.Default.Palette,
                    radius = shapeManager(radius = settings.data.cornerRadius, isBoth = true),
                    actionType = ActionType.CUSTOM,
                    onCustomClick = { showCompletedColorDialog = true }
                )
            }
        }
    }
}
