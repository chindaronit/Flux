package com.flux.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ControlPointDuplicate
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
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.TrackChanges
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
    onConvertNote: () ->Unit,
    onCopyNote: () -> Unit,
    onCloneNote: () -> Unit,
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
                text = { Text("Clone") },
                leadingIcon = { Icon(Icons.Outlined.ControlPointDuplicate, contentDescription = null) },
                onClick = {
                    expanded = false
                    onCloneNote()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Copy") },
                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                onClick = {
                    expanded = false
                    onCopyNote()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Convert") },
                leadingIcon = { Icon(Icons.Outlined.SwapHoriz, contentDescription = null) },
                onClick = {
                    expanded = false
                    onConvertNote()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.share)) },
                leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                onClick = {
                    expanded = false
                    onShareNote()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.save)) },
                leadingIcon = { Icon(Icons.Outlined.Download, contentDescription = null) },
                onClick = {
                    expanded = false
                    onSaveNote()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.print)) },
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
                text = { Text(stringResource(R.string.delete)) },
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
fun JournalDropdownMenu(
    onAddLabel: () -> Unit,
    onAboutClicked: () -> Unit,
    onShareNote: () -> Unit,
    onSaveNote: () -> Unit,
    onPrintNote: () -> Unit,
    onDelete: () -> Unit,
    onConvertNote: () ->Unit,
    onCopyNote: () -> Unit,
    onCloneNote: () -> Unit
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
                text = { Text("Clone") },
                leadingIcon = { Icon(Icons.Outlined.ControlPointDuplicate, contentDescription = null) },
                onClick = {
                    expanded = false
                    onCloneNote()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Copy") },
                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                onClick = {
                    expanded = false
                    onCopyNote()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Convert") },
                leadingIcon = { Icon(Icons.Outlined.SwapHoriz, contentDescription = null) },
                onClick = {
                    expanded = false
                    onConvertNote()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.share)) },
                leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                onClick = {
                    expanded = false
                    onShareNote()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.save)) },
                leadingIcon = { Icon(Icons.Outlined.Download, contentDescription = null) },
                onClick = {
                    expanded = false
                    onSaveNote()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.print)) },
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
                text = { Text(stringResource(R.string.delete)) },
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
    val scrollState = rememberScrollState()

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        scrollState = scrollState,
        modifier = Modifier.heightIn(max = 300.dp)
    ) {

        @Composable
        fun MenuItem(
            visible: Boolean,
            id: Int,
            title: String,
            icon: ImageVector
        ) {
            if (!visible) return

            DropdownMenuItem(
                text = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 100.dp)
                    )
                },
                leadingIcon = {
                    Icon(icon, null)
                },
                onClick = {
                    onConfirm(id)
                    onDismiss()
                }
            )
        }

        MenuItem(
            selectedSpaces.contains(1),
            1,
            stringResource(R.string.Notes),
            Icons.AutoMirrored.Default.Notes
        )

        MenuItem(
            selectedSpaces.contains(2),
            2,
            stringResource(R.string.To_Do),
            Icons.Outlined.TaskAlt
        )

        MenuItem(
            selectedSpaces.contains(3),
            3,
            stringResource(R.string.Events),
            Icons.Outlined.Event
        )

        MenuItem(
            selectedSpaces.contains(4),
            4,
            stringResource(R.string.Journal),
            Icons.Outlined.AutoStories
        )

        MenuItem(
            selectedSpaces.contains(5),
            5,
            stringResource(R.string.Habits),
            Icons.Outlined.EventAvailable
        )

        MenuItem(
            selectedSpaces.contains(7),
            7,
            stringResource(R.string.progress_tracker),
            Icons.Outlined.TrackChanges
        )

        MenuItem(
            selectedSpaces.contains(6),
            6,
            stringResource(R.string.Analytics),
            Icons.Outlined.Analytics
        )
    }
}

@Composable
fun WorkspaceMore(
    isLocked: Boolean = false,
    isCoverAdded: Boolean = false,
    onEditDetails: () -> Unit,
    onRemoveCover: () -> Unit = {},
    onAddCover: () -> Unit = {},
    onDelete: () -> Unit,
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
                    text = { Text(stringResource(R.string.Remove_Cover)) },
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

@Composable
fun TodoDropdownMenu(
    canShare: Boolean,
    onShare: () -> Unit,
    onPrint: () -> Unit,
    onClone: () -> Unit,
    onCopy:  () -> Unit,
    onConvert: () -> Unit,
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
            if(canShare){
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.share)) },
                    leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onShare()
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.print)) },
                    leadingIcon = { Icon(Icons.Outlined.Print, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onPrint()
                    }
                )
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text("Clone") },
                leadingIcon = { Icon(Icons.Outlined.ControlPointDuplicate, contentDescription = null) },
                onClick = {
                    expanded = false
                    onClone()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Copy") },
                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                onClick = {
                    expanded = false
                    onCopy()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Convert") },
                leadingIcon = { Icon(Icons.Outlined.SwapHoriz, contentDescription = null) },
                onClick = {
                    expanded = false
                    onConvert()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error
                ),
                text = { Text(stringResource(R.string.delete)) },
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
fun EventDropdownMenu(
    onDelete: () -> Unit,
    onCopyNote: () -> Unit,
    onCloneNote: () -> Unit
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
                text = { Text("Clone") },
                leadingIcon = { Icon(Icons.Outlined.ControlPointDuplicate, contentDescription = null) },
                onClick = {
                    expanded = false
                    onCloneNote()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Copy") },
                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                onClick = {
                    expanded = false
                    onCopyNote()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error
                ),
                text = { Text(stringResource(R.string.delete)) },
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
fun HabitDropdownMenu(
    onDelete: () -> Unit,
    onCopyNote: () -> Unit,
    onCloneNote: () -> Unit,
    onShare: () -> Unit
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
                text = { Text(stringResource(R.string.clone)) },
                leadingIcon = { Icon(Icons.Outlined.ControlPointDuplicate, contentDescription = null) },
                onClick = {
                    expanded = false
                    onCloneNote()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.copy)) },
                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                onClick = {
                    expanded = false
                    onCopyNote()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.share)) },
                leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                onClick = {
                    expanded = false
                    onShare()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error
                ),
                text = { Text(stringResource(R.string.delete)) },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDelete()
                }
            )
        }
    }
}