package com.flux.ui.screens.search

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.EventModel
import com.flux.data.model.HabitInstanceModel
import com.flux.data.model.HabitModel
import com.flux.data.model.JournalModel
import com.flux.data.model.LabelModel
import com.flux.data.model.NotesModel
import com.flux.data.model.ProgressBoardModel
import com.flux.data.model.RecurrenceRule
import com.flux.data.model.Space
import com.flux.data.model.TodoModel
import com.flux.data.model.WorkspaceModel
import com.flux.data.model.getSpacesList
import com.flux.data.model.isCounted
import com.flux.data.model.isLive
import com.flux.navigation.Loader
import com.flux.navigation.NavRoutes
import com.flux.other.computeMonthlyEventDates
import com.flux.ui.common.BottomBar
import com.flux.ui.common.CategoryRow
import com.flux.ui.common.EmptyData
import com.flux.ui.common.GeneralSearchBar
import com.flux.ui.common.MultiOptionRow
import com.flux.ui.common.SearchFilterCategory
import com.flux.ui.common.SearchFilterOption
import com.flux.ui.common.SelectionType
import com.flux.ui.common.convertMillisToDate
import com.flux.ui.common.convertMillisToTime
import com.flux.ui.events.HabitEvents
import com.flux.ui.events.ProgressBoardEvents
import com.flux.ui.events.TaskEvents
import com.flux.ui.events.TodoEvents
import com.flux.ui.screens.events.DailyViewCalendar
import com.flux.ui.screens.events.EventCard
import com.flux.ui.screens.events.MonthlyViewCalendar
import com.flux.ui.screens.habits.HabitPreviewCard
import com.flux.ui.screens.habits.isDateAllowedForHabit
import com.flux.ui.screens.journal.JournalCardHeader
import com.flux.ui.screens.journal.JournalPreview
import com.flux.ui.screens.journal.TimelineBody
import com.flux.ui.screens.notes.NotesPreviewCard
import com.flux.ui.screens.progressBoard.BoardContainer
import com.flux.ui.screens.progressBoard.NewBoardItemSheet
import com.flux.ui.screens.todo.TodoExpandableCard
import com.flux.ui.state.States
import com.flux.ui.theme.completed
import com.flux.ui.theme.failed
import com.flux.ui.theme.pending
import com.flux.ui.viewModel.ViewModels
import java.time.LocalDate
import java.time.YearMonth
import kotlin.collections.minus
import kotlin.collections.plus
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController, states: States, viewModels: ViewModels){
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    val allSpaces = getSpacesList().filter { it.id!=6 }
    val lockedWorkspace = states.workspaceState.allWorkspaces.filter { it.passKey?.isNotBlank()==true }.map { it.workspaceId }
    var filterState by remember {
        mutableStateOf(
            FilterState(
                selectedWorkspaceIds = states.workspaceState.allWorkspaces
                    .filterNot { lockedWorkspace.contains(it.workspaceId) }
                    .map { it.workspaceId }
                    .toSet(),

                selectedSpacesIds = allSpaces
                    .map { it.id.toString() }
                    .toSet()
            )
        )
    }
    val notesPreviewMode = states.settings.data.notesPreviewMode
    val radius = states.settings.data.cornerRadius
    val is24HourFormat = states.settings.data.is24HourFormat
    val isMonthlyView = states.settings.data.isCalendarMonthlyView
    val isLoading =
        states.notesState.isLoading || states.labelState.isLoading ||
        states.journalState.isLoading || states.habitState.isLoading ||
        states.eventState.isAllEventsLoading || states.progressBoardState.isLoading ||
        states.todoState.isLoading

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedWorkspaceIds = states.workspaceState.allWorkspaces
        .filterNot { lockedWorkspace.contains(it.workspaceId) }
        .filter { it.workspaceId in filterState.selectedWorkspaceIds }.map { it.workspaceId }
    val selectedSpaces = allSpaces.filter { it.id.toString() in filterState.selectedSpacesIds }

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
    val selectedDate = states.eventState.selectedDate
    val selectedMonth = states.eventState.selectedYearMonth
    val datedEvents = states.eventState.datedEvents
        .filter { selectedWorkspaceIds.contains(it.workspaceId) }
        .filter { it.title.contains(query, ignoreCase = true)  }
        .filterNot { lockedWorkspace.contains(it.workspaceId) }

    val pendingTasks = datedEvents.filter { task ->
        val instance = states.eventState.allEventInstances
            .find { it.eventId == task.id && it.instanceDate == selectedDate }
        instance == null
    }
    val completedTasks = datedEvents.filter { task ->
        val instance = states.eventState.allEventInstances
            .find { it.eventId == task.id && it.instanceDate == selectedDate }
        instance != null
    }
    val monthlyEventCount =
        computeMonthlyEventDates(
            states.eventState.allEvent
                .filter { selectedWorkspaceIds.contains(it.workspaceId) }
                .filterNot { lockedWorkspace.contains(it.workspaceId) },
            selectedMonth
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
    var selectedSpace by remember { mutableIntStateOf(1) }
    val workspacesLabel = stringResource(R.string.workspaces)
    val spacesLabel = stringResource(R.string.spaces)

    LaunchedEffect(query) {
        if(query.isNotBlank() && expandedTODOIds.value.isEmpty()){
            expandedTODOIds.value = todoLists.map { it.id }.toSet()
        }
        else if(query.isBlank()) { expandedTODOIds.value = emptySet() }
    }

    LaunchedEffect(selectedSpaces) {
        val availableIds = selectedSpaces.filter { it.id != 6 }.map { it.id }
        if (availableIds.isNotEmpty() && selectedSpace !in availableIds) {
            selectedSpace = availableIds.first()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            GeneralSearchBar(
                leadingIcon = Icons.Default.Search,
                trailingIcon = Icons.Default.FilterList,
                textFieldState = TextFieldState(query),
                onSearch = { query = it },
                onTrailingIconClicked = { isToolsSheetVisible=true },
                onCloseClicked = { query = "" }
            )
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                isLoading -> Loader()
                else -> {
                    LazyColumn(
                        Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(
                            top = 16.dp,
                            bottom = 64.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if(selectedSpaces.isEmpty()){ item { EmptyData() } }
                        else {
                            item {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(selectedSpaces){ space->
                                        FilterChip(
                                            onClick = { selectedSpace = space.id },
                                            label = { Text(space.title) },
                                            selected = selectedSpace == space.id,
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = if (selectedSpace == space.id) Icons.Filled.Done else space.icon,
                                                    contentDescription = "Done icon",
                                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                )
                                            },
                                        )
                                    }
                                }
                            }

                            when(selectedSpace.absoluteValue) {
                                1 -> searchedNotes(navController, notesPreviewMode, radius, notes, labels)
                                2 -> searchedTodo(navController, radius, context, expandedTODOIds.value, todoLists, { id->
                                    val todoItem = todoLists.first { it.id == id }
                                    val workspaceId = todoItem.workspaceId

                                    if(todoItem.recurrence is RecurrenceRule.NONE){
                                        expandedTODOIds.value =
                                            if (id in expandedTODOIds.value) expandedTODOIds.value - id
                                            else expandedTODOIds.value + id
                                    }
                                    else{ navController.navigate(NavRoutes.TodoDetail.withArgs(workspaceId, id)) }
                                }, viewModels.todoViewModel::onEvent)
                                3 -> searchedEvent(navController, radius, is24HourFormat,pendingTasks, completedTasks, selectedDate, selectedMonth, isMonthlyView, monthlyEventCount, viewModels.eventViewModel::onEvent)
                                4 -> searchedJournal(navController, radius, journals, labels)
                                5 -> searchedHabits(navController, radius, is24HourFormat, currentHabits, pastHabits, states.habitState.allInstances, viewModels.habitViewModel::onEvent)
                                7 -> searchedProgressBoard(radius, notStartedItems, inProgressItems, completedItems) { selectedProgressBoardItem = it }
                                else -> {}
                            }
                        }
                    }
                    BottomBar(
                        navController = navController,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                    )
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
            filterState,
            states.workspaceState.allWorkspaces.filterNot { lockedWorkspace.contains(it.workspaceId) },
            allSpaces,
            sheetState,
            { isToolsSheetVisible=false }
        ) { map ->

            filterState = FilterState(
                selectedWorkspaceIds = map[workspacesLabel] ?: emptySet(),
                selectedSpacesIds = map[spacesLabel] ?: emptySet()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.searchedNotes(navController: NavController, notesPreviewMode: Int, radius: Int, notes: List<NotesModel>, labels: List<LabelModel>){
    if (notes.isEmpty()) item { EmptyData() }
    items(notes, key = { it.notesId }) { note ->
        NotesPreviewCard(
            radius = radius,
            isSelected = false,
            note = note,
            notesPreviewMode = notesPreviewMode,
            labels = labels.filter { note.labels.contains(it.labelId) }.map { it.value },
            onClick = { navController.navigate(NavRoutes.NoteDetails.withArgs(note.workspaceId, note.notesId)) },
            onLongPressed = { navController.navigate(NavRoutes.NoteDetails.withArgs(note.workspaceId, note.notesId)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.searchedJournal(navController: NavController, radius: Int, allEntries: List<JournalModel>, labels: List<LabelModel>){
    if(allEntries.isEmpty()) item { EmptyData() }
    items(allEntries, key = { it.journalId }) { journal ->
        JournalCardHeader(convertMillisToDate(journal.dateTime) + ", " + convertMillisToTime(journal.dateTime))
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            TimelineBody(isLast = false)
            JournalPreview(
                radius,
                journal.text,
                labels.filter { journal.labels.contains(it.labelId) }) {
                navController.navigate(
                    NavRoutes.EditJournal.withArgs(
                        journal.workspaceId,
                        journal.journalId,
                        0L
                    )
                )
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
    pendingTasks: List<EventModel>,
    completedTasks: List<EventModel>,
    selectedDate: Long,
    selectedMonth: YearMonth,
    isMonthlyView: Boolean,
    monthlyEventCount: Map<LocalDate, Int>,
    onEvent: (TaskEvents) -> Unit
) {
    if (isMonthlyView) {
        item {
            MonthlyViewCalendar(
                selectedMonth, selectedDate, monthlyEventCount,
                onMonthChange = { onEvent(TaskEvents.ChangeMonth(it)) },
                onDateChange = { onEvent(TaskEvents.ChangeDate(it)) }
            )
        }
    } else {
        item {
            DailyViewCalendar(selectedMonth, selectedDate){
                onEvent(TaskEvents.ChangeDate(it))
            }
        }
    }

    if((pendingTasks + completedTasks).isEmpty()) item { EmptyData() }
    if (pendingTasks.isNotEmpty()) {
        items(pendingTasks) { task ->
            EventCard(
                radius = radius,
                is24HourFormat = is24HourFormat,
                isPending = true,
                title = task.title,
                repeat = task.recurrence,
                startDateTime = task.startDateTime,
                onChangeStatus = { onEvent(TaskEvents.ToggleStatus(true, task.id, task.workspaceId, selectedDate)) },
                onClick = { navController.navigate(NavRoutes.EventDetails.withArgs(task.workspaceId, task.id, selectedDate)) }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
    if (completedTasks.isNotEmpty()) {
        items(completedTasks) { task ->
            EventCard(
                radius = radius,
                is24HourFormat = is24HourFormat,
                isPending = false,
                title = task.title,
                repeat = task.recurrence,
                startDateTime = task.startDateTime,
                onChangeStatus = { onEvent(TaskEvents.ToggleStatus(false, task.id, task.workspaceId, selectedDate)) },
                onClick = { navController.navigate(NavRoutes.EventDetails.withArgs(task.workspaceId, task.id, selectedDate)) }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.searchedProgressBoard(
    radius: Int,
    notStartedItems: List<ProgressBoardModel>,
    inProgressItems: List<ProgressBoardModel>,
    completedItems: List<ProgressBoardModel>,
    onSelectProgressBoardItem: (ProgressBoardModel) -> Unit
) {
    if((notStartedItems + inProgressItems + completedItems).isEmpty()) item { EmptyData() }
    if (notStartedItems.isNotEmpty()) {
        item {
            BoardContainer(
                failed,
                stringResource(R.string.not_started),
                radius,
                notStartedItems,
                onSelectProgressBoardItem
            )
        }
    }

    if (inProgressItems.isNotEmpty()) {
        item {
            BoardContainer(
                pending,
                stringResource(R.string.in_progress),
                radius,
                inProgressItems,
                onSelectProgressBoardItem
            )
        }
    }

    if (completedItems.isNotEmpty()) {
        item {
            BoardContainer(
                completed,
                stringResource(R.string.Completed),
                radius,
                completedItems,
                onSelectProgressBoardItem
            )
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
    items(todoList, key = { it.id }) { todoItem ->
        TodoExpandableCard(
            navController = navController,
            radius = radius,
            item = todoItem,
            context = context,
            workspaceId = todoItem.workspaceId,
            isExpanded = todoItem.id in expandedTODOIds,
            onExpandToggle = onExpandToggle,
            onTodoEvents = onEvent
        )
    }
}

fun buildSearchFilterCategories(context: Context, workspaces: List<WorkspaceModel>, spaces: List<Space>): List<SearchFilterCategory> {
    return listOf(
        SearchFilterCategory(
            name = context.getString(R.string.workspaces),
            options = workspaces.map { SearchFilterOption(it.workspaceId, it.title) },
            type = SelectionType.MULTIPLE
        ),
        SearchFilterCategory(
            name = context.getString(R.string.spaces),
            options = spaces.map { SearchFilterOption(it.id.toString(), it.title) },
            type = SelectionType.MULTIPLE
        )
    )
}

data class FilterState(
    val selectedWorkspaceIds: Set<String>,
    val selectedSpacesIds: Set<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    filterState: FilterState,
    workspaces: List<WorkspaceModel>,
    spaces: List<Space>,
    sheetState: SheetState,
    onDismiss: () -> Unit = {},
    onApply: (Map<String, Set<String>>) -> Unit
) {
    val context = LocalContext.current
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.4f
    val categories = remember(workspaces, spaces) {
        buildSearchFilterCategories(context, workspaces, spaces)
    }
    val workspacesLabel = stringResource(R.string.workspaces)
    val spacesLabel = stringResource(R.string.spaces)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        // key resets all remembered state whenever filterState changes
        key(filterState) {
            var selectedCategory by remember { mutableStateOf(categories.first()) }

            val multiSelections = remember {
                mutableStateMapOf<String, SnapshotStateList<String>>().apply {
                    categories.forEach { category ->
                        val selected = when (category.name) {
                            workspacesLabel -> filterState.selectedWorkspaceIds
                            spacesLabel     -> filterState.selectedSpacesIds
                            else         -> emptySet()
                        }
                        put(category.name, mutableStateListOf<String>().apply { addAll(selected) })
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .padding(top = 8.dp)
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    // LEFT PANEL
                    LazyColumn(
                        modifier = Modifier
                            .width(150.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        items(categories) { category ->
                            CategoryRow(
                                label = category.name,
                                isSelected = category.name == selectedCategory.name,
                                onClick = { selectedCategory = category }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(0.5.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.onSurface)
                    )

                    // RIGHT PANEL
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        if (selectedCategory.options.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) { Text(stringResource(R.string.no_options_available)) }
                            }
                        }

                        items(selectedCategory.options) { option ->
                            when (selectedCategory.type) {
                                SelectionType.MULTIPLE -> {
                                    val list = multiSelections[selectedCategory.name] ?: return@items
                                    val isSelected = option.id in list
                                    MultiOptionRow(
                                        label = option.label,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (isSelected) list.remove(option.id)
                                            else list.add(option.id)
                                        }
                                    )
                                }
                                else -> {}
                            }
                        }
                    }
                }

                HorizontalDivider()

                // FOOTER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            categories.forEach { category ->
                                val list = multiSelections[category.name] ?: return@forEach
                                list.clear()
                                list.addAll(category.options.map { it.id })
                            }
                        }
                    ) { Text(stringResource(R.string.reset)) }

                    Button(
                        modifier = Modifier.weight(2f),
                        onClick = {
                            onApply(multiSelections.mapValues { it.value.toSet() })
                            onDismiss()
                        }
                    ) { Text(stringResource(R.string.Confirm)) }
                }

                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}