package com.flux.ui.screens.workspaces

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.data.model.EventInstanceModel
import com.flux.data.model.EventModel
import com.flux.data.model.HabitInstanceModel
import com.flux.data.model.HabitModel
import com.flux.data.model.JournalModel
import com.flux.data.model.LabelModel
import com.flux.data.model.NotesModel
import com.flux.data.model.SpacesList
import com.flux.data.model.TodoModel
import com.flux.data.model.WorkspaceModel
import com.flux.navigation.NavRoutes
import com.flux.other.icons
import com.flux.ui.components.AddNewSpacesBottomSheet
import com.flux.ui.components.CalendarToolBar
import com.flux.ui.components.ChangeIconBottomSheet
import com.flux.ui.components.DeleteAlert
import com.flux.ui.components.EventToolBar
import com.flux.ui.components.HabitBottomSheet
import com.flux.ui.components.HabitToolBar
import com.flux.ui.components.JournalToolBar
import com.flux.ui.components.NewWorkspaceBottomSheet
import com.flux.ui.components.NotesToolBar
import com.flux.ui.components.SelectedBar
import com.flux.ui.components.SetPasskeyDialog
import com.flux.ui.components.SpacesMenu
import com.flux.ui.components.SpacesToolBar
import com.flux.ui.components.TodoToolBar
import com.flux.ui.components.WorkspaceTopBar
import com.flux.ui.events.HabitEvents
import com.flux.ui.events.JournalEvents
import com.flux.ui.events.NotesEvents
import com.flux.ui.events.SettingEvents
import com.flux.ui.events.TaskEvents
import com.flux.ui.events.TodoEvents
import com.flux.ui.events.WorkspaceEvents
import com.flux.ui.screens.analytics.analyticsItems
import com.flux.ui.screens.calendar.calendarItems
import com.flux.ui.screens.events.eventHomeItems
import com.flux.ui.screens.habits.habitsHomeItems
import com.flux.ui.screens.journal.journalHomeItems
import com.flux.ui.screens.notes.notesHomeItems
import com.flux.ui.screens.todo.todoHomeItems
import com.flux.ui.state.Settings
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.YearMonth

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceDetails(
    navController: NavController,
    allLabels: List<LabelModel>,
    settings: Settings,
    isNotesLoading: Boolean,
    isAllEventsLoading: Boolean,
    isDatedTaskLoading: Boolean,
    isTodoLoading: Boolean,
    isJournalEntriesLoading: Boolean,
    isHabitLoading: Boolean,
    workspace: WorkspaceModel,
    allEvents: List<EventModel>,
    allNotes: List<NotesModel>,
    selectedNotes: List<String>,
    selectedYearMonth: YearMonth,
    selectedDate: LocalDate,
    datedEvents: List<EventModel>,
    allHabits: List<HabitModel>,
    allLists: List<TodoModel>,
    allEntries: List<JournalModel>,
    allHabitInstances: List<HabitInstanceModel>,
    allEventInstances: List<EventInstanceModel>,
    onWorkspaceEvents: (WorkspaceEvents) -> Unit,
    onNotesEvents: (NotesEvents) -> Unit,
    onTaskEvents: (TaskEvents) -> Unit,
    onHabitEvents: (HabitEvents) -> Unit,
    onTodoEvents: (TodoEvents) -> Unit,
    onJournalEvents: (JournalEvents) -> Unit,
    onSettingEvents: (SettingEvents) -> Unit,
) {
    val radius = settings.data.cornerRadius
    val workspaceId = workspace.workspaceId
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val selectedSpaceId =
        rememberSaveable { mutableIntStateOf(if (workspace.selectedSpaces.isEmpty()) -1 else workspace.selectedSpaces.first()) }
    var editWorkspaceDialog by remember { mutableStateOf(false) }
    var editIconSheet by remember { mutableStateOf(false) }
    var showSpacesMenu by remember { mutableStateOf(false) }
    var showDeleteWorkspaceDialog by remember { mutableStateOf(false) }
    var showHabitDialog by remember { mutableStateOf(false) }
    var showLockDialog by remember { mutableStateOf(false) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                onWorkspaceEvents(
                    WorkspaceEvents.UpsertSpace(
                        workspace.copy(
                            cover = copyToInternalStorage(context, uri).toString()
                        )
                    )
                )
            }
        }
    )
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var addSpaceBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            WorkspaceTopBar(
                workspace,
                onBackPressed = { navController.popBackStack() },
                onDelete = { showDeleteWorkspaceDialog = true },
                onTogglePinned = {
                    onWorkspaceEvents(
                        WorkspaceEvents.UpsertSpace(
                            workspace.copy(
                                isPinned = !workspace.isPinned
                            )
                        )
                    )
                },
                onToggleLock = {
                    if (workspace.passKey.isNotBlank()) {
                        onWorkspaceEvents(WorkspaceEvents.UpsertSpace(workspace.copy(passKey = "")))
                    } else showLockDialog = true
                },
                onAddCover = { imagePickerLauncher.launch("image/*") },
                onEditDetails = { editWorkspaceDialog = true },
                onEditLabel = { navController.navigate(NavRoutes.EditLabels.withArgs(workspaceId)) },
                onRemoveCover = { onWorkspaceEvents(WorkspaceEvents.UpsertSpace(workspace.copy(cover = ""))) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            Modifier
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            item {
                IconButton(onClick = { editIconSheet = true }) {
                    Icon(
                        icons[workspace.icon],
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            item {
                Text(
                    workspace.title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (workspace.description.isNotBlank()) {
                item { Text(workspace.description, style = MaterialTheme.typography.bodyLarge) }
            }
            item {
                if (SpacesList.find { it.id == selectedSpaceId.intValue }?.title == "Notes" && selectedNotes.isNotEmpty()) {
                    SelectedBar(
                        true,
                        allNotes.size == selectedNotes.size,
                        allNotes.filter { selectedNotes.contains(it.notesId) }.all { it.isPinned },
                        selectedNotes.size,
                        onPinClick = {
                            onNotesEvents(NotesEvents.TogglePinMultiple(allNotes.filter {
                                selectedNotes.contains(
                                    it.notesId
                                )
                            }))
                        },
                        onDeleteClick = {
                            onNotesEvents(NotesEvents.DeleteNotes(allNotes.filter {
                                selectedNotes.contains(
                                    it.notesId
                                )
                            }))
                        },
                        onSelectAllClick = {
                            if (allNotes.size == selectedNotes.size) {
                                onNotesEvents(NotesEvents.ClearSelection)
                            } else {
                                onNotesEvents(NotesEvents.SelectAllNotes)
                            }
                        },
                        onCloseClick = { onNotesEvents(NotesEvents.ClearSelection) }
                    )
                } else {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SpacesToolBar(
                            SpacesList.find { it.id == selectedSpaceId.intValue }?.title ?: "",
                            SpacesList.find { it.id == selectedSpaceId.intValue }?.icon
                                ?: Icons.AutoMirrored.Default.Notes,
                            selectedSpaceId.intValue == -1,
                            onMainClick = { showSpacesMenu = true },
                            onEditClick = { addSpaceBottomSheet = true }
                        )
                        SpacesMenu(
                            expanded = showSpacesMenu,
                            workspace = workspace,
                            onConfirm = { newSpaceId -> selectedSpaceId.intValue = newSpaceId }
                        ) { showSpacesMenu = false }

                        if (SpacesList.find { it.id == selectedSpaceId.intValue }?.title == "Habits") {
                            HabitToolBar(context) {
                                showHabitDialog = true
                                scope.launch { sheetState.show() }
                            }
                        }
                        if (SpacesList.find { it.id == selectedSpaceId.intValue }?.title == "Notes") {
                            NotesToolBar(
                                navController,
                                workspaceId,
                                query, settings.data.isGridView,
                                onSearch = { query = it },
                                onChangeView = {
                                    onSettingEvents(
                                        SettingEvents.UpdateSettings(
                                            settings.data.copy(isGridView = !settings.data.isGridView)
                                        )
                                    )
                                })
                        }
                        if (SpacesList.find { it.id == selectedSpaceId.intValue }?.title == "Journal") {
                            JournalToolBar(navController, workspaceId)
                        }
                        if (SpacesList.find { it.id == selectedSpaceId.intValue }?.title == "To-Do") {
                            TodoToolBar(navController, workspaceId)
                        }
                        if (SpacesList.find { it.id == selectedSpaceId.intValue }?.title == "Calendar") {
                            CalendarToolBar(
                                settings.data.isCalendarMonthlyView,
                                onClick = {
                                    onSettingEvents(
                                        SettingEvents.UpdateSettings(
                                            settings.data.copy(
                                                isCalendarMonthlyView = it
                                            )
                                        )
                                    )
                                }
                            )
                        }
                        if (SpacesList.find { it.id == selectedSpaceId.intValue }?.title == "Events") {
                            EventToolBar(context, navController, workspaceId)
                        }
                    }
                }
            }
            if (SpacesList.find { it.id == selectedSpaceId.intValue }?.title == "Habits") {
                habitsHomeItems(
                    navController,
                    isHabitLoading,
                    radius,
                    workspace.workspaceId,
                    allHabits,
                    allHabitInstances,
                    settings,
                    onHabitEvents
                )
            }
            if (SpacesList.find { it.id == selectedSpaceId.intValue }?.title == "Notes") {
                notesHomeItems(
                    navController,
                    workspaceId,
                    selectedNotes,
                    query,
                    settings.data.cornerRadius,
                    allLabels,
                    isNotesLoading,
                    allNotes,
                    onNotesEvents
                )
            }
            if (SpacesList.find { it.id == selectedSpaceId.intValue }?.title == "Calendar") {
                calendarItems(
                    navController,
                    radius,
                    isDatedTaskLoading,
                    workspaceId,
                    selectedYearMonth,
                    selectedDate,
                    settings,
                    datedEvents,
                    allEventInstances,
                    onTaskEvents
                )
            }
            if (SpacesList.find { it.id == selectedSpaceId.intValue }?.title == "Journal") {
                journalHomeItems(navController, isJournalEntriesLoading, workspaceId, allEntries)
            }
            if (SpacesList.find { it.id == selectedSpaceId.intValue }?.title == "Analytics") {
                analyticsItems(
                    workspace,
                    radius,
                    allHabitInstances,
                    totalHabits = allHabits.size,
                    totalNotes = allNotes.size,
                    allEntries,
                    allEvents,
                    allEventInstances
                )
            }
            if (SpacesList.find { it.id == selectedSpaceId.intValue }?.title == "To-Do") {
                todoHomeItems(
                    navController,
                    radius,
                    allLists,
                    workspaceId,
                    isTodoLoading,
                    onTodoEvents
                )
            }
            if (SpacesList.find { it.id == selectedSpaceId.intValue }?.title == "Events") {
                eventHomeItems(
                    navController,
                    radius,
                    isAllEventsLoading,
                    allEvents,
                    allEventInstances,
                    settings,
                    workspaceId,
                    onTaskEvents
                )
            }
        }
    }

    if (showLockDialog) {
        SetPasskeyDialog({ onWorkspaceEvents(WorkspaceEvents.UpsertSpace(workspace.copy(passKey = it))) })
        { showLockDialog = false }
    }

    if (showDeleteWorkspaceDialog) {
        DeleteAlert(onConfirmation = {
            showDeleteWorkspaceDialog = false
            navController.popBackStack()
            onWorkspaceEvents(WorkspaceEvents.DeleteSpace(workspace))
            onNotesEvents(NotesEvents.DeleteAllWorkspaceNotes(workspaceId))
            onTodoEvents(TodoEvents.DeleteAllWorkspaceLists(workspaceId))
            onTaskEvents(TaskEvents.DeleteAllWorkspaceEvents(workspaceId, context))
            onHabitEvents(HabitEvents.DeleteAllWorkspaceHabits(workspaceId, context))
        }, onDismissRequest = {
            showDeleteWorkspaceDialog = false
        })
    }

    AddNewSpacesBottomSheet(
        isVisible = addSpaceBottomSheet,
        sheetState = sheetState,
        selectedSpaces = SpacesList.filter { workspace.selectedSpaces.contains(it.id) },
        onDismiss = { addSpaceBottomSheet = false },
        onRemove = {
            if (workspace.selectedSpaces.size == 1) selectedSpaceId.intValue = -1
            else selectedSpaceId.intValue = workspace.selectedSpaces.first()
            onWorkspaceEvents(
                WorkspaceEvents.UpsertSpace(
                    workspace.copy(selectedSpaces = workspace.selectedSpaces.minus(it))
                )
            )
            removeSpaceData(
                workspaceId,
                it,
                context,
                onTaskEvents,
                onTodoEvents,
                onHabitEvents,
                onNotesEvents,
                onJournalEvents
            )
        },
        onSelect = {
            if (selectedSpaceId.intValue == -1) selectedSpaceId.intValue = it
            onWorkspaceEvents(
                WorkspaceEvents.UpsertSpace(
                    workspace.copy(
                        selectedSpaces = workspace.selectedSpaces.plus(
                            it
                        )
                    )
                )
            )
        }
    )

    // Edit Workspace Sheet
    NewWorkspaceBottomSheet(
        isEditing = true,
        workspace = workspace,
        isVisible = editWorkspaceDialog,
        sheetState = sheetState,
        onDismiss = {
            scope.launch { sheetState.hide() }.invokeOnCompletion { editWorkspaceDialog = false }
        },
        onConfirm = {
            onWorkspaceEvents(WorkspaceEvents.UpsertSpace(it))
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                editWorkspaceDialog = false
            }
        }
    )

    // Habit Bottom Sheet — always composed
    HabitBottomSheet(
        settings = settings,
        isVisible = showHabitDialog,
        sheetState = sheetState,
        onDismissRequest = {
            scope.launch { sheetState.hide() }.invokeOnCompletion { showHabitDialog = false }
        },
        onConfirm = { newHabit, adjustedTime ->
            onHabitEvents(
                HabitEvents.UpsertHabit(
                    context,
                    newHabit.copy(workspaceId = workspaceId),
                    adjustedTime
                )
            )
            scope.launch { sheetState.hide() }.invokeOnCompletion { showHabitDialog = false }
        }
    )

    // Edit Workspace Sheet
    ChangeIconBottomSheet(
        isVisible = editIconSheet,
        sheetState = sheetState,
        onDismiss = {
            scope.launch { sheetState.hide() }.invokeOnCompletion { editIconSheet = false }
        },
        onConfirm = { index ->
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                onWorkspaceEvents(WorkspaceEvents.UpsertSpace(workspace.copy(icon = index)))
                editIconSheet = false
            }
        }
    )
}

fun copyToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val fileName = "img_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        inputStream?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun removeSpaceData(
    workspaceId: String,
    spaceId: Int,
    context: Context,
    onTaskEvents: (TaskEvents) -> Unit,
    onTodoEvents: (TodoEvents) -> Unit,
    onHabitEvents: (HabitEvents) -> Unit,
    onNotesEvents: (NotesEvents) -> Unit,
    onJournalEvents: (JournalEvents) -> Unit
) {
    when (spaceId) {
        1 -> onNotesEvents(NotesEvents.DeleteAllWorkspaceNotes(workspaceId))
        2 -> onTodoEvents(TodoEvents.DeleteAllWorkspaceLists(workspaceId))
        3 -> onTaskEvents(TaskEvents.DeleteAllWorkspaceEvents(workspaceId, context))
        5 -> onJournalEvents(JournalEvents.DeleteWorkspaceEntries(workspaceId))
        6 -> onHabitEvents(HabitEvents.DeleteAllWorkspaceHabits(workspaceId, context))
    }
}