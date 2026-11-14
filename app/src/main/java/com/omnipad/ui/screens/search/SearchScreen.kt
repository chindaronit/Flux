package com.omnipad.ui.screens.search

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.omnipad.R
import com.omnipad.data.model.EventExceptionModel
import com.omnipad.data.model.EventOccurrence
import com.omnipad.data.model.EventModel
import com.omnipad.data.model.filterCompletedEventOccurrences
import com.omnipad.data.model.findEventOccurrences
import com.omnipad.data.model.initialEventOccurrenceIndex
import com.omnipad.data.model.HabitInstanceModel
import com.omnipad.data.model.HabitModel
import com.omnipad.data.model.JournalModel
import com.omnipad.data.model.LabelModel
import com.omnipad.data.model.NotesModel
import com.omnipad.data.model.ProgressBoardModel
import com.omnipad.data.model.RecurrenceRule
import com.omnipad.data.model.Space
import com.omnipad.data.model.TodoModel
import com.omnipad.data.model.WorkspaceModel
import com.omnipad.data.model.getSpacesList
import com.omnipad.data.model.isCounted
import com.omnipad.data.model.isLive
import com.omnipad.navigation.Loader
import com.omnipad.navigation.NavRoutes
import com.omnipad.other.canScheduleReminder
import com.omnipad.other.isNotificationPermissionGranted
import com.omnipad.other.openAppNotificationSettings
import com.omnipad.other.requestExactAlarmPermission
import com.omnipad.ui.common.BottomBar
import com.omnipad.ui.common.DeleteAlert
import com.omnipad.ui.common.EmptyData
import com.omnipad.ui.common.GeneralSearchBar
import com.omnipad.ui.common.SelectWorkspaceDialog
import com.omnipad.ui.common.convertMillisToDate
import com.omnipad.ui.common.convertMillisToTime
import com.omnipad.ui.events.HabitEvents
import com.omnipad.ui.events.ProgressBoardEvents
import com.omnipad.ui.events.TaskEvents
import com.omnipad.ui.events.TodoEvents
import com.omnipad.ui.events.WorkspaceEvents
import com.omnipad.ui.screens.events.EventCard
import com.omnipad.ui.screens.events.EventCalendar
import com.omnipad.ui.screens.events.EventAgendaDateHeader
import com.omnipad.ui.screens.events.AgendaEventActions
import com.omnipad.ui.screens.events.CalendarFocusRequest
import com.omnipad.ui.screens.events.agendaTodayHeaderIndex
import com.omnipad.ui.screens.events.formatAgendaDate
import com.omnipad.ui.screens.habits.HabitPreviewCard
import com.omnipad.ui.screens.habits.isDateAllowedForHabit
import com.omnipad.ui.screens.journal.JournalCardHeader
import com.omnipad.ui.screens.journal.JournalPreview
import com.omnipad.ui.screens.journal.TimelineBody
import com.omnipad.ui.screens.notes.NotesPreviewCard
import com.omnipad.ui.screens.progressBoard.BoardContainer
import com.omnipad.ui.screens.progressBoard.NewBoardItemSheet
import com.omnipad.ui.screens.todo.TodoExpandableCard
import com.omnipad.ui.state.States
import com.omnipad.ui.theme.completed
import com.omnipad.ui.theme.failed
import com.omnipad.ui.theme.pending
import com.omnipad.ui.viewModel.ViewModels
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.collections.minus
import kotlin.collections.plus

private const val ALL_TYPES = 0
private const val EVENTS_TYPE = 3

internal fun searchResultType(selectedTypeIds: List<Int>): Int =
    selectedTypeIds.singleOrNull() ?: ALL_TYPES

internal fun searchableWorkspaceIds(
    workspaces: List<WorkspaceModel>,
    workspaceId: String? = null
): List<String> = if (workspaceId == null) {
    workspaces.filterNot { it.isLocked }.map { it.workspaceId }
} else {
    workspaces.filter { it.workspaceId == workspaceId }.map { it.workspaceId }
}

internal fun matchingSearchEvents(
    events: List<EventModel>,
    selectedWorkspaceIds: Collection<String>,
    lockedWorkspaceIds: Collection<String>,
    query: String
): List<EventModel> = events.filter {
    it.workspaceId in selectedWorkspaceIds &&
        it.workspaceId !in lockedWorkspaceIds &&
        (query.isBlank() || it.title.contains(query, ignoreCase = true))
}

internal fun searchSectionStartIndices(
    selectedTypes: List<Int>,
    noteCount: Int,
    todoCount: Int,
    eventGroupSizes: List<Int>,
    journalCount: Int,
    currentHabitCount: Int,
    pastHabitCount: Int,
    progressGroupSizes: List<Int>
): Map<Int, Int> {
    var index = 0
    return buildMap {
        selectedTypes.forEach { type ->
            val itemCount = when (type) {
                1 -> noteCount
                2 -> todoCount
                3 -> eventGroupSizes.sum() + eventGroupSizes.count { it > 0 }
                4 -> journalCount
                5 -> currentHabitCount + pastHabitCount + if (pastHabitCount > 0) 1 else 0
                7 -> progressGroupSizes.count { it > 0 }
                else -> 0
            }
            if (itemCount > 0) {
                put(type, index)
                index += 1 + itemCount
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    states: States,
    viewModels: ViewModels,
    workspaceId: String? = null,
    initialSpaceId: Int = -1,
    workspaceHeader: @Composable (() -> Unit)? = null,
    workspaceControls: @Composable (() -> Unit)? = null
){
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    if (states.workspaceState.isLoading) {
        Loader()
        return
    }
    val allSpaces = getSpacesList().filter { it.id!=6 }
    val lockedWorkspace = states.workspaceState.allWorkspaces
        .filter { it.isLocked && it.workspaceId != workspaceId }.map { it.workspaceId }
    var selectedSpaceIds by remember { mutableStateOf(allSpaces.map { it.id }.toSet()) }
    val notesPreviewMode = states.settings.data.notesPreviewMode
    val radius = states.settings.data.cornerRadius
    val is24HourFormat = states.settings.data.is24HourFormat
    val isLoading =
        states.notesState.isLoading || states.labelState.isLoading ||
        states.journalState.isLoading || states.habitState.isLoading ||
        states.eventState.isAllEventsLoading || states.progressBoardState.isLoading ||
        states.todoState.isLoading

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedWorkspaceIds = searchableWorkspaceIds(states.workspaceState.allWorkspaces, workspaceId)
    val selectedSpaces = allSpaces.filter { it.id in selectedSpaceIds }
    val selectedSpace = searchResultType(selectedSpaces.map { it.id })

    val labels = states.labelState.allLabels
        .filterNot { lockedWorkspace.contains(it.workspaceId) }
        .filter { selectedWorkspaceIds.contains(it.workspaceId) }

    val labelMap = remember(labels) { labels.associateBy { it.labelId } }
    var isToolsSheetVisible by remember { mutableStateOf(false) }

    val notes = states.notesState.allNotes
        .filter { selectedWorkspaceIds.contains(it.workspaceId) }
        .filter { note ->
            val matchesText = note.title.contains(query, true) || note.description.contains(query, true)
            val matchesLabel = note.labels.any { labelId -> labelMap[labelId]?.value?.contains(query, true) == true }

            matchesText || matchesLabel
        }
        .filterNot { lockedWorkspace.contains(it.workspaceId) }

    val todoLists = states.todoState.allLists
        .filter { selectedWorkspaceIds.contains(it.workspaceId) }
        .filter { it.title.contains(query, ignoreCase = true) || it.items.any { item -> item.value.contains(query, ignoreCase = true) } }
        .filterNot { lockedWorkspace.contains(it.workspaceId) }

    val expandedTODOIds = rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    val toggleTodo: (String) -> Unit = { id ->
        val todo = todoLists.first { it.id == id }
        if (todo.recurrence is RecurrenceRule.NONE) {
            expandedTODOIds.value = if (id in expandedTODOIds.value) expandedTODOIds.value - id
            else expandedTODOIds.value + id
        } else {
            navController.navigate(NavRoutes.TodoDetail.withArgs(todo.workspaceId, id))
        }
    }

    val journals = states.journalState.data
        .filter { selectedWorkspaceIds.contains(it.workspaceId) }
        .filter { entry ->
            val matchesText = entry.text.contains(query, ignoreCase = true)
            val matchesTime = convertMillisToTime(entry.dateTime).contains(query, ignoreCase = true) ||
                    convertMillisToDate(entry.dateTime).contains(query, ignoreCase = true)

            val matchesLabel = entry.labels.any { labelId -> labelMap[labelId]?.value?.contains(query, true) == true }

            matchesText || matchesLabel || matchesTime
        }.filterNot { lockedWorkspace.contains(it.workspaceId) }

    val habits = states.habitState.allHabits
        .filter { selectedWorkspaceIds.contains(it.workspaceId) }
        .filter {
            it.title.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true)
        }.filterNot { lockedWorkspace.contains(it.workspaceId) }

    val currentHabits = habits.filter { it.isLive() }
    val pastHabits = habits.filter { !it.isLive() }
    val matchingEvents = matchingSearchEvents(
        states.eventState.allEvent, selectedWorkspaceIds, lockedWorkspace, query
    )
    val boardItems = states.progressBoardState.allItems
        .filter { selectedWorkspaceIds.contains(it.workspaceId) }
        .filter {
            it.title.contains(query, ignoreCase = true) ||
            convertMillisToDate(it.startDate).contains(query, ignoreCase = true) ||
            convertMillisToDate(it.endDate).contains(query, ignoreCase = true) }
        .filterNot { lockedWorkspace.contains(it.workspaceId) }

    val notStartedItems = boardItems.filter { it.status == 0 }
    val inProgressItems = boardItems.filter { it.status == 1 }
    val completedItems = boardItems.filter { it.status == 2 }
    var selectedProgressBoardItem by remember { mutableStateOf<ProgressBoardModel?>(null) }
    var showEventCalendar by rememberSaveable { mutableStateOf(false) }
    val isEventCalendar = showEventCalendar && EVENTS_TYPE in selectedSpaceIds
    var eventCalendarMonthOffset by rememberSaveable { mutableIntStateOf(0) }
    var selectedCalendarDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var eventCalendarFocus by remember { mutableStateOf<CalendarFocusRequest?>(null) }
    var eventCalendarFocusSequence by remember { mutableIntStateOf(0) }
    var deletionTarget by remember { mutableStateOf<EventOccurrence?>(null) }
    var showDeleteChoice by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var deleteOccurrence by remember { mutableStateOf(false) }
    val openEventCalendar: (EventOccurrence) -> Unit = { occurrence ->
        selectedCalendarDay = null
        eventCalendarFocusSequence++
        eventCalendarFocus = CalendarFocusRequest(occurrence.event.id, occurrence.date, eventCalendarFocusSequence)
        showEventCalendar = true
    }
    val deleteEvent: (EventOccurrence) -> Unit = { occurrence ->
        deletionTarget = occurrence
        deleteOccurrence = false
        if (occurrence.event.recurrence != RecurrenceRule.Once) showDeleteChoice = true
        else showDeleteConfirmation = true
    }
    var showCreateTypeDialog by remember { mutableStateOf(false) }
    var createSpace by remember { mutableIntStateOf(ALL_TYPES) }
    val scope = rememberCoroutineScope()
    val eventAgendaStart = remember(query, selectedSpace) {
        mutableStateOf(LocalDate.now().minusDays(365))
    }
    val eventAgendaEnd = remember(query, selectedSpace) {
        mutableStateOf(LocalDate.now().plusDays(365))
    }
    val eventSearchListState = rememberLazyListState()
    val matchingEventOccurrences = remember(
        query,
        selectedSpace,
        selectedSpaceIds,
        matchingEvents,
        eventAgendaStart.value,
        eventAgendaEnd.value,
        states.eventState.allEventInstances,
        states.eventState.eventExceptions,
        states.settings.data.showCompletedEvents
    ) {
        if (selectedSpace != EVENTS_TYPE &&
            (selectedSpace != ALL_TYPES || selectedSpaces.none { it.id == EVENTS_TYPE })
        ) emptyList()
        else filterCompletedEventOccurrences(
            findEventOccurrences(
                matchingEvents,
                states.eventState.eventExceptions,
                eventAgendaStart.value,
                eventAgendaEnd.value
            ),
            states.eventState.allEventInstances,
            states.settings.data.showCompletedEvents
        )
    }
    val matchingEventGroups = remember(matchingEventOccurrences) {
        matchingEventOccurrences.groupBy { it.date }.toList()
    }
    val sectionStartIndices = searchSectionStartIndices(
        selectedTypes = selectedSpaces.map { it.id },
        noteCount = notes.size,
        todoCount = todoLists.size,
        eventGroupSizes = matchingEventGroups.map { it.second.size },
        journalCount = journals.size,
        currentHabitCount = currentHabits.size,
        pastHabitCount = pastHabits.size,
        progressGroupSizes = listOf(notStartedItems.size, inProgressItems.size, completedItems.size)
    )
    val currentSectionStartIndices by rememberUpdatedState(sectionStartIndices)
    val populatedSpaces = selectedSpaces.filter { it.id in sectionStartIndices }
    var pendingInitialSpace by rememberSaveable(workspaceId, initialSpaceId) {
        mutableIntStateOf(initialSpaceId)
    }
    LaunchedEffect(isLoading, sectionStartIndices, pendingInitialSpace) {
        if (!isLoading && pendingInitialSpace != -1) {
            sectionStartIndices[pendingInitialSpace]?.let { eventSearchListState.scrollToItem(it) }
            pendingInitialSpace = -1
        }
    }
    val initialMatchingOccurrenceIndex = initialEventOccurrenceIndex(matchingEventOccurrences)
    val initialMatchingListIndex = remember(matchingEventGroups, initialMatchingOccurrenceIndex) {
        val initialOccurrence = matchingEventOccurrences.getOrNull(initialMatchingOccurrenceIndex)
        if (initialOccurrence == null) {
            0
        } else {
            val previousGroupsItemCount = matchingEventGroups
                .takeWhile { (date, _) -> date != initialOccurrence.date }
                .sumOf { (_, occurrences) -> occurrences.size + 1 }
            val occurrenceOffset = matchingEventGroups
                .first { (date, _) -> date == initialOccurrence.date }
                .second
                .indexOf(initialOccurrence)
            previousGroupsItemCount + 1 + occurrenceOffset
        }
    }
    LaunchedEffect(query, selectedSpace, matchingEventOccurrences.isNotEmpty()) {
        if (selectedSpace == EVENTS_TYPE && matchingEventOccurrences.isNotEmpty()) {
            eventSearchListState.scrollToItem(initialMatchingListIndex)
        }
    }
    LaunchedEffect(
        query,
        selectedSpace,
        eventSearchListState,
        matchingEventOccurrences.isNotEmpty(),
        eventAgendaStart.value
    ) {
        snapshotFlow {
            Triple(
                eventSearchListState.firstVisibleItemIndex,
                eventSearchListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0,
                eventSearchListState.isScrollInProgress
            )
        }.distinctUntilChanged().collect { (firstVisible, lastVisible, isScrolling) ->
            if (selectedSpace == EVENTS_TYPE && isScrolling &&
                firstVisible <= 1 &&
                eventAgendaStart.value.isAfter(LocalDate.of(1900, 1, 1))
            ) {
                eventAgendaStart.value = eventAgendaStart.value.minusDays(365)
            }
            if (selectedSpace == EVENTS_TYPE && isScrolling &&
                matchingEventOccurrences.isNotEmpty() &&
                lastVisible >= eventSearchListState.layoutInfo.totalItemsCount - 3
            ) {
                eventAgendaEnd.value = eventAgendaEnd.value.plusDays(365)
            }
        }
    }
    var showWorkspaceSelectorDialog by remember { mutableStateOf(false) }
    val notificationPermissionLabel = stringResource(R.string.Notification_Permission)
    val reminderPermissionLabel = stringResource(R.string.Reminder_Permission)

    LaunchedEffect(query) {
        if(query.isNotBlank() && expandedTODOIds.value.isEmpty()){
            expandedTODOIds.value = todoLists.map { it.id }.toSet()
        }
        else if(query.isBlank()) { expandedTODOIds.value = emptySet() }
    }

    fun hasOrAppendSpace(workspaceId: String, spaceId: Int){
        val workspaces = states.workspaceState.allWorkspaces
        val selectedWorkspace = workspaces.find { it.workspaceId == workspaceId }
        val currentSpaces = selectedWorkspace!!.selectedSpaces

        if(!currentSpaces.contains(spaceId)){
            val updatedWorkspace = selectedWorkspace.copy(selectedSpaces = currentSpaces.plus(spaceId))
            viewModels.workspaceViewModel.onEvent(WorkspaceEvents.UpsertSpace(updatedWorkspace))
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun navigateToCreateNewSpaceEntry(selectedWorkspaceId: String){
        val newId = UUID.randomUUID().toString()
        val localDate = LocalDate.now()
        val currentTime = LocalTime.now()
        val zonedDateTime = ZonedDateTime.of(localDate, currentTime, ZoneId.systemDefault())
        val selectedDateMillis = zonedDateTime.toInstant().toEpochMilli()

        hasOrAppendSpace(selectedWorkspaceId, createSpace)

        when(createSpace){
            1-> navController.navigate(NavRoutes.NoteDetails.withArgs(selectedWorkspaceId, newId))
            2-> navController.navigate(NavRoutes.NewTodoList.withArgs(selectedWorkspaceId, newId))
            3-> navController.navigate(NavRoutes.NewEvent.withArgs(selectedWorkspaceId, newId, selectedDateMillis))
            4-> navController.navigate(NavRoutes.EditJournal.withArgs(selectedWorkspaceId, newId, System.currentTimeMillis()))
            5-> {
                if (!canScheduleReminder(context)) {
                    Toast.makeText(context, reminderPermissionLabel, Toast.LENGTH_SHORT).show()
                    requestExactAlarmPermission(context)
                }
                if (!isNotificationPermissionGranted(context)) {
                    Toast.makeText(context, notificationPermissionLabel, Toast.LENGTH_SHORT).show()
                    openAppNotificationSettings(context)
                }
                if (canScheduleReminder(context) && isNotificationPermissionGranted(context)) {
                    navController.navigate(NavRoutes.NewHabit.withArgs(selectedWorkspaceId, newId))
                }
            }
            7-> navController.navigate(NavRoutes.NewProgressItem.withArgs(selectedWorkspaceId, newId))
            else -> {}
        }

        showWorkspaceSelectorDialog = false
    }

    fun startCreating(spaceId: Int) {
        createSpace = spaceId
        if (workspaceId == null) showWorkspaceSelectorDialog = true
        else navigateToCreateNewSpaceEntry(workspaceId)
    }

    if(showWorkspaceSelectorDialog){
        SelectWorkspaceDialog(
            workspaces = states.workspaceState.allWorkspaces,
            onDismiss = { showWorkspaceSelectorDialog = false },
            onConfirm = {
                navigateToCreateNewSpaceEntry(it)
                showWorkspaceSelectorDialog = false
            }
        )
    }
    if (showCreateTypeDialog) {
        AlertDialog(
            onDismissRequest = { showCreateTypeDialog = false },
            title = { Text(stringResource(R.string.search_choose_create_type)) },
            text = {
                Column {
                    selectedSpaces.forEach { space ->
                        TextButton(onClick = {
                            showCreateTypeDialog = false
                            startCreating(space.id)
                        }) {
                            Icon(space.icon, contentDescription = null)
                            Text(space.title, Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCreateTypeDialog = false }) {
                    Text(stringResource(R.string.Cancel))
                }
            }
        )
    }

    if (showDeleteChoice && deletionTarget != null) {
        AlertDialog(
            onDismissRequest = { showDeleteChoice = false; deletionTarget = null },
            title = { Text(stringResource(R.string.delete_event_scope_title)) },
            text = { Text(stringResource(R.string.delete_event_scope_message)) },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = {
                        deleteOccurrence = true
                        showDeleteChoice = false
                        showDeleteConfirmation = true
                    }) { Text(stringResource(R.string.event_scope_this)) }
                    TextButton(onClick = {
                        deleteOccurrence = false
                        showDeleteChoice = false
                        showDeleteConfirmation = true
                    }) { Text(stringResource(R.string.event_scope_series)) }
                    TextButton(onClick = { showDeleteChoice = false; deletionTarget = null }) {
                        Text(stringResource(R.string.Cancel))
                    }
                }
            }
        )
    }
    val target = deletionTarget
    if (showDeleteConfirmation && target != null) {
        DeleteAlert(
            onDismissRequest = { showDeleteConfirmation = false; deletionTarget = null },
            onConfirmation = {
                if (deleteOccurrence) {
                    viewModels.eventViewModel.onEvent(TaskEvents.DeleteOccurrence(EventExceptionModel(
                        target.event.id, target.instanceDate, target.event.workspaceId
                    )))
                } else {
                    viewModels.eventViewModel.onEvent(TaskEvents.DeleteTask(target.event, context))
                }
                showDeleteConfirmation = false
                deletionTarget = null
            },
            dialogTitle = stringResource(if (deleteOccurrence) R.string.delete_occurrence_title else R.string.deleteDialogTitle),
            dialogText = stringResource(if (deleteOccurrence) R.string.delete_occurrence_message else R.string.deleteDialogText)
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            Column {
                workspaceHeader?.invoke()
                if (!isEventCalendar) {
                    GeneralSearchBar(
                        leadingIcon = Icons.Default.Search,
                        trailingIcon = Icons.Default.FilterList,
                        trailingIconContentDescription = stringResource(R.string.search_filters),
                        windowInsets = if (workspaceId == null) SearchBarDefaults.windowInsets else WindowInsets(0, 0, 0, 0),
                        textFieldState = TextFieldState(query),
                        onSearch = { query = it },
                        onAddClicked = if (selectedSpaces.isEmpty()) null else {
                            {
                                if (selectedSpace == ALL_TYPES) showCreateTypeDialog = true
                                else startCreating(selectedSpace)
                            }
                        },
                        onTrailingIconClicked = { isToolsSheetVisible=true },
                        onCloseClicked = { query = "" }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            when {
                isLoading -> Loader()
                else -> {
                    Column(Modifier.fillMaxSize()) {
                        workspaceControls?.invoke()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!isEventCalendar) {
                                    val scopeLabel = when {
                                        selectedSpaces.isEmpty() -> stringResource(R.string.search_no_types)
                                        selectedSpaces.size == allSpaces.size -> stringResource(R.string.search_all_types)
                                        selectedSpaces.size == 1 -> selectedSpaces.first().title
                                        else -> stringResource(R.string.search_type_count, selectedSpaces.size)
                                    }
                                    Text(
                                        text = stringResource(R.string.search_scope, scopeLabel),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (selectedCalendarDay == null && (selectedSpace == EVENTS_TYPE || isEventCalendar)) {
                                    FilterChip(
                                        selected = !showEventCalendar,
                                        onClick = { showEventCalendar = false; eventCalendarFocus = null; selectedCalendarDay = null },
                                        label = { Text(stringResource(R.string.event_agenda)) }
                                    )
                                    FilterChip(
                                        selected = showEventCalendar,
                                        onClick = { showEventCalendar = true },
                                        label = { Text(stringResource(R.string.Calendar)) }
                                    )
                                    TextButton(
                                        enabled = showEventCalendar || matchingEventOccurrences.isNotEmpty(),
                                        onClick = {
                                            if (showEventCalendar) {
                                                selectedCalendarDay = null
                                                eventCalendarMonthOffset = 0
                                            }
                                            else agendaTodayHeaderIndex(matchingEventOccurrences, LocalDate.now())?.let { index ->
                                                scope.launch { eventSearchListState.scrollToItem(index) }
                                            }
                                        }
                                    ) {
                                        Text(stringResource(R.string.Today))
                                    }
                                }
                            }
                            if (isEventCalendar) {
                                IconButton(onClick = {
                                    startCreating(EVENTS_TYPE)
                                }) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = stringResource(R.string.add),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        if (!isEventCalendar && selectedSpace == ALL_TYPES && populatedSpaces.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    stringResource(R.string.search_jump_to),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                populatedSpaces.forEach { space ->
                                    val description = stringResource(R.string.search_jump_to_type, space.title)
                                    AssistChip(
                                        onClick = {
                                            scope.launch {
                                                currentSectionStartIndices[space.id]?.let { index ->
                                                    eventSearchListState.scrollToItem(index)
                                                }
                                            }
                                        },
                                        modifier = Modifier.semantics { contentDescription = description },
                                        label = { Text(space.title) }
                                    )
                                }
                            }
                        }
                        if (isEventCalendar) {
                            if (query.isNotBlank() || !states.settings.data.showCompletedEvents) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(R.string.search_calendar_filtered),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (query.isNotBlank()) {
                                        TextButton(onClick = { query = "" }) {
                                            Text(stringResource(R.string.clear))
                                        }
                                    }
                                }
                            }
                            EventCalendar(
                                events = matchingEvents,
                                exceptions = states.eventState.eventExceptions,
                                instances = states.eventState.allEventInstances,
                                occurrenceColors = states.eventState.eventOccurrenceColors,
                                showCompletedEvents = states.settings.data.showCompletedEvents,
                                completedColorArgb = states.settings.data.completedEventColorArgb,
                                monthOffset = eventCalendarMonthOffset,
                                onMonthOffsetChange = { eventCalendarMonthOffset = it },
                                selectedDay = selectedCalendarDay,
                                onSelectedDayChange = { selectedCalendarDay = it },
                                is24HourFormat = is24HourFormat,
                                radius = radius,
                                onEventClick = { occurrence ->
                                    navController.navigate(NavRoutes.eventEditor(occurrence.event, occurrence.instanceDate))
                                },
                                modifier = Modifier.weight(1f).padding(start = 12.dp, end = 12.dp, bottom = if (workspaceId == null) 80.dp else 0.dp),
                                focusRequest = eventCalendarFocus
                            )
                        } else {
                        LazyColumn(
                            state = eventSearchListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                            contentPadding = PaddingValues(
                                top = 16.dp,
                                bottom = if (workspaceId == null) 64.dp else 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (selectedSpace == ALL_TYPES) {
                                if (populatedSpaces.isEmpty()) item { EmptyData() }
                                populatedSpaces.forEach { space ->
                                    item(key = "type:${space.id}") {
                                        Text(
                                            text = space.title,
                                            modifier = Modifier
                                                .padding(top = 8.dp, bottom = 2.dp)
                                                .semantics { heading() },
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                    when (space.id) {
                                        1 -> searchedNotes(navController, notesPreviewMode, radius, notes, labels)
                                        2 -> searchedTodo(navController, radius, context, expandedTODOIds.value, todoLists, toggleTodo, viewModels.todoViewModel::onEvent)
                                        3 -> searchedEvent(
                                            navController, radius, is24HourFormat, matchingEventOccurrences,
                                            states.eventState.allEventInstances, states.eventState.eventOccurrenceColors,
                                            states.settings.data.completedEventColorArgb,
                                            onEvent = viewModels.eventViewModel::onEvent,
                                            onCalendar = openEventCalendar,
                                            onDelete = deleteEvent
                                        )
                                        4 -> searchedJournal(navController, radius, journals, labels)
                                        5 -> searchedHabits(navController, radius, is24HourFormat, currentHabits, pastHabits, states.habitState.allInstances, viewModels.habitViewModel::onEvent)
                                        7 -> searchedProgressBoard(navController, radius, notStartedItems, inProgressItems, completedItems)
                                    }
                                }
                            } else if(selectedSpaces.isEmpty()){ item { EmptyData() } }
                            else when(selectedSpace) {
                                1 -> searchedNotes(navController, notesPreviewMode, radius, notes, labels)
                                2 -> searchedTodo(navController, radius, context, expandedTODOIds.value, todoLists, toggleTodo, viewModels.todoViewModel::onEvent)
                                3 -> searchedEvent(
                                    navController, radius, is24HourFormat, matchingEventOccurrences,
                                    states.eventState.allEventInstances,
                                    states.eventState.eventOccurrenceColors,
                                    states.settings.data.completedEventColorArgb,
                                    onEvent = viewModels.eventViewModel::onEvent,
                                    onCalendar = openEventCalendar,
                                    onDelete = deleteEvent
                                )
                                4 -> searchedJournal(navController, radius, journals, labels)
                                5 -> searchedHabits(navController, radius, is24HourFormat, currentHabits, pastHabits, states.habitState.allInstances, viewModels.habitViewModel::onEvent)
                                7 -> searchedProgressBoard(navController, radius, notStartedItems, inProgressItems, completedItems)
                                else -> {}
                            }
                        }
                        }
                    }
                    if (workspaceId == null) {
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
    }

    selectedProgressBoardItem?.let { item ->
        NewBoardItemSheet(
            true,
            sheetState,
            item,
            { selectedProgressBoardItem = null },
            { viewModels.progressBoardViewModel.onEvent(ProgressBoardEvents.UpsertProgressItem(it)) }
        ) { viewModels.progressBoardViewModel.onEvent(ProgressBoardEvents.DeleteProgressItem(it)) }
    }

    if(isToolsSheetVisible){
        FilterSheet(
            selectedSpaceIds,
            allSpaces,
            sheetState,
            { isToolsSheetVisible=false }
        ) {
            selectedSpaceIds = it
            if (EVENTS_TYPE !in it) {
                showEventCalendar = false
                eventCalendarFocus = null
                selectedCalendarDay = null
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.searchedNotes(navController: NavController, notesPreviewMode: Int, radius: Int, notes: List<NotesModel>, labels: List<LabelModel>){
    if (notes.isEmpty()) item { EmptyData() }
    items(notes, key = { "note:${it.notesId}" }) { note ->
        NotesPreviewCard(
            radius = radius,
            isSelected = false,
            note = note,
            notesPreviewMode = notesPreviewMode,
            labels = labels.filter { note.labels.contains(it.labelId) }.map { it.value },
            onClick = { navController.navigate(NavRoutes.NoteDetails.withArgs(note.workspaceId, note.notesId)) },
            onLongPressed = { navController.navigate(NavRoutes.WorkspaceHome.withArgs(note.workspaceId) + "?spaceId=1") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.searchedJournal(navController: NavController, radius: Int, allEntries: List<JournalModel>, labels: List<LabelModel>){
    if(allEntries.isEmpty()) item { EmptyData() }
    items(allEntries, key = { "journal:${it.journalId}" }) { journal ->
        JournalCardHeader(convertMillisToDate(journal.dateTime) + ", " + convertMillisToTime(journal.dateTime))
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            TimelineBody(isLast = false)
            JournalPreview(
                radius,
                journal.text,
                labels.filter { journal.labels.contains(it.labelId) },{
                    navController.navigate(
                        NavRoutes.EditJournal.withArgs(
                            journal.workspaceId,
                            journal.journalId,
                            0L
                        )
                    )
                }) {
                navController.navigate(NavRoutes.WorkspaceHome.withArgs(journal.workspaceId) + "?spaceId=4")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.searchedHabits(
    navController: NavController,
    radius: Int,
    is24HourFormat: Boolean,
    currentHabits: List<HabitModel>,
    pastHabits: List<HabitModel>,
    instances: List<HabitInstanceModel>,
    onEvent: (HabitEvents) -> Unit
) {
    if((currentHabits+pastHabits).isEmpty())  item { EmptyData() }
    items(currentHabits) { habit ->
        val habitInstances = instances.filter { it.habitId == habit.id }
        HabitPreviewCard(
            radius = radius,
            habit = habit,
            is24HourFormat = is24HourFormat,
            instances = habitInstances,
            onClick = { date ->
                if (isDateAllowedForHabit(habit.recurrence, date)) {
                    val oldInstance = habitInstances.find { it.instanceDate == date }
                    val count = if(habit.isCounted){
                        if(oldInstance!=null) oldInstance.count+1
                        else 1
                    } else 0

                    val newInstance = HabitInstanceModel(
                        instanceDate = date,
                        habitId = habit.id,
                        workspaceId = habit.workspaceId,
                        count = count
                    )
                    onEvent(HabitEvents.UpdateInstance(newInstance, habit.habitConfig))
                }
            },
            onLongPressed = { navController.navigate(NavRoutes.WorkspaceHome.withArgs(habit.workspaceId) + "?spaceId=5") },
            onAnalyticsClicked = { navController.navigate(NavRoutes.HabitDetails.withArgs(habit.workspaceId, habit.id)) }
        )
    }
    if(pastHabits.isNotEmpty()) {
        item {
            Text(
                stringResource(R.string.past_habits),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
    items(pastHabits) { habit ->
        val habitInstances = instances.filter { it.habitId == habit.id }
        HabitPreviewCard (
            radius = radius,
            is24HourFormat = is24HourFormat,
            habit = habit,
            instances = habitInstances,
            onClick = {},
            onLongPressed = { navController.navigate(NavRoutes.WorkspaceHome.withArgs(habit.workspaceId) + "?spaceId=5") },
            onAnalyticsClicked = {
                navController.navigate(
                    NavRoutes.HabitDetails.withArgs(
                        habit.workspaceId,
                        habit.id
                    )
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.searchedEvent(
    navController: NavController,
    radius: Int,
    is24HourFormat: Boolean,
    occurrences: List<EventOccurrence>,
    instances: List<com.omnipad.data.model.EventInstanceModel>,
    occurrenceColors: List<com.omnipad.data.model.EventOccurrenceColorModel>,
    completedColorArgb: Int?,
    onEvent: (TaskEvents) -> Unit,
    onCalendar: (EventOccurrence) -> Unit,
    onDelete: (EventOccurrence) -> Unit
) {
    if (occurrences.isEmpty()) item { EmptyData() }
    occurrences.groupBy { it.date }.forEach { (date, dateOccurrences) ->
        item(key = "date:$date") {
            EventAgendaDateHeader(formatAgendaDate(date))
        }
        items(dateOccurrences, key = { "event:${it.event.id}:${it.instanceDate}" }) { occurrence ->
            val event = occurrence.event
            val isPending = instances.none {
                it.eventId == event.id && it.instanceDate == occurrence.instanceDate
            }
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                TimelineBody(isLast = false)
                Column {
                    AgendaEventActions(
                        isPending = isPending,
                        onCalendar = { onCalendar(occurrence) },
                        onComplete = {
                            onEvent(TaskEvents.ToggleStatus(
                                isPending, event.id, event.workspaceId, occurrence.instanceDate
                            ))
                        },
                        onDelete = { onDelete(occurrence) }
                    ) { trailing ->
                        EventCard(
                            radius = radius,
                            is24HourFormat = is24HourFormat,
                            isPending = isPending,
                            title = event.title,
                            repeat = event.recurrence,
                            startDateTime = event.startDateTime,
                            isAllDay = event.isAllDay,
                            occurrenceDate = occurrence.instanceDate,
                            colorArgb = occurrenceColors.firstOrNull {
                                it.eventId == event.id && it.occurrenceDate == occurrence.instanceDate
                            }?.colorArgb ?: event.colorArgb,
                            completedColorArgb = completedColorArgb,
                            onChangeStatus = {
                                onEvent(TaskEvents.ToggleStatus(
                                    isPending, event.id, event.workspaceId, occurrence.instanceDate
                                ))
                            },
                            onClick = {
                                navController.navigate(NavRoutes.eventEditor(event, occurrence.instanceDate))
                            },
                            onLongPressed = {
                                navController.navigate(NavRoutes.eventEditor(event, occurrence.instanceDate))
                            },
                            trailingContent = trailing
                        )
                    }
                    if (occurrence != dateOccurrences.last()) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.searchedProgressBoard(
    navController: NavController,
    radius: Int,
    notStartedItems: List<ProgressBoardModel>,
    inProgressItems: List<ProgressBoardModel>,
    completedItems: List<ProgressBoardModel>
) {
    if((notStartedItems + inProgressItems + completedItems).isEmpty()) item { EmptyData() }
    if (notStartedItems.isNotEmpty()) {
        item {
            BoardContainer(
                failed,
                stringResource(R.string.not_started),
                radius,
                notStartedItems,
                { navController.navigate(NavRoutes.NewProgressItem.withArgs(it.workspaceId, it.itemId)) }
            ) { navController.navigate(NavRoutes.WorkspaceHome.withArgs(it.workspaceId) + "?spaceId=7") }
        }
    }
    if (inProgressItems.isNotEmpty()) {
        item {
            BoardContainer(
                pending,
                stringResource(R.string.in_progress),
                radius,
                inProgressItems,
                { navController.navigate(NavRoutes.NewProgressItem.withArgs(it.workspaceId, it.itemId)) }
            ) { navController.navigate(NavRoutes.WorkspaceHome.withArgs(it.workspaceId) + "?spaceId=7") }
        }
    }
    if (completedItems.isNotEmpty()) {
        item {
            BoardContainer(
                completed,
                stringResource(R.string.Completed),
                radius,
                completedItems,
                { navController.navigate(NavRoutes.NewProgressItem.withArgs(it.workspaceId, it.itemId)) }
            ) { navController.navigate(NavRoutes.WorkspaceHome.withArgs(it.workspaceId) + "?spaceId=7") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.searchedTodo(
    navController: NavController,
    radius: Int,
    context: Context,
    expandedTODOIds: Set<String>,
    todoList: List<TodoModel>,
    onExpandToggle: (String) -> Unit,
    onEvent: (TodoEvents) -> Unit
) {
    if(todoList.isEmpty()) item { EmptyData() }
    items(todoList, key = { "todo:${it.id}" }) { todoItem ->
        TodoExpandableCard(
            navController = navController,
            radius = radius,
            item = todoItem,
            context = context,
            workspaceId = todoItem.workspaceId,
            isExpanded = todoItem.id in expandedTODOIds,
            onExpandToggle = onExpandToggle,
            onTodoEvents = onEvent,
            isReordering = false,
            onLongPressed = { navController.navigate(NavRoutes.WorkspaceHome.withArgs(todoItem.workspaceId) + "?spaceId=2") },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    selectedSpaceIds: Set<Int>,
    spaces: List<Space>,
    sheetState: SheetState,
    onDismiss: () -> Unit = {},
    onApply: (Set<Int>) -> Unit
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val maxHeight = with(density) { windowInfo.containerSize.height.toDp() } * 0.7f
    var selection by remember { mutableStateOf(selectedSpaceIds) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
        ) {
            Text(
                text = stringResource(R.string.search_filter_title),
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.titleLarge
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { selection = spaces.map { it.id }.toSet() }) {
                    Text(stringResource(R.string.search_select_all))
                }
                TextButton(onClick = { selection = emptySet() }) {
                    Text(stringResource(R.string.search_clear_all))
                }
            }
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(spaces, key = { it.id }) { space ->
                    val checked = space.id in selection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = checked,
                                role = Role.Checkbox,
                                onValueChange = { selected ->
                                    selection = if (selected) selection + space.id else selection - space.id
                                }
                            )
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(space.icon, contentDescription = null)
                        Text(space.title, modifier = Modifier.weight(1f))
                        Checkbox(checked = checked, onCheckedChange = null)
                    }
                }
            }
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onDismiss) {
                    Text(stringResource(R.string.Cancel))
                }
                Button(
                    modifier = Modifier.weight(2f),
                    onClick = {
                        onApply(selection)
                        onDismiss()
                    }
                ) { Text(stringResource(R.string.Confirm)) }
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}