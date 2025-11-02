package com.flux.ui.screens.workspaces

import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
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
import androidx.compose.ui.res.stringResource
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
import com.flux.data.model.TodoModel
import com.flux.data.model.WorkspaceModel
import com.flux.data.model.getSpacesList
import com.flux.navigation.NavRoutes
import com.flux.other.icons
import com.flux.ui.components.AddNewSpacesBottomSheet
import com.flux.ui.components.CalendarToolBar
import com.flux.ui.components.ChangeIconBottomSheet
import com.flux.ui.components.DeleteAlert
import com.flux.ui.components.EventToolBar
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
import java.time.YearMonth
import com.flux.R
import com.mohamedrejeb.richeditor.model.rememberRichTextState

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
    selectedDate: Long,
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
    val is24HourFormat = settings.data.is24HourFormat
    val workspaceId = workspace.workspaceId
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val selectedSpaceId = rememberSaveable { mutableIntStateOf(if (workspace.selectedSpaces.isEmpty()) -1 else workspace.selectedSpaces.first()) }
    var editWorkspaceDialog by remember { mutableStateOf(false) }
    var editIconSheet by remember { mutableStateOf(false) }
    var showSpacesMenu by remember { mutableStateOf(false) }
    var showDeleteWorkspaceDialog by remember { mutableStateOf(false) }
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
    val spacesList = getSpacesList()
    val richTextState = rememberRichTextState()

    val importNoteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""

                // Get filename (remove extension)
                val fileName = uri.lastPathSegment
                    ?.substringAfterLast("/")
                    ?.substringBeforeLast(".")
                    ?: "Imported Note"

                richTextState.setMarkdown(content)

                // Create a new note with parsed HTML
                val newNote = NotesModel(
                    title = fileName,
                    description = richTextState.toHtml(),
                    workspaceId = workspaceId,
                    lastEdited = System.currentTimeMillis()
                )

                onNotesEvents(NotesEvents.UpsertNote(newNote))
                Toast.makeText(context, context.getString(R.string.import_success), Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, context.getString(R.string.import_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (workspace.description.isNotBlank()) {
                item { Text(workspace.description, style = MaterialTheme.typography.bodyLarge) }
            }
            item {
                if (spacesList.find { it.id == selectedSpaceId.intValue }?.title == stringResource(R.string.Notes) && selectedNotes.isNotEmpty()) {
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
                        SpacesToolBar(spacesList.find { it.id == selectedSpaceId.intValue }?.title ?: "", spacesList.find { it.id == selectedSpaceId.intValue }?.icon
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

                        if (spacesList.find { it.id == selectedSpaceId.intValue }?.title == stringResource(R.string.Habits)) {
                            HabitToolBar(context) { navController.navigate(NavRoutes.NewHabit.withArgs(workspaceId, "")) }
                        }
                        if (spacesList.find { it.id == selectedSpaceId.intValue }?.title == stringResource(R.string.Notes)) {
                            NotesToolBar(
                                navController,
                                workspaceId,
                                query, settings.data.isGridView,
                                onSearch = { query = it },
                                onImportNote = { importNoteLauncher.launch(arrayOf("text/markdown", "text/plain")) },
                                onChangeView = {
                                    onSettingEvents(
                                        SettingEvents.UpdateSettings(
                                            settings.data.copy(isGridView = !settings.data.isGridView)
                                        )
                                    )
                                })
                        }
                        if (spacesList.find { it.id == selectedSpaceId.intValue }?.title == stringResource(R.string.Journal)) {
                            JournalToolBar(navController, workspaceId)
                        }
                        if (spacesList.find { it.id == selectedSpaceId.intValue }?.title == stringResource(R.string.To_Do)) {
                            TodoToolBar(navController, workspaceId)
                        }
                        if (spacesList.find { it.id == selectedSpaceId.intValue }?.title == stringResource(R.string.Calendar)) {
                            CalendarToolBar(
                                navController,
                                workspaceId,
                                context,
                                selectedDate,
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
                        if (spacesList.find { it.id == selectedSpaceId.intValue }?.title == stringResource(R.string.Events)) {
                            EventToolBar(context, navController, workspaceId)
                        }
                    }
                }
            }
            if (spacesList.find { it.id == selectedSpaceId.intValue }?.title == context.getString(R.string.Habits)) {
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
            if (spacesList.find { it.id == selectedSpaceId.intValue }?.title == context.getString(R.string.Notes)) {
                notesHomeItems(
                    navController,
                    workspaceId,
                    selectedNotes,
                    query,
                    settings.data.cornerRadius,
                    isGridView = settings.data.isGridView,
                    allLabels,
                    isNotesLoading,
                    allNotes,
                    onNotesEvents
                )
            }
            if (spacesList.find { it.id == selectedSpaceId.intValue }?.title == context.getString(R.string.Calendar)) {
                calendarItems(
                    navController,
                    radius,
                    is24HourFormat,
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
            if (spacesList.find { it.id == selectedSpaceId.intValue }?.title == context.getString(R.string.Journal)) {
                journalHomeItems(navController, isJournalEntriesLoading, workspaceId, allEntries)
            }
            if (spacesList.find { it.id == selectedSpaceId.intValue }?.title == context.getString(R.string.Analytics)) {
                analyticsItems(
                    workspace,
                    radius,
                    allHabitInstances,
                    totalHabits = allHabits.size,
                    totalNotes = allNotes.size,
                    allEntries,
                    allHabits,
                    allEvents,
                    allEventInstances
                )
            }
            if (spacesList.find { it.id == selectedSpaceId.intValue }?.title == context.getString(R.string.To_Do)) {
                todoHomeItems(
                    navController,
                    radius,
                    allLists,
                    workspaceId,
                    isTodoLoading,
                    onTodoEvents
                )
            }
            if (spacesList.find { it.id == selectedSpaceId.intValue }?.title == context.getString(R.string.Events)) {
                eventHomeItems(
                    navController,
                    radius,
                    is24HourFormat,
                    isAllEventsLoading,
                    allEvents,
                    allEventInstances,
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
        selectedSpaces = spacesList.filter { workspace.selectedSpaces.contains(it.id) },
        onDismiss = { addSpaceBottomSheet = false },
        onRemove = { spaceId ->
            val newSelected = workspace.selectedSpaces.firstOrNull { it != spaceId } ?: -1
            selectedSpaceId.intValue = newSelected

            onWorkspaceEvents(
                WorkspaceEvents.UpsertSpace(
                    workspace.copy(selectedSpaces = workspace.selectedSpaces.minus(spaceId))
                )
            )

            removeSpaceData(
                workspaceId, spaceId, context, onTaskEvents, onTodoEvents,
                onHabitEvents, onNotesEvents, onJournalEvents
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