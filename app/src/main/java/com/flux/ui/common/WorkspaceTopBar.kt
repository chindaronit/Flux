package com.flux.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flux.R
import com.flux.data.model.WorkspaceModel
import com.flux.other.icons

/**
 * A reusable dropdown trigger showing the current workspace icon + name.
 * Clicking opens a dropdown menu to switch workspaces or create a new one.
 */
@Composable
fun WorkspaceDropdownTrigger(
    currentWorkspace: WorkspaceModel?,
    allWorkspaces: List<WorkspaceModel>,
    onWorkspaceSelected: (WorkspaceModel) -> Unit,
    onNewWorkspace: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (currentWorkspace != null) {
                Icon(
                    icons.getOrElse(currentWorkspace.icon) { icons[48] },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = currentWorkspace.title.ifBlank { stringResource(R.string.Workspace) },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 120.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = stringResource(R.string.Select_Workspace),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(16.dp)
        ) {
            allWorkspaces.forEach { ws ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = ws.title.ifBlank { stringResource(R.string.Workspace) },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (ws.workspaceId == currentWorkspace?.workspaceId)
                                FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        Icon(
                            icons.getOrElse(ws.icon) { icons[48] },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        expanded = false
                        if (ws.workspaceId != currentWorkspace?.workspaceId) {
                            onWorkspaceSelected(ws)
                        }
                    }
                )
            }

            if (allWorkspaces.isNotEmpty()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.New_Workspace)) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        expanded = false
                        onNewWorkspace()
                    }
                )
            }
        }
    }
}

/**
 * Global top bar with workspace dropdown on the left.
 * Used on every main screen of the app.
 *
 * @param currentWorkspace  The currently active workspace (null if none)
 * @param allWorkspaces     Full list of workspaces for the dropdown
 * @param onWorkspaceSelected Called when user picks a workspace from the dropdown
 * @param onNewWorkspace    Called when user taps "New workspace"
 * @param showBackButton    If true, shows a back arrow before the workspace dropdown
 * @param onBackPressed    Called when back arrow is tapped
 * @param title            Optional center title (defaults to workspace name)
 * @param actions          Optional action buttons on the right of the bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceDropdownTopBar(
    currentWorkspace: WorkspaceModel?,
    allWorkspaces: List<WorkspaceModel>,
    onWorkspaceSelected: (WorkspaceModel) -> Unit,
    onNewWorkspace: () -> Unit,
    showBackButton: Boolean = false,
    onBackPressed: () -> Unit = {},
    title: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        navigationIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                if (showBackButton) {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
                WorkspaceDropdownTrigger(
                    currentWorkspace = currentWorkspace,
                    allWorkspaces = allWorkspaces,
                    onWorkspaceSelected = onWorkspaceSelected,
                    onNewWorkspace = onNewWorkspace
                )
            }
        },
        title = {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        actions = actions
    )
}
