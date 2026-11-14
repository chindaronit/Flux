package com.omnipad.ui.screens.progressBoard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.omnipad.R
import com.omnipad.data.model.WorkspaceModel
import com.omnipad.navigation.Loader
import com.omnipad.navigation.NavRoutes
import com.omnipad.ui.common.EmptyData
import com.omnipad.ui.common.SpaceSearchBar
import com.omnipad.ui.common.SpaceTopBar
import com.omnipad.ui.common.SpacesMenu
import com.omnipad.ui.common.convertMillisToDate
import com.omnipad.ui.screens.workspaces.SpacesToolBar
import com.omnipad.ui.state.ProgressBoardState
import com.omnipad.ui.state.Settings
import com.omnipad.ui.theme.completed
import com.omnipad.ui.theme.failed
import com.omnipad.ui.theme.pending
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressTrackerScreen(
    navController: NavController,
    state: ProgressBoardState,
    settings: Settings,
    workspace: WorkspaceModel,
    onShowSpaceBottomSheet: () -> Unit,
    onSpaceChange: (Int) -> Unit,
    onAddCover: () -> Unit,
    onRemoveCover: () -> Unit,
    onDeleteWorkspace: () -> Unit,
    onToggleLock: () -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    showSearchBar: Boolean,
    onSearchBarVisibilityChange: (Boolean) -> Unit
){
    val workspaceId = workspace.workspaceId
    val isLoading = state.isLoading
    val radius = settings.data.cornerRadius
    val boardItems = state.allItems.filter {
        it.workspaceId == workspaceId && (
            it.title.contains(query, ignoreCase = true) ||
            convertMillisToDate(it.startDate).contains(query, ignoreCase = true) ||
            convertMillisToDate(it.endDate).contains(query, ignoreCase = true)
        )
    }
    val notStartedItems = boardItems.filter { it.status == 0 }
    val inProgressItems = boardItems.filter { it.status == 1 }
    val completedItems = boardItems.filter { it.status == 2 }
    var showSpacesMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            SpaceTopBar(
                title            = workspace.title,
                description      = workspace.description,
                cover            = workspace.cover,
                icon             = workspace.icon,
                isLocked = workspace.isLocked,
                onBackPressed = { navController.popBackStack() },
                onAddCover = onAddCover,
                onRemoveCover = onRemoveCover,
                onToggleLock = onToggleLock,
                onDeleteWorkspace = onDeleteWorkspace,
                onEditWorkspace = { navController.navigate(NavRoutes.NewWorkspace.withArgs(workspaceId)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton({
                val newId = UUID.randomUUID().toString()
                navController.navigate(NavRoutes.NewProgressItem.withArgs(workspaceId, newId))
            }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { innerPadding ->
        when {
            isLoading -> Loader()
            else -> Column(
                Modifier.fillMaxSize().padding(innerPadding).padding(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SpacesToolBar(
                        stringResource(R.string.progress_tracker),
                        Icons.Default.TrackChanges,
                        false,
                        onMainClick = { showSpacesMenu = true },
                        onEditClick = onShowSpaceBottomSheet
                    )
                    SpacesMenu(showSpacesMenu, workspace, onSpaceChange) { showSpacesMenu = false }
                }
                SpaceSearchBar(
                    query, onQueryChange, {},
                    modifier = Modifier.padding(bottom = 12.dp),
                    horizontalPadding = 0.dp
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if(boardItems.isEmpty()) item { EmptyData() }
                    if (notStartedItems.isNotEmpty()) {
                        item {
                            BoardContainer(
                                failed,
                                stringResource(R.string.not_started),
                                radius,
                                notStartedItems,
                                { navController.navigate(NavRoutes.NewProgressItem.withArgs(workspaceId, it.itemId)) }
                            ) { }
                        }
                    }
                    if (inProgressItems.isNotEmpty()) {
                        item {
                            BoardContainer(
                                pending,
                                stringResource(R.string.in_progress),
                                radius,
                                inProgressItems,
                                { navController.navigate(NavRoutes.NewProgressItem.withArgs(workspaceId, it.itemId)) }
                            ) {  }
                        }
                    }
                    if (completedItems.isNotEmpty()) {
                        item {
                            BoardContainer(
                                completed,
                                stringResource(R.string.Completed),
                                radius,
                                completedItems,
                                { navController.navigate(NavRoutes.NewProgressItem.withArgs(workspaceId, it.itemId)) }
                            ) {  }
                        }
                        }
                    }
                }
        }
    }
}