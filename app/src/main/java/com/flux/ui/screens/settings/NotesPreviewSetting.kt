package com.flux.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.flux.R
import com.flux.ui.common.BasicScaffold
import com.flux.ui.events.SettingEvents
import com.flux.ui.state.Settings

@Composable
fun NotesPreviewSetting(
    navController: NavController,
    settings: Settings,
    onSettingsEvents: (SettingEvents) -> Unit
){
    val radius = settings.data.cornerRadius
    val notesPreviewMode = settings.data.notesPreviewMode
    val maxHeight = when (notesPreviewMode) {
        0 -> 0.dp
        1 -> 180.dp
        else -> 360.dp
    }

    BasicScaffold(
        title = stringResource(R.string.notes_preview),
        onBackClicked = { navController.popBackStack() }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 12.dp,
                    bottom = 140.dp
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                        ),
                        modifier = Modifier.clip(
                            shapeManager(isBoth = true, radius = radius / 2)
                        ),
                        shape = shapeManager(isBoth = true, radius = radius / 2),
                        onClick = {}
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .heightIn(max = if (maxHeight != 0.dp) maxHeight else 100.dp)
                        ) {
                            Text(
                                stringResource(R.string.Title),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            if (maxHeight != 0.dp) {
                                Text(NOTE1, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                        ),
                        modifier = Modifier.clip(
                            shapeManager(isBoth = true, radius = radius / 2)
                        ),
                        shape = shapeManager(isBoth = true, radius = radius / 2),
                        onClick = {}
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .heightIn(max = if (maxHeight != 0.dp) maxHeight else 100.dp)
                        ) {
                            Text(
                                stringResource(R.string.Title),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            if (maxHeight != 0.dp) {
                                Text(NOTE2, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                        ),
                        modifier = Modifier.clip(
                            shapeManager(isBoth = true, radius = radius / 2)
                        ),
                        shape = shapeManager(isBoth = true, radius = radius / 2),
                        onClick = {}
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .heightIn(max = if (maxHeight != 0.dp) maxHeight else 100.dp)
                        ) {
                            Text(
                                stringResource(R.string.Title),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            if (maxHeight != 0.dp) {
                                Text(NOTE3, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                        ),
                        modifier = Modifier.clip(
                            shapeManager(isBoth = true, radius = radius / 2)
                        ),
                        shape = shapeManager(isBoth = true, radius = radius / 2),
                        onClick = {}
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .heightIn(max = if (maxHeight != 0.dp) maxHeight else 100.dp)
                        ) {
                            Text(
                                stringResource(R.string.Title),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            if (maxHeight != 0.dp) {
                                Text(NOTE4, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        stringResource(R.string.change_height),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(0, 3),
                            onClick = { onSettingsEvents(SettingEvents.UpdateSettings(settings.data.copy(notesPreviewMode = 0))) },
                            selected = notesPreviewMode == 0,
                            label = { Text(stringResource(R.string.compact)) }
                        )

                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(1, 3),
                            onClick = { onSettingsEvents(SettingEvents.UpdateSettings(settings.data.copy(notesPreviewMode = 1))) },
                            selected = notesPreviewMode == 1,
                            label = { Text(stringResource(R.string.normal)) }
                        )

                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(2, 3),
                            onClick = { onSettingsEvents(SettingEvents.UpdateSettings(settings.data.copy(notesPreviewMode = 2))) },
                            selected = notesPreviewMode == 2,
                            label = { Text(stringResource(R.string.elongated)) }
                        )
                    }
                }
            }
        }
    }
}

const val NOTE1 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi eu sapien sagittis elit tincidunt feugiat. Interdum et malesuada fames ac ante ipsum primis in faucibus. Nullam ut nibh eu dolor maximus gravida. Integer non dapibus sem. Nullam nec lectus metus. Nullam vitae fermentum ipsum. Interdum et malesuada fames ac ante ipsum primis in faucibus."
const val NOTE2 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
const val NOTE3 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi eu sapien sagittis elit tincidunt feugiat. Interdum et malesuada fames ac ante ipsum primis in faucibus."
const val NOTE4 = "Lorem ipsum dolor sit amet"