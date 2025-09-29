package com.flux.ui.screens.settings

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.other.canScheduleReminder
import com.flux.other.isNotificationPermissionGranted
import com.flux.other.openAppNotificationSettings
import com.flux.other.requestExactAlarmPermission
import com.flux.ui.components.ActionType
import com.flux.ui.components.BasicScaffold
import com.flux.ui.components.SettingOption
import com.flux.ui.components.shapeManager
import com.flux.ui.viewModel.BackupViewModel

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Backup(
    navController: NavController,
    radius: Int,
    backupViewModel: BackupViewModel
) {
    val context = LocalContext.current
    val backupResult = backupViewModel.backupResult.collectAsState(initial = null)

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

    // Observe result
    LaunchedEffect(backupResult.value) {
        backupResult.value?.let { result ->
            if (result.isSuccess) {
                Toast.makeText(context, "Operation successful!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Operation failed.", Toast.LENGTH_LONG).show()
            }
        }
    }

    BasicScaffold(
        title = stringResource(R.string.Backup),
        onBackClicked = {
            navController.navigateUp()
        }
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
        }
    }
}

