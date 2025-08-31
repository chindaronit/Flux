package com.flux.ui.screens.settings.attention_manager

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.navigation.NavRoutes
import com.flux.ui.components.ActionType
import com.flux.ui.components.BasicScaffold
import com.flux.ui.components.SettingOption
import com.flux.ui.components.shapeManager
import com.flux.ui.events.SettingEvents
import com.flux.ui.state.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttentionManager(
    navController: NavController,
    settings: Settings,
    onSettingsEvents: (SettingEvents) -> Unit
) {
    val data = settings.data

    BasicScaffold(
        title = stringResource(
            R.string.Attention_Manager
        ),
        onBackClicked = { navController.popBackStack() }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                SettingOption(
                    title = "App Blocking",
                    description = "Blocks apps from launching",
                    icon = Icons.Filled.Block,
                    radius = shapeManager(radius = data.cornerRadius, isBoth = true),
                    actionType = ActionType.SWITCH,
                    variable = data.isAttentionManagerEnabled,
                    switchEnabled = {
                        onSettingsEvents(
                            SettingEvents.UpdateSettings(
                                data.copy(
                                    isAttentionManagerEnabled = it
                                )
                            )
                        )
                    }
                )
            }

            item {
                SettingOption(
                    title = "Blocked Apps",
                    description = "Set blocked apps list",
                    icon = Icons.Filled.Check,
                    radius = shapeManager(radius = data.cornerRadius, isBoth = true),
                    actionType = ActionType.CUSTOM,
                    customButton = {
                        navController.navigate(NavRoutes.AttentionManagerAppPicker.route)
                    }
                )
                Spacer(Modifier.height(16.dp))
            }
            // TODO: whitelist/blacklist?
        }
    }
}
