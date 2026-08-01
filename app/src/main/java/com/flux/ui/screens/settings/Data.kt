package com.flux.ui.screens.settings

import android.net.Uri
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
import androidx.compose.material.icons.rounded.Lock
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.flux.ui.common.AutoBackupNeedsPasswordDialog
import com.flux.ui.common.BackupPasswordEntryDialog
import com.flux.ui.common.ChangePasswordWarningDialog
import com.flux.ui.common.DeleteAlert
import com.flux.ui.events.SettingEvents
import com.flux.ui.state.Settings
import com.flux.ui.viewModel.BackupSettingsViewModel
import com.flux.ui.viewModel.BackupViewModel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Data(
    navController: NavController,
    radius: Int,
    settings: Settings,
    snackbarHostState: SnackbarHostState,
    backupViewModel: BackupViewModel,
    backupSettingsViewModel: BackupSettingsViewModel,
    onSettingsEvents: (SettingEvents) -> Unit
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val operationSuccessful = stringResource(R.string.success)
    val operationFailed = stringResource(R.string.Failed)

    var showWarningDialog by remember { mutableStateOf(false) }
    var showAutoBackupNeedsPasswordDialog by remember { mutableStateOf(false) }
    var showSetPasswordDialog by remember { mutableStateOf(false) }
    var showChangePasswordWarning by remember { mutableStateOf(false) }
    var showChangePasswordEntry by remember { mutableStateOf(false) }
    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var pendingBackupFrequency by remember {
        mutableStateOf<BackupFrequency?>(null)
    }
    val hasPassword by backupSettingsViewModel.hasBackupPassword.collectAsState()

    // --- Migration / startup check: existing users with auto-backup already on, no password yet ---
    LaunchedEffect(Unit) {
        if (backupSettingsViewModel.isAutoBackupUnsafe(settings.data.backupFrequency)) {
            showAutoBackupNeedsPasswordDialog = true
        }
    }

    // --- Post-import check: imported settings may enable auto-backup on a device with no password ---
    LaunchedEffect(Unit) {
        backupViewModel.importCompleted.collect {
            if (backupSettingsViewModel.isAutoBackupUnsafe(settings.data.backupFrequency)) {
                showAutoBackupNeedsPasswordDialog = true
            }
        }
    }

    // --- Import needs a password we don't have / doesn't match the stored one ---
    LaunchedEffect(Unit) {
        backupViewModel.passwordRequired.collect { uri ->
            pendingImportUri = uri
            showImportPasswordDialog = true
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { backupViewModel.importBackup(context, it) }
    }

    LaunchedEffect(Unit) {
        backupViewModel.backupResult.collect { result ->
            if (result.isSuccess) {
                Toast.makeText(context, operationSuccessful, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, operationFailed, Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                title = { Text(stringResource(R.string.data_title)) },
                navigationIcon = {
                    IconButton({ navController.navigateUp() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
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
                    onCustomClick = { coroutineScope.launch { backupViewModel.exportBackup(context) } }
                )
            }

            item {
                val reminderPermission = stringResource(R.string.Reminder_Permission)
                val notificationPermission = stringResource(R.string.Notification_Permission)

                SettingOption(
                    title = stringResource(R.string.Restore),
                    description = stringResource(R.string.Restore_Description),
                    icon = Icons.Rounded.Restore,
                    radius = shapeManager(radius = radius),
                    actionType = ActionType.CUSTOM,
                    onCustomClick = {
                        if (!canScheduleReminder(context)) {
                            Toast.makeText(context, reminderPermission, Toast.LENGTH_SHORT).show()
                            requestExactAlarmPermission(context)
                        }
                        if (!isNotificationPermissionGranted(context)) {
                            Toast.makeText(context, notificationPermission, Toast.LENGTH_SHORT).show()
                            openAppNotificationSettings(context)
                        }
                        if (canScheduleReminder(context) && isNotificationPermissionGranted(context)) {
                            importLauncher.launch(arrayOf("application/json"))
                        }
                    }
                )
            }

            // --- Backup password setting row ---
            item {
                SettingOption(
                    title = stringResource(R.string.backup_password_setting),
                    description =
                        if (hasPassword) stringResource(R.string.backup_encrypted)
                        else stringResource(R.string.backup_not_encrypted)
                    ,
                    icon = Icons.Rounded.Lock,
                    radius = shapeManager(radius = radius, isLast = true),
                    actionType = ActionType.CUSTOM,
                    onCustomClick = {
                        if (hasPassword) showChangePasswordWarning = true
                        else showSetPasswordDialog = true
                    }
                )
            }

            item {
                val workManager = remember { WorkManager.getInstance(context.applicationContext) }
                val backupManager = remember(workManager) { BackupManager(workManager) }

                fun mapDaysToSliderPosition(days: Int): Float = when (days) {
                    0 -> 0f; 1 -> 1f; 7 -> 2f; 30 -> 3f; else -> 0f
                }
                fun mapSliderPositionToFrequency(position: Float): BackupFrequency = when (position) {
                    0f -> BackupFrequency.NEVER; 1f -> BackupFrequency.DAILY
                    2f -> BackupFrequency.WEEKLY; 3f -> BackupFrequency.MONTHLY
                    else -> BackupFrequency.NEVER
                }

                var currentSliderPosition by remember(settings.data.backupFrequency) {
                    mutableFloatStateOf(mapDaysToSliderPosition(settings.data.backupFrequency))
                }
                val selectedFrequency = mapSliderPositionToFrequency(currentSliderPosition)

                ListItem(
                    modifier = Modifier.padding(top = 8.dp),
                    leadingContent = { Icon(Icons.Outlined.EditCalendar, contentDescription = "Auto backup") },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    headlineContent = { Text(stringResource(R.string.backup_frequency)) },
                    supportingContent = { Text(text = stringResource(selectedFrequency.textRes)) }
                )

                Slider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    value = currentSliderPosition,
                    onValueChange = { newPosition ->
                        currentSliderPosition = newPosition
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                        val newFrequency = mapSliderPositionToFrequency(newPosition)
                        onSettingsEvents(SettingEvents.UpdateSettings(settings.data.copy(backupFrequency = newFrequency.days)))
                    },
                    onValueChangeFinished = {
                        val newFrequency = mapSliderPositionToFrequency(currentSliderPosition)

                        coroutineScope.launch {
                            if (backupSettingsViewModel.isAutoBackupUnsafe(newFrequency.days)) {
                                pendingBackupFrequency = newFrequency
                                showAutoBackupNeedsPasswordDialog = true
                            } else {
                                backupManager.scheduleBackup(newFrequency)
                            }
                        }
                    },
                    valueRange = 0f..3f,
                    steps = 2
                )
            }

            item {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    leadingContent = { Icon(Icons.Outlined.CleaningServices, contentDescription = "Reset") },
                    headlineContent = { Text(stringResource(R.string.reset_database)) },
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
                        ) { Text(stringResource(R.string.reset)) }
                    },
                    supportingContent = { Text(stringResource(R.string.clear_app_data)) }
                )
            }
        }

        if (showWarningDialog) {
            DeleteAlert(
                onConfirmation = { showWarningDialog = false; onSettingsEvents(SettingEvents.ResetDatabase) },
                onDismissRequest = { showWarningDialog = false },
                dialogTitle = stringResource(R.string.deleteDialogTitle),
                dialogText = stringResource(R.string.deleteDialogText),
                icon = Icons.Default.Delete
            )
        }

        // --- Mandatory: auto-backup on, no password (migration / post-import / live toggle) ---
        if (showAutoBackupNeedsPasswordDialog) {
            AutoBackupNeedsPasswordDialog(
                onSetPasswordClick = {
                    showSetPasswordDialog = true
                },
                onTurnOffAutoBackup = {
                    showAutoBackupNeedsPasswordDialog = false
                    onSettingsEvents(SettingEvents.UpdateSettings(settings.data.copy(backupFrequency = BackupFrequency.NEVER.days)))
                    WorkManager.getInstance(context.applicationContext).let { BackupManager(it).scheduleBackup(BackupFrequency.NEVER) }
                }
            )
        }

        // --- Initial password setup (first time only) ---
        if (showSetPasswordDialog) {
            BackupPasswordEntryDialog(
                title = stringResource(R.string.set_backup_password),
                onConfirm = { pw ->
                    showAutoBackupNeedsPasswordDialog=false
                    showSetPasswordDialog = false
                    backupSettingsViewModel.setPassword(pw) {
                        pendingBackupFrequency?.let { frequency ->
                            val workManager = WorkManager.getInstance(context.applicationContext)
                            onSettingsEvents(SettingEvents.UpdateSettings(settings.data.copy(backupFrequency = frequency.days)))
                            BackupManager(workManager).scheduleBackup(frequency)
                            pendingBackupFrequency = null
                        }
                    }
                },
                onDismiss = {
                    showSetPasswordDialog = false
                }
            )
        }

        // --- Rotation warning, then capture new password ---
        if (showChangePasswordWarning) {
            ChangePasswordWarningDialog(
                onConfirm = { showChangePasswordWarning = false; showChangePasswordEntry = true },
                onDismiss = { showChangePasswordWarning = false }
            )
        }

        if (showChangePasswordEntry) {
            BackupPasswordEntryDialog(
                title = stringResource(R.string.set_backup_password),
                onConfirm = { pw ->
                    showChangePasswordEntry = false
                    backupSettingsViewModel.changePassword(pw)
                },
                onDismiss = { showChangePasswordEntry = false }
            )
        }

        // --- Per-file password prompt on import (new device, or file predates a rotation) ---
        if (showImportPasswordDialog) {
            BackupPasswordEntryDialog(
                title = stringResource(R.string.enter_backup_password_title),
                onConfirm = { pw ->
                    showImportPasswordDialog = false
                    pendingImportUri?.let { backupViewModel.importBackupWithPassword(context, it, pw) }
                },
                onDismiss = { showImportPasswordDialog = false }
            )
        }
    }
}
