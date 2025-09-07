package com.flux.ui.screens.workspaces

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.data.model.WorkspaceModel
import com.flux.navigation.NavRoutes
import com.flux.ui.components.EmptySpaces
import com.flux.ui.components.NewWorkspaceBottomSheet
import com.flux.ui.components.SelectedBar
import com.flux.ui.components.SetPasskeyDialog
import com.flux.ui.components.WorkspaceCard
import com.flux.ui.components.WorkspaceSearchBar
import com.flux.ui.events.HabitEvents
import com.flux.ui.events.JournalEvents
import com.flux.ui.events.NotesEvents
import com.flux.ui.events.TaskEvents
import com.flux.ui.events.TodoEvents
import com.flux.ui.events.WorkspaceEvents
import kotlinx.coroutines.launch
import com.flux.R
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkSpaces(
    snackbarHostState: SnackbarHostState,
    navController: NavController,
    gridColumns: Int,
    radius: Int,
    allSpaces: List<WorkspaceModel>,
    onNotesEvents: (NotesEvents) -> Unit,
    onTaskEvents: (TaskEvents) -> Unit,
    onHabitEvents: (HabitEvents) -> Unit,
    onTodoEvents: (TodoEvents) -> Unit,
    onWorkSpaceEvents: (WorkspaceEvents) -> Unit,
    onJournalEvents: (JournalEvents) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var addWorkspace by remember { mutableStateOf(false) }
    val selectedWorkspace = remember { mutableStateListOf<WorkspaceModel>() }
    var lockedWorkspace by remember { mutableStateOf<WorkspaceModel?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    lockedWorkspace?.let { it ->
        SetPasskeyDialog(onConfirmRequest = { passkey ->
            if (it.passKey == passkey) {
                onNotesEvents(NotesEvents.LoadAllNotes(it.workspaceId))
                onNotesEvents(NotesEvents.LoadAllLabels(it.workspaceId))
                onTaskEvents(TaskEvents.LoadAllInstances(it.workspaceId))
                onTaskEvents(TaskEvents.LoadAllTask(it.workspaceId))
                onTaskEvents(TaskEvents.LoadDateTask(it.workspaceId, LocalDate.now().toEpochDay()))
                onHabitEvents(HabitEvents.LoadAllHabits(it.workspaceId))
                onHabitEvents(HabitEvents.LoadAllInstances(it.workspaceId))
                onTodoEvents(TodoEvents.LoadAllLists(it.workspaceId))
                onJournalEvents(JournalEvents.LoadJournalEntries(it.workspaceId))
                navController.navigate(NavRoutes.WorkspaceHome.withArgs(it.workspaceId))
            } else {
                Toast.makeText(context, context.getString(R.string.Wrong_Passkey), Toast.LENGTH_SHORT).show()
            }
        }) { lockedWorkspace = null }
    }

    fun handleWorkspaceClick(space: WorkspaceModel) {
        if (space.passKey.isNotBlank()) {
            lockedWorkspace = space
        } else {
            onNotesEvents(NotesEvents.LoadAllNotes(space.workspaceId))
            onNotesEvents(NotesEvents.LoadAllLabels(space.workspaceId))
            onTaskEvents(TaskEvents.LoadAllInstances(space.workspaceId))
            onTaskEvents(TaskEvents.LoadAllTask(space.workspaceId))
            onTaskEvents(TaskEvents.LoadDateTask(space.workspaceId, LocalDate.now().toEpochDay()))
            onHabitEvents(HabitEvents.LoadAllHabits(space.workspaceId))
            onHabitEvents(HabitEvents.LoadAllInstances(space.workspaceId))
            onTodoEvents(TodoEvents.LoadAllLists(space.workspaceId))
            onJournalEvents(JournalEvents.LoadJournalEntries(space.workspaceId))
            navController.navigate(NavRoutes.WorkspaceHome.withArgs(space.workspaceId))
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            if (selectedWorkspace.isEmpty()) {
                WorkspaceSearchBar(
                    textFieldState = TextFieldState(query),
                    onSearch = { query = it },
                    onSettingsClicked = { navController.navigate(NavRoutes.Settings.route) },
                    onCloseClicked = { query = "" }
                )
            } else{
                Box(Modifier.padding(top = 42.dp)){
                    SelectedBar(
                        false,
                        selectedWorkspace.containsAll(allSpaces),
                        selectedWorkspace.all { it.isPinned },
                        selectedWorkspace.size,
                        onPinClick = { onWorkSpaceEvents(WorkspaceEvents.UpsertSpaces(selectedWorkspace.toList())) },
                        onDeleteClick = {},
                        onSelectAllClick = {
                            if (selectedWorkspace.containsAll(allSpaces)){ selectedWorkspace.clear() }
                            else {
                                selectedWorkspace.clear()
                                selectedWorkspace.addAll(allSpaces)
                            }
                        },
                        onCloseClick = { selectedWorkspace.clear() }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton({ addWorkspace = true }){
                Icon(Icons.Default.Add, null)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (allSpaces.isEmpty()) {
            EmptySpaces()
        } else {
            val spacing = when (gridColumns) {
                1 -> 6.dp
                2 -> 4.dp
                else -> 2.dp
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(vertical = 16.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                if (allSpaces.none {
                        it.title.contains(
                            query,
                            ignoreCase = true
                        ) || it.description.contains(query, ignoreCase = true)
                    }) {
                    item(span = { GridItemSpan(maxLineSpan) }) { EmptySpaces() }
                }
                if (allSpaces.any {
                        it.isPinned && (it.title.contains(
                            query,
                            ignoreCase = true
                        ) || it.description.contains(query, ignoreCase = true))
                    }) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            stringResource(R.string.Pinned),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
                items(allSpaces.filter {
                    it.isPinned && (it.title.contains(
                        query,
                        ignoreCase = true
                    ) || it.description.contains(query, ignoreCase = true))
                }) { space ->
                    WorkspaceCard(
                        gridColumns = gridColumns,
                        iconIndex = space.icon,
                        radius = radius,
                        isLocked = space.passKey.isNotBlank(),
                        cover = space.cover,
                        title = space.title,
                        description = space.description,
                        isSelected = selectedWorkspace.contains(space),
                        onClick = { handleWorkspaceClick(space) },
                        onLongPressed = {
                            if(selectedWorkspace.contains(space)){
                                selectedWorkspace.remove(space)
                            }
                            else{
                                selectedWorkspace.add(space)
                            }
                        }
                    )
                }
                if (allSpaces.any {
                        it.isPinned && (it.title.contains(
                            query,
                            ignoreCase = true
                        ) || it.description.contains(query, ignoreCase = true))
                    }) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            stringResource(R.string.Others),
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
                items(allSpaces.filter {
                    !it.isPinned && (it.title.contains(
                        query,
                        ignoreCase = true
                    ) || it.description.contains(query, ignoreCase = true))
                }) { space ->
                    WorkspaceCard(
                        gridColumns = gridColumns,
                        iconIndex = space.icon,
                        radius = radius,
                        isLocked = space.passKey.isNotBlank(),
                        cover = space.cover,
                        title = space.title,
                        description = space.description,
                        isSelected = selectedWorkspace.contains(space),
                        onClick = { handleWorkspaceClick(space) },
                        onLongPressed = {
                            if(selectedWorkspace.contains(space)){
                                selectedWorkspace.remove(space)
                            }
                            else{
                                selectedWorkspace.add(space)
                            }
                        }
                    )
                }
            }
        }
    }

    NewWorkspaceBottomSheet(isVisible = addWorkspace, sheetState = sheetState, onDismiss = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                addWorkspace = false
            }
        }
    }, onConfirm = { onWorkSpaceEvents(WorkspaceEvents.UpsertSpace(it)) })
}