package com.flux.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ChromeReaderMode
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.flux.R
import com.flux.ui.components.ActionType
import com.flux.ui.components.BasicScaffold
import com.flux.ui.components.SettingOption
import com.flux.ui.components.shapeManager
import com.flux.ui.events.SettingEvents
import com.flux.ui.state.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Editor(
    navController: NavController,
    settings: Settings,
    onSettingsEvents: (SettingEvents) -> Unit
) {
    BasicScaffold(
        title = stringResource(R.string.editor_title),
        onBackClicked = { navController.popBackStack() }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp, 8.dp, 16.dp, 16.dp)
        ) {

            item {
                SettingOption(
                    title = stringResource(R.string.show_line_numbers_title),
                    description = stringResource(R.string.show_line_numbers_description),
                    icon = Icons.Filled.FormatListNumbered,
                    radius = shapeManager(radius = settings.data.cornerRadius, isFirst = true),
                    actionType = ActionType.SWITCH,
                    variable = settings.data.isLineNumbersVisible,
                    switchEnabled = {
                        onSettingsEvents(
                            SettingEvents.UpdateSettings(
                                settings.data.copy(
                                    isLineNumbersVisible = it
                                )
                            )
                        )
                    }
                )
            }

            item {
                SettingOption(
                    title = stringResource(R.string.markdown_lint_title),
                    description = stringResource(R.string.markdown_lint_description),
                    icon = Icons.Filled.Spellcheck,
                    radius = shapeManager(radius = settings.data.cornerRadius, isLast = true),
                    actionType = ActionType.SWITCH,
                    variable = settings.data.isLintValid,
                    switchEnabled = {
                        onSettingsEvents(
                            SettingEvents.UpdateSettings(
                                settings.data.copy(
                                    isLintValid = it
                                )
                            )
                        )
                    }
                )
            }

            item {
                Spacer(Modifier.height(12.dp))
                AnimatedVisibility(visible = !settings.data.dynamicTheme) {
                    Text(
                        stringResource(R.string.default_editor_view),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(Modifier.height(12.dp))

                AnimatedVisibility(visible = !settings.data.dynamicTheme) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = settings.data.startWithReadView,
                            onClick = {
                                onSettingsEvents(
                                    SettingEvents.UpdateSettings(
                                        settings.data.copy(startWithReadView = true)
                                    )
                                )
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = 0,
                                count = 2
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ChromeReaderMode, null)
                                Text(stringResource(R.string.reading_view))
                            }
                        }

                        SegmentedButton(
                            selected = !settings.data.startWithReadView,
                            onClick = {
                                onSettingsEvents(
                                    SettingEvents.UpdateSettings(
                                        settings.data.copy(startWithReadView = false)
                                    )
                                )
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = 1,
                                count = 2
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.EditNote, null)
                                Text(stringResource(R.string.editing_view))
                            }
                        }
                    }
                }
            }
        }
    }
}
