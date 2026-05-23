package com.flux.ui.screens.workspaces

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.data.model.WorkspaceModel
import com.flux.navigation.NavRoutes
import com.flux.ui.common.EmptySpaces
import com.flux.ui.events.WorkspaceEvents
import com.flux.R
import com.flux.ui.common.BottomBar
import com.flux.ui.common.SelectedToolBarRow
import com.flux.ui.events.LabelEvents
import com.flux.ui.state.States
import com.flux.ui.viewModel.ViewModels

@OptIn(ExperimentalMaterial3Api::class)
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
    var query by rememberSaveable { mutableStateOf("") }
    val wrongPassKeyLabel = stringResource(R.string.Wrong_Passkey)
    val selectedWorkspace = remember { mutableStateListOf<WorkspaceModel>() }
    var lockedWorkspace by remember { mutableStateOf<WorkspaceModel?>(null) }

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            if (selectedWorkspace.isNotEmpty()) {
                Box(Modifier.padding(top = 42.dp)){
                    SelectedToolBarRow (
                        false,
                        selectedWorkspace.size,
                        selectedWorkspace.containsAll(allSpaces),
                        selectedWorkspace.all { it.isPinned },
                        onTogglePin = { viewModels.workspaceViewModel.onEvent(WorkspaceEvents.UpsertSpaces(selectedWorkspace.toList())) },
                        onToggleSelection = {
                            if (selectedWorkspace.containsAll(allSpaces)){ selectedWorkspace.clear() }
                            else {
                                selectedWorkspace.clear()
                                selectedWorkspace.addAll(allSpaces)
                            }
                        },
                        onClear = { selectedWorkspace.clear() }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (allSpaces.isEmpty()) { EmptySpaces() } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val vSpacing = when (gridColumns) {
                    1 -> 12.dp
                    2 -> 16.dp
                    else -> 10.dp
                }
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(gridColumns),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalItemSpacing = vSpacing,
                    contentPadding = PaddingValues(
                        top = 16.dp,
                        bottom = 64.dp
                    )
                ) {
                    if (allSpaces.none {
                            it.title.contains(query, ignoreCase = true) ||
                                    it.description.contains(query, ignoreCase = true)
                        }) {
                        item(span = StaggeredGridItemSpan.FullLine) { EmptySpaces() }
                    }

                    if (allSpaces.any {
                            it.isPinned && (it.title.contains(query, ignoreCase = true) ||
                                    it.description.contains(query, ignoreCase = true))
                        }) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Text(
                                stringResource(R.string.Pinned),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    items(allSpaces.filter {
                        it.isPinned && (it.title.contains(query, ignoreCase = true) ||
                                it.description.contains(query, ignoreCase = true))
                    }) { space ->
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
                                if (selectedWorkspace.contains(space)) selectedWorkspace.remove(
                                    space
                                )
                                else selectedWorkspace.add(space)
                            }
                        )
                    }

                    if (allSpaces.any {
                            it.isPinned && (it.title.contains(query, ignoreCase = true) ||
                                    it.description.contains(query, ignoreCase = true))
                        }) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Text(
                                stringResource(R.string.Others),
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    items(allSpaces.filter {
                        !it.isPinned && (it.title.contains(query, ignoreCase = true) ||
                                it.description.contains(query, ignoreCase = true))
                    }) { space ->
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
                                if (selectedWorkspace.contains(space)) selectedWorkspace.remove(
                                    space
                                )
                                else selectedWorkspace.add(space)
                            }
                        )
                    }
                }
                BottomBar(
                    navController = navController,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                )
            }
        }
    }
}