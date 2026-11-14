package com.omnipad.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.work.WorkManager
import com.omnipad.R
import com.omnipad.other.BackupFrequency
import com.omnipad.other.BackupManager
import com.omnipad.other.canScheduleReminder
import com.omnipad.other.isNotificationPermissionGranted
import com.omnipad.other.openAppNotificationSettings
import com.omnipad.other.requestExactAlarmPermission
import com.omnipad.ui.common.ChangeRootProgressDialog
import com.omnipad.ui.common.BackupPasswordEntryDialog
import com.omnipad.ui.common.ChangePasswordWarningDialog
import com.omnipad.ui.common.DeleteAlert
import com.omnipad.ui.events.SettingEvents
import com.omnipad.ui.state.RootChangeState
import com.omnipad.ui.state.Settings
import com.omnipad.ui.viewModel.BackupViewModel
import com.omnipad.ui.viewModel.BackupSettingsViewModel
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Data(
    navController: NavController,
    radius: Int,
    settings: Settings,
    rootChangeState: RootChangeState,
    snackbarHostState: SnackbarHostState,
    backupViewModel: BackupViewModel,
    backupSettingsViewModel: BackupSettingsViewModel,
    onSettingsEvents: (SettingEvents) -> Unit
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val operationSuccessful = stringResource(R.string.success)
    val operationFailed = stringResource(R.string.Failed)
    val reminderPermission = stringResource(R.string.Reminder_Permission)
    val notificationPermission = stringResource(R.string.Notification_Permission)
    val backupOperation by backupViewModel.operation.collectAsState()

    var showRootChangeDialog by remember { mutableStateOf(false) }
    var showWarningDialog by remember { mutableStateOf(false) }
    var showRestoreWarning by remember { mutableStateOf(false) }
    var showSetPasswordDialog by remember { mutableStateOf(false) }
    var showChangePasswordWarning by remember { mutableStateOf(false) }
    var showChangePasswordEntry by remember { mutableStateOf(false) }
    var showPasswordOptions by remember { mutableStateOf(false) }
    var showDisableEncryptionDialog by remember { mutableStateOf(false) }
    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    val hasPassword by backupSettingsViewModel.hasBackupPassword.collectAsState()
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            backupViewModel.importBackup(context, it)
        }
    }

    var pendingNewRootUri by remember { mutableStateOf<Uri?>(null) }

    val rootPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        pendingNewRootUri = uri
        showRootChangeDialog = true
        onSettingsEvents(SettingEvents.ChangeStorageRoot(context, uri))
    }

    LaunchedEffect(Unit) {
        backupViewModel.passwordRequired.collect { uri ->
            pendingImportUri = uri
            showImportPasswordDialog = true
        }
    }

    LaunchedEffect(Unit) {
        backupViewModel.backupResult.collect { result ->
            if (result.isSuccess) {
                Toast.makeText(context, operationSuccessful, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(
                    context,
                    result.exceptionOrNull()?.localizedMessage ?: operationFailed,
                    Toast.LENGTH_LONG
                ).show()
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
                    customButton = {
                        if (backupOperation == com.omnipad.ui.viewModel.BackupOperation.BACKUP) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            RenderCustomIcon()
                        }
                    },
                    onCustomClick = { backupViewModel.exportBackup(context) }
                )
            }

            item {
                SettingOption(
                    title = stringResource(R.string.Restore),
                    description = stringResource(R.string.Restore_Description),
                    icon = Icons.Rounded.Restore,
                    radius = shapeManager(radius = radius),
                    actionType = ActionType.CUSTOM,
                    customButton = {
                        if (backupOperation == com.omnipad.ui.viewModel.BackupOperation.RESTORE) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            RenderCustomIcon()
                        }
                    },
                    onCustomClick = {
                        if (backupOperation == null) showRestoreWarning = true
                    }
                )
            }

            item {
                SettingOption(
                    title = stringResource(R.string.backup_password_setting),
                    description = stringResource(
                        if (hasPassword) R.string.backup_encrypted
                        else R.string.backup_not_encrypted
                    ),
                    icon = Icons.Rounded.Lock,
                    radius = shapeManager(radius = radius, isLast = true),
                    actionType = ActionType.CUSTOM,
                    onCustomClick = {
                        if (hasPassword) showPasswordOptions = true
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
                fun mapSliderPositionToFrequency(position: Float): BackupFrequency {
                    return when (position.roundToInt()) {
                        0 -> BackupFrequency.NEVER
                        1 -> BackupFrequency.DAILY
                        2 -> BackupFrequency.WEEKLY
                        3 -> BackupFrequency.MONTHLY
                        else -> BackupFrequency.NEVER
                    }
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
                    },
                    onValueChangeFinished = {
                        val frequency = mapSliderPositionToFrequency(currentSliderPosition)

                        onSettingsEvents(
                            SettingEvents.UpdateSettings(
                                settings.data.copy(
                                    backupFrequency = frequency.days
                                )
                            )
                        )
                        backupManager.scheduleBackup(frequency)
                    },
                    valueRange = 0f..3f,
                    steps = 2
                )
            }

            item {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    leadingContent = { Icon(Icons.Outlined.Storage, contentDescription = "Storage") },
                    headlineContent = { Text(stringResource(R.string.change_storage_root)) },
                    trailingContent = {
                        TextButton(
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                                    addFlags(
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                                                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                    )
                                }
                                rootPicker.launch(intent)
                            },
                            colors = ButtonDefaults.textButtonColors().copy(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) { Text(stringResource(R.string.change)) }
                    },
                    supportingContent = { Text(stringResource(R.string.change_root_directory)) }
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

        if (showRestoreWarning) {
            AlertDialog(
                onDismissRequest = { showRestoreWarning = false },
                title = { Text(stringResource(R.string.restore_backup_warning_title)) },
                text = { Text(stringResource(R.string.restore_backup_warning_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        showRestoreWarning = false
                        if (!canScheduleReminder(context)) {
                            Toast.makeText(context, reminderPermission, Toast.LENGTH_SHORT).show()
                            requestExactAlarmPermission(context)
                        }
                        if (!isNotificationPermissionGranted(context)) {
                            Toast.makeText(context, notificationPermission, Toast.LENGTH_SHORT).show()
                            openAppNotificationSettings(context)
                        }
                        if (canScheduleReminder(context) && isNotificationPermissionGranted(context)) {
                            importLauncher.launch(null)
                        }
                    }) { Text(stringResource(R.string.Confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreWarning = false }) {
                        Text(stringResource(R.string.Dismiss))
                    }
                }
            )
        }

        if (showSetPasswordDialog) {
            BackupPasswordEntryDialog(
                title = stringResource(R.string.set_backup_password_title),
                onConfirm = { password ->
                    showSetPasswordDialog = false
                    backupSettingsViewModel.setPassword(password)
                },
                onDismiss = { showSetPasswordDialog = false }
            )
        }

        if (showPasswordOptions) {
            AlertDialog(
                onDismissRequest = { showPasswordOptions = false },
                title = { Text(stringResource(R.string.backup_password_setting)) },
                text = { Text(stringResource(R.string.backup_encrypted)) },
                confirmButton = {
                    TextButton(onClick = {
                        showPasswordOptions = false
                        showChangePasswordWarning = true
                    }) { Text(stringResource(R.string.change_backup_password_title)) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showPasswordOptions = false
                        showDisableEncryptionDialog = true
                    }) { Text(stringResource(R.string.disable_backup_encryption)) }
                }
            )
        }

        if (showDisableEncryptionDialog) {
            AlertDialog(
                onDismissRequest = { showDisableEncryptionDialog = false },
                title = { Text(stringResource(R.string.disable_backup_encryption)) },
                text = { Text(stringResource(R.string.disable_backup_encryption_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        showDisableEncryptionDialog = false
                        backupSettingsViewModel.disableEncryption()
                    }) { Text(stringResource(R.string.disable_backup_encryption)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDisableEncryptionDialog = false }) {
                        Text(stringResource(R.string.Dismiss))
                    }
                }
            )
        }

        if (showChangePasswordWarning) {
            ChangePasswordWarningDialog(
                onConfirm = {
                    showChangePasswordWarning = false
                    showChangePasswordEntry = true
                },
                onDismiss = { showChangePasswordWarning = false }
            )
        }

        if (showChangePasswordEntry) {
            BackupPasswordEntryDialog(
                title = stringResource(R.string.set_backup_password_title),
                onConfirm = { password ->
                    showChangePasswordEntry = false
                    backupSettingsViewModel.changePassword(password)
                },
                onDismiss = { showChangePasswordEntry = false }
            )
        }

        if (showImportPasswordDialog) {
            BackupPasswordEntryDialog(
                title = stringResource(R.string.enter_backup_password_title),
                onConfirm = { password ->
                    showImportPasswordDialog = false
                    pendingImportUri?.let {
                        backupViewModel.importBackupWithPassword(context, it, password)
                    }
                },
                onDismiss = { showImportPasswordDialog = false }
            )
        }

        if (showRootChangeDialog) {
            ChangeRootProgressDialog(
                state = rootChangeState,
                newRootUri = pendingNewRootUri,
                onRetry = { uri -> onSettingsEvents(SettingEvents.ChangeStorageRoot(context, uri)) },
                onDismiss = {
                    showRootChangeDialog = false
                    onSettingsEvents(SettingEvents.ResetRootChange)
                }
            )
        }
    }
}
