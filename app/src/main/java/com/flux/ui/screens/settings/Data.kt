package com.flux.ui.screens.settings

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.work.WorkManager
import com.flux.R
import com.flux.other.BackupFrequency
import com.flux.other.BackupManager
import com.flux.other.canScheduleReminder
import com.flux.other.isNotificationPermissionGranted
import com.flux.other.openAppNotificationSettings
import com.flux.other.requestExactAlarmPermission
import com.flux.ui.components.ActionType
import com.flux.ui.components.DeleteAlert
import com.flux.ui.components.SettingOption
import com.flux.ui.components.shapeManager
import com.flux.ui.events.SettingEvents
import com.flux.ui.state.Settings
import com.flux.ui.viewModel.BackupViewModel

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Data(
    navController: NavController,
    radius: Int,
    settings: Settings,
    snackbarHostState: SnackbarHostState,
    backupViewModel: BackupViewModel,
    onSettingsEvents: (SettingEvents) -> Unit
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    var showWarningDialog by remember { mutableStateOf(false) }

    // EXPORT launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { backupViewModel.exportBackup(context, it) }
    }

    // IMPORT launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { backupViewModel.importBackup(context, it) }
    }

    // Observe result - Collect SharedFlow properly
    LaunchedEffect(Unit) {
        backupViewModel.backupResult.collect { result ->
            if (result.isSuccess) {
                Toast.makeText(context, "Operation successful!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Operation failed.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ... rest of your UI
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                title = { Text("Data") },
                navigationIcon = {
                    IconButton({navController.navigateUp()}) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ){ innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                SettingOption(
                    title = stringResource(R.string.Backup),
                    description = stringResource(R.string.Backup_Description),
                    icon = Icons.Rounded.Backup,
                    radius = shapeManager(radius = radius, isFirst = true),
                    actionType = ActionType.CUSTOM,
                    onCustomClick = { exportLauncher.launch("flux-backup.json") }
                )
            }

            item {
                SettingOption(
                    title = stringResource(R.string.Restore),
                    description = stringResource(R.string.Restore_Description),
                    icon = Icons.Rounded.Restore,
                    radius = shapeManager(radius = radius, isLast = true),
                    actionType = ActionType.CUSTOM,
                    onCustomClick = {
                        if (!canScheduleReminder(context)) {
                            Toast.makeText(
                                context,
                                context.getText(R.string.Reminder_Permission),
                                Toast.LENGTH_SHORT
                            ).show()
                            requestExactAlarmPermission(context)
                        }
                        if (!isNotificationPermissionGranted(context)) {
                            Toast.makeText(
                                context,
                                context.getText(R.string.Notification_Permission),
                                Toast.LENGTH_SHORT
                            ).show()
                            openAppNotificationSettings(context)
                        }
                        if (canScheduleReminder(context) && isNotificationPermissionGranted(context)) {
                            importLauncher.launch(arrayOf("application/json"))
                        }
                    }
                )
            }

            item {
                val workManager = remember { WorkManager.getInstance(context.applicationContext) }
                val backupManager = remember(workManager) { BackupManager(workManager) }

                fun mapDaysToSliderPosition(days: Int): Float = when (days) {
                    0 -> 0f
                    1 -> 1f
                    7 -> 2f
                    30 -> 3f
                    else -> 0f
                }

                fun mapSliderPositionToFrequency(position: Float): BackupFrequency = when (position) {
                    0f -> BackupFrequency.NEVER
                    1f -> BackupFrequency.DAILY
                    2f -> BackupFrequency.WEEKLY
                    3f -> BackupFrequency.MONTHLY
                    else -> BackupFrequency.NEVER
                }

                val sliderPosition = remember(settings.data.backupFrequency) {
                    mapDaysToSliderPosition(settings.data.backupFrequency)
                }

                val selectedFrequency = remember(sliderPosition) {
                    mapSliderPositionToFrequency(sliderPosition)
                }

                ListItem(
                    modifier = Modifier.padding(top = 8.dp),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.EditCalendar,
                            contentDescription = "Auto backup"
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    headlineContent = { Text(text = "Frequency of automatic backup") },
                    supportingContent = { Text(text = stringResource(selectedFrequency.textRes)) }
                )

                Slider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    value = sliderPosition,
                    onValueChange = { newPosition ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                        val newFrequency = mapSliderPositionToFrequency(newPosition)
                        onSettingsEvents(SettingEvents.UpdateSettings(settings.data.copy(backupFrequency = newFrequency.days)))
                    },
                    onValueChangeFinished = { backupManager.scheduleBackup(selectedFrequency) },
                    valueRange = 0f..3f,
                    steps = 2
                )
            }
            item {
                ListItem(
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.CleaningServices,
                            contentDescription = "Reset"
                        )
                    },
                    headlineContent = { Text(text = "Reset Database") },
                    trailingContent = {
                        TextButton(
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                showWarningDialog = true
                            },
                            colors = ButtonDefaults.textButtonColors().copy(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text(text = "Reset")
                        }
                    },
                    supportingContent = {
                        Text(
                            text = "Clear all app data"
                        )
                    }
                )
            }
        }
        if(showWarningDialog){
            DeleteAlert(
                onConfirmation = {
                    showWarningDialog=false
                    onSettingsEvents(SettingEvents.ResetDatabase) },
                onDismissRequest = { showWarningDialog = false },
                dialogTitle = stringResource(R.string.deleteDialogTitle),
                dialogText = stringResource(R.string.deleteDialogText),
                icon = Icons.Default.Delete
            )
        }
    }
}

