package com.flux.ui.screens.workspaces

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.WorkspaceModel
import com.flux.navigation.NavRoutes
import com.flux.ui.events.WorkspaceEvents
import com.flux.ui.state.States
import com.flux.ui.viewModel.ViewModels

@Composable
fun WorkspaceHomeScreen(
    snackbarHostState: SnackbarHostState,
    navController: NavController,
    states: States,
    viewModels: ViewModels,
) {
    val context = LocalContext.current
    val radius = states.settings.data.cornerRadius
    val gridColumns = states.settings.data.workspaceGridColumns
    val allSpaces = states.workspaceState.allWorkspaces
    val wrongPassKeyLabel = stringResource(R.string.Wrong_Passkey)
    val selectedWorkspace = remember { mutableStateListOf<WorkspaceModel>() }
    var lockedWorkspace by remember { mutableStateOf<WorkspaceModel?>(null) }

    // Auto-navigate to default workspace if set
    val defaultWsId = states.settings.data.defaultWorkspaceId
    LaunchedEffect(defaultWsId, allSpaces) {
        if (defaultWsId != null) {
            val ws = allSpaces.find { it.workspaceId == defaultWsId }
            if (ws != null) {
                if (ws.passKey != null) {
                    lockedWorkspace = ws
                } else {
                    navController.navigate(NavRoutes.WorkspaceHome.withArgs(ws.workspaceId)) {
                        popUpTo(NavRoutes.Workspace.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    lockedWorkspace?.let {
        SetPasskeyDialog(onConfirmRequest = { passkey ->
            if (it.passKey == passkey) {
                navController.navigate(NavRoutes.WorkspaceHome.withArgs(it.workspaceId))
            } else {
                Toast.makeText(context, wrongPassKeyLabel, Toast.LENGTH_SHORT).show()
            }
        }) { lockedWorkspace = null }
    }

    fun handleWorkspaceClick(space: WorkspaceModel) {
        if (space.passKey!=null) { lockedWorkspace = space }
        else {
            navController.navigate(NavRoutes.WorkspaceHome.withArgs(space.workspaceId))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (allSpaces.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Workspaces,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
                Text(stringResource(R.string.Empty_Workspace))
            }
        }

        val vSpacing = when (gridColumns) {
            1 -> 12.dp
            2 -> 16.dp
            else -> 10.dp
        }
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(gridColumns),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalItemSpacing = vSpacing,
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = 64.dp
            )
        ) {
            if (allSpaces.any { it.isPinned }) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        stringResource(R.string.Pinned),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            items(allSpaces.filter { it.isPinned }) { space ->
                WorkspaceCard(
                    gridColumns = gridColumns,
                    iconIndex = space.icon,
                    radius = radius,
                    isLocked = space.passKey != null,
                    cover = space.cover,
                    title = space.title,
                    description = space.description,
                    isSelected = selectedWorkspace.contains(space),
                    onClick = { handleWorkspaceClick(space) },
                    onLongPressed = {
                        if (selectedWorkspace.contains(space)) selectedWorkspace.remove(space)
                        else selectedWorkspace.add(space)
                    }
                )
            }

            if (allSpaces.any { it.isPinned }) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        stringResource(R.string.Others),
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            items(allSpaces.filter { !it.isPinned }) { space ->
                WorkspaceCard(
                    gridColumns = gridColumns,
                    iconIndex = space.icon,
                    radius = radius,
                    isLocked = space.passKey != null,
                    cover = space.cover,
                    title = space.title,
                    description = space.description,
                    isSelected = selectedWorkspace.contains(space),
                    onClick = { handleWorkspaceClick(space) },
                    onLongPressed = {
                        if (selectedWorkspace.contains(space)) selectedWorkspace.remove(space)
                        else selectedWorkspace.add(space)
                    }
                )
            }
        }
    }
}
