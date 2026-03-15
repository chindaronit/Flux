package com.flux.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.ViewCompact
import androidx.compose.material.icons.rounded.ViewCompactAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.flux.R
import com.flux.navigation.NavRoutes
import com.flux.ui.components.ActionType
import com.flux.ui.components.BasicScaffold
import com.flux.ui.components.FontDialog
import com.flux.ui.components.SettingIcon
import com.flux.ui.components.SettingOption
import com.flux.ui.components.SingleSettingOption
import com.flux.ui.components.shapeManager
import com.flux.ui.events.SettingEvents
import com.flux.ui.state.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Customize(
    navController: NavController,
    settings: Settings,
    onSettingsEvents: (SettingEvents) -> Unit
) {
    var showRadiusDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }

    if (showFontDialog){
        FontDialog(settings.data.fontNumber, onSelectFont = {
            onSettingsEvents(SettingEvents.UpdateSettings(
                settings.data.copy(
                    fontNumber = it
                )
            ))
        }) {
            showFontDialog=false
        }
    }

    if(showRadiusDialog){
        OnRadiusClicked(settings) {
            onSettingsEvents(
                SettingEvents.UpdateSettings(
                    settings.data.copy(
                        cornerRadius = it
                    )
                )
            )
            showRadiusDialog=false
        }
    }

    BasicScaffold(
        title = stringResource(R.string.Customize),
        onBackClicked = { navController.popBackStack() }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp, 8.dp, 16.dp, 16.dp)
        ) {
            item {
                SingleSettingOption(
                    radius = settings.data.cornerRadius,
                    text = stringResource(R.string.Themes),
                    description = "Change app theme",
                    leadingIcon = SettingIcon.Vector(Icons.Default.LightMode)
                ) {
                    navController.navigate(NavRoutes.Theme.route)
                }
            }

            item {
                val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S // Android 12+

                if (supportsDynamicColor) {
                    Spacer(Modifier.height(12.dp))

                    SettingOption(
                        title = stringResource(R.string.Dynamic_theme),
                        description = stringResource(R.string.Dynamic_theme_desc),
                        icon = Icons.Filled.Colorize,
                        radius = shapeManager(radius = settings.data.cornerRadius, isBoth = true),
                        actionType = ActionType.SWITCH,
                        variable = settings.data.dynamicTheme,
                        switchEnabled = {
                            onSettingsEvents(
                                SettingEvents.UpdateSettings(
                                    settings.data.copy(
                                        dynamicTheme = it
                                    )
                                )
                            )
                        },
                    )
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                SettingOption(
                    title = stringResource(R.string.Radius),
                    description = stringResource(R.string.Radius_desc),
                    icon = Icons.Rounded.RoundedCorner,
                    radius = shapeManager(
                        radius = settings.data.cornerRadius,
                        isFirst = true
                    ),
                    actionType = ActionType.CUSTOM,
                    onCustomClick = { showRadiusDialog=true }
                )
            }
            item {
                SettingOption(
                    title = stringResource(R.string.Compact_Mode),
                    description = stringResource(R.string.Compact_Mode_Desc),
                    icon = Icons.Rounded.ViewCompactAlt,
                    radius = shapeManager(radius = settings.data.cornerRadius),
                    variable = settings.data.workspaceGridColumns > 1,
                    actionType = ActionType.SWITCH,
                    switchEnabled = {
                        if (it) {
                            onSettingsEvents(
                                SettingEvents.UpdateSettings(
                                    settings.data.copy(
                                        workspaceGridColumns = 2
                                    )
                                )
                            )
                        } else {
                            onSettingsEvents(
                                SettingEvents.UpdateSettings(
                                    settings.data.copy(
                                        workspaceGridColumns = 1
                                    )
                                )
                            )
                        }
                    }
                )
            }
            item {
                val isEnabled = settings.data.workspaceGridColumns > 1
                SettingOption(
                    title = stringResource(R.string.Extreme_Compact_Mode),
                    description = stringResource(R.string.Extreme_Compact_Mode_Desc),
                    icon = Icons.Rounded.ViewCompact,
                    isEnabled = isEnabled,
                    radius = shapeManager(radius = settings.data.cornerRadius),
                    variable = settings.data.workspaceGridColumns == 3,
                    actionType = ActionType.SWITCH,
                    switchEnabled = {
                        if (it) {
                            onSettingsEvents(
                                SettingEvents.UpdateSettings(
                                    settings.data.copy(
                                        workspaceGridColumns = 3
                                    )
                                )
                            )
                        } else {
                            onSettingsEvents(
                                SettingEvents.UpdateSettings(
                                    settings.data.copy(
                                        workspaceGridColumns = 2
                                    )
                                )
                            )
                        }
                    }
                )
            }

            item {
                SettingOption(
                    title = stringResource(R.string.Hour_Format_24),
                    description = stringResource(R.string.Hour_Format_24_Desc),
                    icon = Icons.Filled.AccessTime,
                    radius = shapeManager(
                        radius = settings.data.cornerRadius,
                        isLast = true
                    ),
                    actionType = ActionType.SWITCH,
                    variable = settings.data.is24HourFormat,
                    switchEnabled = {
                        onSettingsEvents(
                            SettingEvents.UpdateSettings(
                                settings.data.copy(
                                    is24HourFormat = it
                                )
                            )
                        )
                    }
                )
            }

            item {
                Spacer(Modifier.height(12.dp))
                SettingOption(
                    title = stringResource(R.string.Font),
                    description = stringResource(R.string.Change_Font),
                    icon = Icons.Rounded.FontDownload,
                    radius = shapeManager(
                        radius = settings.data.cornerRadius,
                        isBoth = true
                    ),
                    actionType = ActionType.CUSTOM,
                    onCustomClick = { showFontDialog = true }
                )
            }
        }
    }
}

@Composable
fun OnRadiusClicked(settings: Settings, onExit: (Int) -> Unit) {
    val minimalRadius = 5
    val settingsRadius = settings.data.cornerRadius
    var sliderPosition by remember { mutableFloatStateOf(((settingsRadius - minimalRadius).toFloat() / 30)) }
    val realRadius: Int = (((sliderPosition * 100).toInt()) / 3) + minimalRadius

    @Composable
    fun example(shape: RoundedCornerShape) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp, 3.dp, 32.dp, 1.dp)
                .background(
                    shape = shape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                )
                .height(62.dp),
        )
    }
    Dialog(onDismissRequest = { onExit(realRadius) }) {
        Column(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(realRadius / 3)
                )
                .fillMaxWidth()
                .fillMaxSize(0.38f)
        ) {
            Text(
                text = stringResource(R.string.Select_radius),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp)
            )
            example(shapeManager(radius = realRadius, isFirst = true))
            example(shapeManager(radius = realRadius))
            example(shapeManager(radius = realRadius, isLast = true))
            Slider(
                value = sliderPosition,
                modifier = Modifier.padding(32.dp, 16.dp, 32.dp, 16.dp),
                colors = SliderDefaults.colors(inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                onValueChange = { newValue -> sliderPosition = newValue }
            )
        }
    }
}