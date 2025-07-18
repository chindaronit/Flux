package com.flux.ui.screens.workspaces

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.WorkspaceModel
import com.flux.navigation.NavRoutes
import com.flux.ui.components.EmptySpaces
import com.flux.ui.components.NewWorkspaceBottomSheet
import com.flux.ui.components.PinnedSpacesCard
import com.flux.ui.components.SetPasskeyDialog
import com.flux.ui.components.WorkSpacesCard
import com.flux.ui.components.WorkspaceSearchBar
import com.flux.ui.components.shapeManager
import com.flux.ui.events.HabitEvents
import com.flux.ui.events.JournalEvents
import com.flux.ui.events.NotesEvents
import com.flux.ui.events.TaskEvents
import com.flux.ui.events.TodoEvents
import com.flux.ui.events.WorkspaceEvents
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkSpaces(
    snackbarHostState: SnackbarHostState,
    navController: NavController,
    radius: Int,
    allSpaces: List<WorkspaceModel>,
    onNotesEvents: (NotesEvents)->Unit,
    onTaskEvents: (TaskEvents)->Unit,
    onHabitEvents: (HabitEvents)->Unit,
    onTodoEvents: (TodoEvents)->Unit,
    onWorkSpaceEvents: (WorkspaceEvents) -> Unit,
    onJournalEvents: (JournalEvents)->Unit
){
    var query by rememberSaveable { mutableStateOf("") }
    var addWorkspace by remember { mutableStateOf(false) }
    var lockedWorkspace by remember { mutableStateOf<WorkspaceModel?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context= LocalContext.current

    lockedWorkspace?.let { it ->
        SetPasskeyDialog(onConfirmRequest = {passkey->
            if(it.passKey==passkey){
                onNotesEvents(NotesEvents.LoadAllNotes(it.workspaceId))
                onNotesEvents(NotesEvents.LoadAllLabels(it.workspaceId))
                onTaskEvents(TaskEvents.LoadAllInstances(it.workspaceId))
                onTaskEvents(TaskEvents.LoadAllTask(it.workspaceId))
                onTaskEvents(TaskEvents.LoadTodayTask(it.workspaceId))
                onHabitEvents(HabitEvents.LoadAllHabits(it.workspaceId))
                onHabitEvents(HabitEvents.LoadAllInstances(it.workspaceId))
                onTodoEvents(TodoEvents.LoadAllLists(it.workspaceId))
                onJournalEvents(JournalEvents.LoadInitialEntries(it.workspaceId))
                navController.navigate(NavRoutes.WorkspaceHome.withArgs(it.workspaceId))
            }
            else{ Toast.makeText(context, "Wrong Passkey", Toast.LENGTH_SHORT).show() }
        }) { lockedWorkspace=null }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        floatingActionButton = { FloatingActionButton(onClick = { addWorkspace=true } ) { Icon(Icons.Default.Add, null) } },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding->
        Column(Modifier.padding(innerPadding)) {
            WorkspaceSearchBar(
                TextFieldState(query),
                { query = it },
                allSpaces.filter { it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) },
                { navController.navigate(NavRoutes.Settings.route) },
                { query = "" },
                onWorkSpaceEvents,
                onClick = { space->
                    if (space.passKey.isNotBlank()) { lockedWorkspace = space }
                    else {
                        onNotesEvents(NotesEvents.LoadAllNotes(space.workspaceId))
                        onNotesEvents(NotesEvents.LoadAllLabels(space.workspaceId))
                        onTaskEvents(TaskEvents.LoadAllInstances(space.workspaceId))
                        onTaskEvents(TaskEvents.LoadAllTask(space.workspaceId))
                        onTaskEvents(TaskEvents.LoadTodayTask(space.workspaceId))
                        onHabitEvents(HabitEvents.LoadAllHabits(space.workspaceId))
                        onHabitEvents(HabitEvents.LoadAllInstances(space.workspaceId))
                        onTodoEvents(TodoEvents.LoadAllLists(space.workspaceId))
                        onJournalEvents(JournalEvents.LoadInitialEntries(space.workspaceId))
                        navController.navigate(NavRoutes.WorkspaceHome.withArgs(space.workspaceId))
                    }
                }
            )
            if(allSpaces.isEmpty()){ EmptySpaces() }
            else {
                if(allSpaces.any { it.isPinned }){ Text(stringResource(R.string.Pinned), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).padding(top = 8.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                LazyRow(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(allSpaces.filter { it.isPinned }) { space->
                        PinnedSpacesCard(
                            radius, space.passKey.isNotBlank(), space.cover, space.title,
                            onClick = {
                                if(space.passKey.isNotBlank()){ lockedWorkspace=space }
                                else{
                                    onNotesEvents(NotesEvents.LoadAllNotes(space.workspaceId))
                                    onNotesEvents(NotesEvents.LoadAllLabels(space.workspaceId))
                                    onTaskEvents(TaskEvents.LoadAllInstances(space.workspaceId))
                                    onTaskEvents(TaskEvents.LoadAllTask(space.workspaceId))
                                    onTaskEvents(TaskEvents.LoadTodayTask(space.workspaceId))
                                    onHabitEvents(HabitEvents.LoadAllHabits(space.workspaceId))
                                    onHabitEvents(HabitEvents.LoadAllInstances(space.workspaceId))
                                    onTodoEvents(TodoEvents.LoadAllLists(space.workspaceId))
                                    onJournalEvents(JournalEvents.LoadInitialEntries(space.workspaceId))
                                    navController.navigate(NavRoutes.WorkspaceHome.withArgs(space.workspaceId))
                                }
                            }
                        )
                    }
                }
                ElevatedCard(
                    shape = shapeManager(radius=radius*2),
                    modifier = Modifier.padding(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp))
                ) {
                    LazyColumn {
                        itemsIndexed(allSpaces) { index, space ->
                            WorkSpacesCard(
                                workspace = space,
                                onClick = {
                                    if (space.passKey.isNotBlank()) { lockedWorkspace = space }
                                    else {
                                        onJournalEvents(JournalEvents.LoadInitialEntries(space.workspaceId))
                                        onNotesEvents(NotesEvents.LoadAllNotes(space.workspaceId))
                                        onNotesEvents(NotesEvents.LoadAllLabels(space.workspaceId))
                                        onTaskEvents(TaskEvents.LoadAllInstances(space.workspaceId))
                                        onTaskEvents(TaskEvents.LoadAllTask(space.workspaceId))
                                        onTaskEvents(TaskEvents.LoadTodayTask(space.workspaceId))
                                        onHabitEvents(HabitEvents.LoadAllHabits(space.workspaceId))
                                        onHabitEvents(HabitEvents.LoadAllInstances(space.workspaceId))
                                        onTodoEvents(TodoEvents.LoadAllLists(space.workspaceId))
                                        navController.navigate(NavRoutes.WorkspaceHome.withArgs(space.workspaceId))
                                    }
                                },
                                onWorkspaceEvents = onWorkSpaceEvents
                            )
                            if (index != allSpaces.lastIndex) {
                                HorizontalDivider(modifier = Modifier.alpha(0.4f))
                            }
                        }
                    }
                }
            }
        }

        NewWorkspaceBottomSheet(isVisible = addWorkspace, sheetState = sheetState, onDismiss = {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) {
                    addWorkspace=false
                }
            }
        }, onConfirm = { onWorkSpaceEvents(WorkspaceEvents.UpsertSpace(it)) })
    }
}