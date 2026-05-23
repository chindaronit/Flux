package com.flux.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.ui.common.BasicScaffold
import com.flux.ui.events.SettingEvents
import com.flux.ui.state.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Privacy(
    navController: NavController,
    settings: Settings,
    onSettingsEvents: (SettingEvents) -> Unit
) {
    val data = settings.data

    BasicScaffold(
        title = stringResource(R.string.Privacy),
        onBackClicked = { navController.popBackStack() }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                SettingOption(
                    title = stringResource(R.string.Screen_Protection),
                    description = stringResource(R.string.Screen_Protection_Desc),
                    icon = Icons.Filled.RemoveRedEye,
                    radius = shapeManager(radius = data.cornerRadius, isFirst = true),
                    actionType = ActionType.SWITCH,
                    variable = data.isScreenProtection,
                    switchEnabled = {
                        onSettingsEvents(SettingEvents.UpdateSettings(data.copy(isScreenProtection = it)))
                    }
                )
            }
            item {
                SettingOption(
                    title = stringResource(R.string.App_Lock),
                    description = stringResource(R.string.App_Lock_desc),
                    icon = Icons.Filled.Fingerprint,
                    radius = shapeManager(radius = data.cornerRadius, isLast = true),
                    actionType = ActionType.SWITCH,
                    variable = data.isBiometricEnabled,
                    switchEnabled = {
                        onSettingsEvents(SettingEvents.UpdateSettings(data.copy(isBiometricEnabled = it)))
                    }
                )
                Spacer(Modifier.height(16.dp))
            }
//            item {
//                SettingOption(
//                    title = stringResource(R.string.Encrypt),
//                    description = stringResource(R.string.Encrypt_Desc),
//                    icon = Icons.Filled.EnhancedEncryption,
//                    radius = shapeManager(radius = data.cornerRadius, isBoth = true),
//                    actionType = ActionType.SWITCH,
//                    variable = false,
//                    switchEnabled = {
//                        onSettingsEvents(SettingEvents.UpdateSettings(data.copy(isBiometricEnabled = it)))
//                    }
//                )
//            }
        }
    }
}