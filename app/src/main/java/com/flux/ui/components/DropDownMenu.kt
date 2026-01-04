package com.flux.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PhotoSizeSelectActual
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flux.R
import com.flux.data.model.WorkspaceModel

@Composable
fun DropdownMenuWithDetails(
    isPinned: Boolean,
    onTogglePinned: () -> Unit,
    onAddLabel: () -> Unit,
    onAboutClicked: () -> Unit,
    onShareNote: () -> Unit,
    onSaveNote: () -> Unit,
    onPrintNote: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier) {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "More options"
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(if (isPinned) stringResource(R.string.Unpin) else stringResource(R.string.Pin)) },
                leadingIcon = {
                    Icon(
                        if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = null
                    )
                },
                onClick = onTogglePinned
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.Labels)) },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Outlined.Label,
                        contentDescription = null
                    )
                },
                trailingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = {
                    expanded = false
                    onAddLabel()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Share Note") },
                leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                onClick = {
                    expanded = false
                    onShareNote()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Save Note") },
                leadingIcon = { Icon(Icons.Outlined.Download, contentDescription = null) },
                onClick = {
                    expanded = false
                    onSaveNote()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Print Note") },
                leadingIcon = { Icon(Icons.Outlined.Print, contentDescription = null) },
                onClick = {
                    expanded = false
                    onPrintNote()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.About)) },
                leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                onClick = {
                    expanded = false
                    onAboutClicked()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error
                ),
                text = { Text(stringResource(R.string.Delete_Note)) },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDelete()
                }
            )
        }
    }
}

@Composable
fun SpacesMenu(
    expanded: Boolean,
    workspace: WorkspaceModel,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedSpaces = workspace.selectedSpaces
    DropdownMenu(
        shape = RoundedCornerShape(16.dp),
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        if (selectedSpaces.contains(1)) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.Notes)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Default.Notes, contentDescription = null) },
                onClick = {
                    onConfirm(1)
                    onDismiss()
                }
            )
        }
        if (selectedSpaces.contains(2)) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.To_Do)) },
                leadingIcon = { Icon(Icons.Outlined.TaskAlt, contentDescription = null) },
                onClick = {
                    onConfirm(2)
                    onDismiss()
                }
            )
        }
        if (selectedSpaces.contains(3)) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.Events)) },
                leadingIcon = { Icon(Icons.Outlined.Event, contentDescription = null) },
                onClick = {
                    onConfirm(3)
                    onDismiss()
                }
            )
        }
        if (selectedSpaces.contains(4)) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.Journal)) },
                leadingIcon = { Icon(Icons.Outlined.AutoStories, contentDescription = null) },
                onClick = {
                    onConfirm(4)
                    onDismiss()
                }
            )
        }
        if (selectedSpaces.contains(5)) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.Habits)) },
                leadingIcon = { Icon(Icons.Outlined.EventAvailable, contentDescription = null) },
                onClick = {
                    onConfirm(5)
                    onDismiss()
                }
            )
        }
        if (selectedSpaces.contains(6)) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.Analytics)) },
                leadingIcon = { Icon(Icons.Outlined.Analytics, contentDescription = null) },
                onClick = {
                    onConfirm(6)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
fun WorkspaceMore(
    isLocked: Boolean,
    isCoverAdded: Boolean,
    showEditLabel: Boolean,
    isPinned: Boolean,
    onEditDetails: () -> Unit,
    onEditLabel: () -> Unit,
    onRemoveCover: () -> Unit,
    onAddCover: () -> Unit,
    onDelete: () -> Unit,
    onTogglePinned: () -> Unit,
    onToggleLock: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier) {
        IconButton(
            onClick = { expanded = true }, colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) { Icon(Icons.Default.MoreVert, contentDescription = "More options") }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.Edit_Details)) },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                onClick = {
                    expanded = false
                    onEditDetails()
                }
            )
            DropdownMenuItem(
                text = { Text(if (isPinned) stringResource(R.string.Unpin) else stringResource(R.string.Pin)) },
                leadingIcon = {
                    Icon(
                        if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onTogglePinned()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.Change_Cover)) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.PhotoSizeSelectActual,
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onAddCover()
                }
            )
            if (isCoverAdded) {
                DropdownMenuItem(
                    colors = MenuDefaults.itemColors(
                        leadingIconColor = MaterialTheme.colorScheme.error,
                        textColor = MaterialTheme.colorScheme.error
                    ),
                    text = { Text("Remove Cover") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.RemoveCircleOutline,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        expanded = false
                        onRemoveCover()
                    }
                )
            }

            if (showEditLabel) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.Labels)) },
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Outlined.Label,
                            contentDescription = null
                        )
                    },
                    trailingIcon = { Icon(Icons.Default.KeyboardDoubleArrowRight, null) },
                    onClick = {
                        expanded = false
                        onEditLabel()
                    }
                )
                HorizontalDivider()
            }

            DropdownMenuItem(
                text = {
                    Text(
                        if (isLocked) stringResource(R.string.Unlock_Workspace) else stringResource(
                            R.string.Lock_Workspace
                        )
                    )
                },
                leadingIcon = {
                    Icon(
                        if (isLocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onToggleLock()
                }
            )
            DropdownMenuItem(
                colors = MenuDefaults.itemColors(
                    leadingIconColor = MaterialTheme.colorScheme.error,
                    textColor = MaterialTheme.colorScheme.error
                ),
                text = { Text(stringResource(R.string.Delete_Workspace)) },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDelete()
                }
            )
        }
    }
}
