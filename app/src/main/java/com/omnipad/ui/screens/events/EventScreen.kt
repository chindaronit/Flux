package com.omnipad.ui.screens.events

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.omnipad.R
import com.omnipad.data.model.WorkspaceModel
import com.omnipad.data.model.EventExceptionModel
import com.omnipad.data.model.EventOccurrence
import com.omnipad.data.model.RecurrenceRule
import com.omnipad.data.model.eventColorForOccurrence
import com.omnipad.data.model.filterCompletedEventOccurrences
import com.omnipad.data.model.findEventOccurrences
import com.omnipad.data.model.initialEventOccurrenceIndex
import com.omnipad.navigation.Loader
import com.omnipad.navigation.NavRoutes
import com.omnipad.other.canScheduleReminder
import com.omnipad.other.isNotificationPermissionGranted
import com.omnipad.other.openAppNotificationSettings
import com.omnipad.other.requestExactAlarmPermission
import com.omnipad.ui.common.EmptyData
import com.omnipad.ui.common.DeleteAlert
import com.omnipad.ui.common.SpaceSearchBar
import com.omnipad.ui.common.SpaceTopBar
import com.omnipad.ui.common.SpacesMenu
import com.omnipad.ui.events.TaskEvents
import com.omnipad.ui.screens.journal.TimelineBody
import com.omnipad.ui.screens.workspaces.SpacesToolBar
import com.omnipad.ui.state.EventState
import com.omnipad.ui.state.Settings
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal fun agendaTodayHeaderIndex(occurrences: List<EventOccurrence>, today: LocalDate): Int? {
    val target = occurrences.getOrNull(initialEventOccurrenceIndex(occurrences, today)) ?: return null
    val firstOnDate = occurrences.indexOfFirst { it.date == target.date }
    return firstOnDate + occurrences.take(firstOnDate).map { it.date }.distinct().size
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU, Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventScreen(
    navController: NavController,
    state: EventState,
    settings: Settings,
    workspace: WorkspaceModel,
    onShowSpaceBottomSheet: () -> Unit,
    onSpaceChange: (Int) -> Unit,
    onAddCover: () -> Unit,
    onRemoveCover: () -> Unit,
    onDeleteWorkspace: () -> Unit,
    onToggleLock: () -> Unit,
    onEvent: (TaskEvents) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit
){
    val context = LocalContext.current
    val workspaceId = workspace.workspaceId
    val isLoading = state.isAllEventsLoading
    val is24HourFormat = settings.data.is24HourFormat
    val radius = settings.data.cornerRadius
    val allEventInstances = state.allEventInstances
    val workspaceEvents = state.allEvent.filter { it.workspaceId == workspaceId }
    var showCalendar by rememberSaveable(workspaceId) { mutableStateOf(false) }
    var calendarMonthOffset by rememberSaveable(workspaceId) { mutableIntStateOf(0) }
    var focusRequest by remember(workspaceId) { mutableStateOf<CalendarFocusRequest?>(null) }
    var focusSequence by remember(workspaceId) { mutableStateOf(0) }
    var deletionTarget by remember(workspaceId) { mutableStateOf<EventOccurrence?>(null) }
    var showDeleteChoice by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var deleteOccurrence by remember { mutableStateOf(false) }
    val matchingEvents = remember(query, workspaceEvents) {
        workspaceEvents.filter { query.isBlank() || it.title.contains(query, ignoreCase = true) }
    }
    val agendaStartDate = remember(query, workspaceId) { mutableStateOf(LocalDate.now().minusDays(365)) }
    val agendaEndDate = remember(query, workspaceId) { mutableStateOf(LocalDate.now().plusDays(365)) }
    val agendaOccurrences = remember(
        matchingEvents,
        agendaStartDate.value,
        agendaEndDate.value,
        state.eventExceptions,
        allEventInstances,
        settings.data.showCompletedEvents
    ) {
        filterCompletedEventOccurrences(
            findEventOccurrences(
                matchingEvents,
                state.eventExceptions,
                agendaStartDate.value,
                agendaEndDate.value
            ),
            allEventInstances,
            settings.data.showCompletedEvents
        )
    }
    val agendaGroups = remember(agendaOccurrences) {
        agendaOccurrences.groupBy { it.date }.toList()
    }
    val initialOccurrenceIndex = initialEventOccurrenceIndex(agendaOccurrences)
    val initialListIndex = remember(agendaGroups, initialOccurrenceIndex) {
        val initialOccurrence = agendaOccurrences.getOrNull(initialOccurrenceIndex)
        if (initialOccurrence == null) {
            0
        } else {
            val previousGroupsItemCount = agendaGroups
                .takeWhile { (date, _) -> date != initialOccurrence.date }
                .sumOf { (_, occurrences) -> occurrences.size + 1 }
            val occurrenceOffset = agendaGroups
                .first { (date, _) -> date == initialOccurrence.date }
                .second
                .indexOf(initialOccurrence)
            previousGroupsItemCount + 1 + occurrenceOffset
        }
    }
    val eventListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(query, workspaceId, agendaOccurrences.isNotEmpty()) {
        if (agendaOccurrences.isNotEmpty()) {
            eventListState.scrollToItem(initialListIndex)
        }
    }
    LaunchedEffect(query, workspaceId, eventListState, agendaOccurrences.isNotEmpty(), agendaStartDate.value) {
        snapshotFlow {
            Triple(
                eventListState.firstVisibleItemIndex,
                eventListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0,
                eventListState.isScrollInProgress
            )
        }.distinctUntilChanged().collect { (firstVisible, lastVisible, isScrolling) ->
            if (isScrolling && agendaStartDate.value.isAfter(LocalDate.of(1900, 1, 1)) &&
                firstVisible <= 1
            ) {
                agendaStartDate.value = agendaStartDate.value.minusDays(365)
            }
            if (isScrolling && agendaOccurrences.isNotEmpty() &&
                lastVisible >= eventListState.layoutInfo.totalItemsCount - 3
            ) {
                agendaEndDate.value = agendaEndDate.value.plusDays(365)
            }
        }
    }
    var showSpacesMenu by remember { mutableStateOf(false) }
    val notificationPermissionLabel = stringResource(R.string.Notification_Permission)
    val reminderPermissionLabel = stringResource(R.string.Reminder_Permission)
    var isActionButtonExpanded by remember { mutableStateOf(false) }
    val importLauncher = rememberIcsImportLauncher(context, workspaceId) {
        onEvent(TaskEvents.ImportIcsEvents(context, it))
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
                    onEvent(TaskEvents.DeleteOccurrence(EventExceptionModel(
                        target.event.id, target.instanceDate, target.event.workspaceId
                    )))
                } else {
                    onEvent(TaskEvents.DeleteTask(target.event, context))
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
            SpaceTopBar(
                title            = workspace.title,
                description      = workspace.description,
                cover            = workspace.cover,
                icon             = workspace.icon,
                isLocked         = workspace.isLocked,
                onBackPressed    = { navController.popBackStack() },
                onAddCover       = onAddCover,
                onRemoveCover    = onRemoveCover,
                onToggleLock     = onToggleLock,
                onDeleteWorkspace = onDeleteWorkspace,
                onEditWorkspace   = { navController.navigate(NavRoutes.NewWorkspace.withArgs(workspaceId)) }
            )
        },
        floatingActionButton = {
            Box(contentAlignment = Alignment.BottomEnd) {
                // Expanded actions
                AnimatedVisibility(
                    visible = isActionButtonExpanded,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        val buttonModifier = Modifier.width(140.dp)

                        ExtendedFloatingActionButton(
                            modifier = buttonModifier,
                            onClick = { importLauncher.launch(arrayOf("text/calendar")) },
                            icon = { Icon(Icons.Outlined.Download, contentDescription = null) },
                            text = { Text(stringResource(R.string.Import)) }
                        )

                        ExtendedFloatingActionButton(
                            modifier = buttonModifier,
                            onClick = {
                                val localDate = LocalDate.now()
                                val currentTime = LocalTime.now()
                                val zonedDateTime = ZonedDateTime.of(localDate, currentTime, ZoneId.systemDefault())
                                val selectedDateMillis = zonedDateTime.toInstant().toEpochMilli()
                                val newId = UUID.randomUUID().toString()
                                navController.navigate(NavRoutes.NewEvent.withArgs(workspaceId, newId, selectedDateMillis))
                            },
                            icon = { Icon(Icons.Outlined.Create, contentDescription = null) },
                            text = { Text(stringResource(R.string.create)) }
                        )

                        ExtendedFloatingActionButton(
                            modifier = buttonModifier,
                            onClick = { isActionButtonExpanded = false },
                            icon = { Icon(Icons.Outlined.Clear, contentDescription = null) },
                            text = { Text(stringResource(R.string.clear)) }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = !isActionButtonExpanded,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    FloatingActionButton( {
                        if (!canScheduleReminder(context)) {
                            Toast.makeText(context, reminderPermissionLabel, Toast.LENGTH_SHORT).show()
                            requestExactAlarmPermission(context)
                        }
                        if (!isNotificationPermissionGranted(context)) {
                            Toast.makeText(context, notificationPermissionLabel, Toast.LENGTH_SHORT).show()
                            openAppNotificationSettings(context)
                        }
                        if (canScheduleReminder(context) && isNotificationPermissionGranted(context)) {
                            isActionButtonExpanded = true
                        }
                    }) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            }
        }
    ) { innerPadding ->
        when {
            isLoading -> { Loader() }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(12.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box {
                        SpacesToolBar(
                            stringResource(R.string.Events),
                            Icons.Default.Event,
                            false,
                            onMainClick = { showSpacesMenu = true },
                            onEditClick = onShowSpaceBottomSheet
                        )
                        SpacesMenu(
                            showSpacesMenu,
                            workspace,
                            onSpaceChange
                        ) { showSpacesMenu = false }
                    }
                    FilterChip(
                        selected = !showCalendar,
                        onClick = { showCalendar = false; focusRequest = null },
                        label = { Text(stringResource(R.string.event_agenda)) }
                    )
                    FilterChip(
                        selected = showCalendar,
                        onClick = { showCalendar = true },
                        label = { Text(stringResource(R.string.Calendar)) }
                    )
                    TextButton(
                        enabled = showCalendar || agendaOccurrences.isNotEmpty(),
                        onClick = {
                            if (showCalendar) {
                                calendarMonthOffset = 0
                            } else {
                                val index = agendaTodayHeaderIndex(agendaOccurrences, LocalDate.now())
                                if (index != null) {
                                    scope.launch { eventListState.scrollToItem(index) }
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.Today))
                    }
                }
                if (showCalendar) {
                    EventCalendar(
                        events = workspaceEvents,
                        exceptions = state.eventExceptions,
                        instances = allEventInstances,
                        occurrenceColors = state.eventOccurrenceColors,
                        showCompletedEvents = settings.data.showCompletedEvents,
                        completedColorArgb = settings.data.completedEventColorArgb,
                        monthOffset = calendarMonthOffset,
                        onMonthOffsetChange = { calendarMonthOffset = it },
                        onEventClick = { occurrence ->
                            navController.navigate(NavRoutes.eventEditor(occurrence.event, occurrence.instanceDate))
                        },
                        modifier = Modifier.weight(1f),
                        focusRequest = focusRequest
                    )
                } else {
                    SpaceSearchBar(
                        query = query,
                        onQueryChange = onQueryChange,
                        onCloseClicked = {},
                        modifier = Modifier.padding(bottom = 12.dp),
                        horizontalPadding = 0.dp
                    )
                    LazyColumn(
                        state = eventListState,
                        modifier = Modifier.weight(1f, fill = true)
                    ) {
                        if (agendaOccurrences.isEmpty()) item { EmptyData() }
                        agendaGroups.forEach { (date, occurrences) ->
                            item(key = "date:$date") {
                                EventAgendaDateHeader(formatAgendaDate(date))
                            }
                            items(
                                items = occurrences,
                                key = { "${it.event.id}:${it.instanceDate}" }
                            ) { occurrence ->
                                val task = occurrence.event
                                val isPending = allEventInstances.none {
                                    it.eventId == task.id && it.instanceDate == occurrence.instanceDate
                                }
                                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                                    TimelineBody(isLast = false)
                                    Column {
                                        AgendaEventActions(
                                            isPending = isPending,
                                            onCalendar = {
                                                focusSequence++
                                                focusRequest = CalendarFocusRequest(task.id, occurrence.date, focusSequence)
                                                showCalendar = true
                                            },
                                            onComplete = {
                                                onEvent(TaskEvents.ToggleStatus(isPending, task.id, workspaceId, occurrence.instanceDate))
                                            },
                                            onDelete = {
                                                deletionTarget = occurrence
                                                deleteOccurrence = false
                                                if (task.recurrence != RecurrenceRule.Once) {
                                                    showDeleteChoice = true
                                                } else {
                                                    showDeleteConfirmation = true
                                                }
                                            }
                                        ) { trailing ->
                                            EventCard(
                                                radius = radius,
                                                is24HourFormat = is24HourFormat,
                                                isPending = isPending,
                                                title = task.title,
                                                repeat = task.recurrence,
                                                startDateTime = task.startDateTime,
                                                isAllDay = task.isAllDay,
                                                occurrenceDate = occurrence.instanceDate,
                                                colorArgb = eventColorForOccurrence(
                                                    task, occurrence.instanceDate, state.eventOccurrenceColors
                                                ),
                                                completedColorArgb = settings.data.completedEventColorArgb,
                                                onClick = {
                                                    navController.navigate(NavRoutes.eventEditor(task, occurrence.instanceDate))
                                                },
                                                onLongPressed = {
                                                    navController.navigate(NavRoutes.eventEditor(task, occurrence.instanceDate))
                                                },
                                                trailingContent = trailing
                                            )
                                        }
                                        if (occurrence != occurrences.last()) {
                                            Spacer(Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatAgendaDate(date: LocalDate): String {
    val day = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val formattedDate = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault()))
    return "$day, $formattedDate"
}

@Composable
fun EventAgendaDateHeader(formattedDate: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.secondaryContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.primary)
        )
        Text(
            text = formattedDate,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSecondaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}