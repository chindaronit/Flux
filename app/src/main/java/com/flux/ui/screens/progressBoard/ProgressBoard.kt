package com.flux.ui.screens.progressBoard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.ProgressBoardModel
import com.flux.data.model.WorkspaceModel
import com.flux.navigation.Loader
import com.flux.navigation.NavRoutes
import com.flux.ui.common.EmptyProgressItems
import com.flux.ui.common.SpaceSearchBar
import com.flux.ui.common.SpaceTopBar
import com.flux.ui.common.SpacesMenu
import com.flux.ui.common.convertMillisToDate
import com.flux.ui.events.ProgressBoardEvents
import com.flux.ui.screens.workspaces.SpacesToolBar
import com.flux.ui.state.ProgressBoardState
import com.flux.ui.state.Settings
import com.flux.ui.theme.completed
import com.flux.ui.theme.failed
import com.flux.ui.theme.pending

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
    onEvent: (ProgressBoardEvents) -> Unit
){
    val workspaceId = workspace.workspaceId
    val isLoading = state.isLoading
    val radius = settings.data.cornerRadius
    var query by remember { mutableStateOf("") }
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
    var showSearchBar by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedItem by remember { mutableStateOf<ProgressBoardModel?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            SpaceTopBar(
                scrollBehavior = scrollBehavior,
                title            = workspace.title,
                description      = workspace.description,
                cover            = workspace.cover,
                icon             = workspace.icon,
                isLocked = workspace.passKey!=null,
                onBackPressed = { navController.popBackStack() },
                onAddCover = onAddCover,
                onRemoveCover = onRemoveCover,
                onToggleLock = onToggleLock,
                onDeleteWorkspace = onDeleteWorkspace,
                onEditWorkspace = { navController.navigate(NavRoutes.NewWorkspace.withArgs(workspaceId)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton({ selectedItem = ProgressBoardModel(workspaceId=workspaceId) }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { innerPadding ->
        when {
            isLoading -> Loader()
            else ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(12.dp)
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        if(showSearchBar){ SpaceSearchBar(query, { query = it }, { showSearchBar = false }) }
                        else {
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
                                SpacesMenu(
                                    showSpacesMenu,
                                    workspace,
                                    onSpaceChange
                                ) { showSpacesMenu = false }

                                Row {
                                    IconButton({ showSearchBar = true }) {
                                        Icon(
                                            Icons.Default.Search,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton({ showSearchBar = true }) {
                                        Icon(
                                            Icons.Default.FilterList,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if(boardItems.isEmpty()) item { EmptyProgressItems() }
                    if (notStartedItems.isNotEmpty()) {
                        item {
                            BoardContainer(
                                failed,
                                stringResource(R.string.not_started),
                                radius,
                                notStartedItems
                            ) { selectedItem = it }
                        }
                    }
                    if (inProgressItems.isNotEmpty()) {
                        item {
                            BoardContainer(
                                pending,
                                stringResource(R.string.in_progress),
                                radius,
                                inProgressItems
                            ) { selectedItem = it }
                        }
                    }

                    if (completedItems.isNotEmpty()) {
                        item {
                            BoardContainer(
                                completed,
                                stringResource(R.string.Completed),
                                radius,
                                completedItems
                            ) { selectedItem = it }
                        }
                    }
                }
        }
    }

    selectedItem?.let { item ->
        NewBoardItemSheet(
            true,
            sheetState,
            item,
            { selectedItem = null },
            {
                onEvent(ProgressBoardEvents.UpsertProgressItem(it))
            }) {
            onEvent(ProgressBoardEvents.DeleteProgressItem(it)
            )
        }
    }
}