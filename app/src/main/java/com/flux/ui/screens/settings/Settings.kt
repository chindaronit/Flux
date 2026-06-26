package com.flux.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ContactSupport
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.flux.R
import com.flux.navigation.NavRoutes
import com.flux.data.model.WorkspaceModel
import com.flux.ui.events.SettingEvents
import com.flux.ui.state.Settings

@Composable
fun Settings(
    navController: NavController,
    settings: Settings,
    onSettingsEvent: (SettingEvents) -> Unit,
    workspaces: List<WorkspaceModel>,
) {
    val context = LocalContext.current
    var showDefaultWsDialog by remember { mutableStateOf(false) }

    // Default workspace picker dialog
    if (showDefaultWsDialog) {
        val allWorkspaces = workspaces
        AlertDialog(
            onDismissRequest = { showDefaultWsDialog = false },
            title = { Text(stringResource(R.string.Default_Workspace)) },
            text = {
                LazyColumn {
                    item {
                        TextButton(
                            onClick = {
                                onSettingsEvent(SettingEvents.UpdateSettings(settings.data.copy(defaultWorkspaceId = null)))
                                showDefaultWsDialog = false
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                if (settings.data.defaultWorkspaceId == null)
                                    "• ${stringResource(R.string.No_Default_Workspace)}"
                                else
                                    stringResource(R.string.No_Default_Workspace)
                            )
                        }
                    }
                    allWorkspaces.forEach { ws ->
                        item {
                            TextButton(
                                onClick = {
                                    onSettingsEvent(SettingEvents.UpdateSettings(settings.data.copy(defaultWorkspaceId = ws.workspaceId)))
                                    showDefaultWsDialog = false
                                },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    if (ws.workspaceId == settings.data.defaultWorkspaceId)
                                        "• ${ws.title}"
                                    else
                                        ws.title
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDefaultWsDialog = false }) {
                    Text(stringResource(R.string.Cancel))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = 64.dp
            ),
        ) {
            item {
                SettingCategory(
                    title = stringResource(R.string.Customize),
                    subTitle = stringResource(R.string.Customize_desc),
                    icon = Icons.Rounded.Palette,
                    shape = shapeManager(radius = settings.data.cornerRadius, isFirst = true),
                    action = {
                        navController.navigate(NavRoutes.Customize.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    })
            }

            item {
                SettingCategory(
                    title = stringResource(R.string.editor_title),
                    subTitle = stringResource(R.string.editor_subtitle),
                    icon = Icons.Rounded.EditNote,
                    shape = shapeManager(radius = settings.data.cornerRadius),
                    action = {
                        navController.navigate(NavRoutes.Editor.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    })
            }

            item {
                SettingCategory(
                    title = stringResource(R.string.Languages),
                    subTitle = stringResource(R.string.Languages_desc),
                    icon = Icons.Rounded.Language,
                    isLast = true,
                    shape = shapeManager(radius = settings.data.cornerRadius, isLast = true),
                    action = {
                        navController.navigate(NavRoutes.Languages.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            item {
                Spacer(Modifier.height(12.dp))
            }

            // Default Workspace setting
            item {
                SettingCategory(
                    title = stringResource(R.string.Default_Workspace),
                    subTitle = stringResource(R.string.Default_Workspace_desc),
                    icon = Icons.Default.Workspaces,
                    shape = shapeManager(radius = settings.data.cornerRadius, isFirst = true, isLast = true),
                    action = { showDefaultWsDialog = true }
                )
            }

            item {
                Spacer(Modifier.height(12.dp))
                SettingCategory(
                    title = stringResource(R.string.Privacy),
                    subTitle = stringResource(R.string.Privacy_desc),
                    icon = Icons.Rounded.PrivacyTip,
                    shape = shapeManager(radius = settings.data.cornerRadius, isFirst = true),
                    action = {
                        navController.navigate(NavRoutes.Privacy.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    })
            }

            item {
                SettingCategory(
                    title = stringResource(R.string.data_title),
                    subTitle = stringResource(R.string.data_subtitle),
                    icon = Icons.Rounded.Backup,
                    shape = shapeManager(radius = settings.data.cornerRadius, isLast = true),
                    action = {
                        navController.navigate(NavRoutes.Backup.route)
                    }
                )
            }

            item {
                Spacer(Modifier.height(12.dp))
                SettingCategory(
                    title = stringResource(R.string.About),
                    subTitle = stringResource(R.string.About_desc),
                    icon = Icons.Rounded.Info,
                    shape = shapeManager(radius = settings.data.cornerRadius, isFirst = true),
                    action = {
                        navController.navigate(NavRoutes.About.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    })
            }

            item {
                SettingCategory(
                    title = stringResource(R.string.Contact),
                    subTitle = stringResource(R.string.Contact_desc),
                    icon = Icons.AutoMirrored.Rounded.ContactSupport,
                    shape = shapeManager(radius = settings.data.cornerRadius, isLast = true),
                    action = {
                        navController.navigate(NavRoutes.Contact.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    })
            }

            item {
                Spacer(Modifier.height(12.dp))
                SingleSettingOption(
                    radius = settings.data.cornerRadius,
                    text = stringResource(R.string.Support),
                    description = stringResource(R.string.Support_desc),
                    trailingIcon = SettingIcon.Vector(Icons.Default.Coffee),
                    last = true
                ) {
                    val intent =
                        Intent(Intent.ACTION_VIEW, "https://coff.ee/chindaronit".toUri())
                    context.startActivity(intent)
                }
            }
        }
    }
}
