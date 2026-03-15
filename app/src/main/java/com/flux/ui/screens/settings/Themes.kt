package com.flux.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.flux.R
import com.flux.ui.components.BasicScaffold
import com.flux.ui.components.SelectableColorPlatte
import com.flux.ui.events.SettingEvents
import com.flux.ui.state.Settings
import com.flux.ui.theme.lightSchemes

@Composable
fun Themes(
    navController: NavController,
    settings: Settings,
    onSettingsEvents: (SettingEvents) -> Unit
){

    val options = listOf(
        stringResource(R.string.Low),
        stringResource(R.string.Medium),
        stringResource(R.string.High)
    )

    BasicScaffold(
        title = stringResource(R.string.Themes),
        onBackClicked = { navController.popBackStack() }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp, 8.dp, 16.dp, 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            item {
                OutlinedCard(modifier = Modifier.height(400.dp).width(210.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(50),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {}

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {}

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(120.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {}
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(120.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {}
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End)
                        {
                            Card(modifier = Modifier
                                .width(40.dp)
                                .height(40.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)){}
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            stringResource(R.string.Themes),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        LazyRow(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                IconButton({
                                        onSettingsEvents(
                                            SettingEvents.UpdateSettings(
                                                settings.data.copy(
                                                    isAutomaticTheme = true,
                                                    isDarkMode = false,
                                                    dynamicTheme = false,
                                                    amoledTheme = false
                                                )
                                            )
                                        )
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    val icon = when {
                                        settings.data.isAutomaticTheme -> Icons.Default.Check
                                        else -> Icons.Default.Settings
                                    }
                                    Icon(icon, null)
                                }
                            }

                            item {
                                VerticalDivider(
                                    modifier = Modifier.height(48.dp),
                                    thickness = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            item {
                                IconButton({
                                    onSettingsEvents(
                                        SettingEvents.UpdateSettings(
                                            settings.data.copy(
                                                isAutomaticTheme = false,
                                                isDarkMode = false,
                                                amoledTheme = false
                                            )
                                        )
                                    )
                                },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    ),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    val icon = when {
                                        !settings.data.isAutomaticTheme && !settings.data.isDarkMode -> Icons.Default.Check
                                        else -> Icons.Default.LightMode
                                    }
                                    Icon(icon, null)
                                }
                            }

                            item {
                                IconButton({
                                    onSettingsEvents(
                                        SettingEvents.UpdateSettings(
                                            settings.data.copy(
                                                isAutomaticTheme = false,
                                                isDarkMode = true,
                                                amoledTheme = false
                                            )
                                        )
                                    )
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color.Black.copy(0.8f),
                                        contentColor = Color.White.copy(0.8f)
                                    ),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    val icon = when {
                                        !settings.data.isAutomaticTheme && settings.data.isDarkMode && !settings.data.amoledTheme-> Icons.Default.Check
                                        else -> Icons.Default.DarkMode
                                    }
                                    Icon(icon, null)
                                }
                            }

                            if(settings.data.isDarkMode && !settings.data.isAutomaticTheme) {
                                item {
                                    IconButton({
                                        onSettingsEvents(
                                            SettingEvents.UpdateSettings(
                                                settings.data.copy(
                                                    amoledTheme = true
                                                )
                                            )
                                        )
                                    },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = Color.Black,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        val icon = when {
                                            settings.data.amoledTheme -> Icons.Default.Check
                                            else -> Icons.Default.BatterySaver
                                        }
                                        Icon(icon, null)
                                    }
                                }
                            }
                        }
                        Text(
                            "Color Palette",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        LazyRow(Modifier.fillMaxWidth()) {
                            itemsIndexed(lightSchemes){ index, item->
                                SelectableColorPlatte(
                                    selected = index==settings.data.themeNumber,
                                    colorScheme = item
                                ) {
                                    onSettingsEvents(SettingEvents.UpdateSettings(settings.data.copy(themeNumber = index)))
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        AnimatedVisibility(visible = !settings.data.dynamicTheme) {
                            Text(
                                stringResource(R.string.Contrast),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        AnimatedVisibility(visible = !settings.data.dynamicTheme) {
                            SingleChoiceSegmentedButtonRow {
                                options.forEachIndexed { index, label ->
                                    SegmentedButton(
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = options.size
                                        ),
                                        onClick = {
                                            onSettingsEvents(
                                                SettingEvents.UpdateSettings(
                                                    settings.data.copy(
                                                        contrast = index
                                                    )
                                                )
                                            )
                                        },
                                        selected = index == settings.data.contrast,
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}