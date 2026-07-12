package com.flux.ui.screens.events

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.WorkspaceModel
import com.flux.navigation.Loader
import com.flux.navigation.NavRoutes
import com.flux.other.canScheduleReminder
import com.flux.other.computeMonthlyEventDates
import com.flux.other.isNotificationPermissionGranted
import com.flux.other.openAppNotificationSettings
import com.flux.other.requestExactAlarmPermission
import com.flux.ui.common.EmptyData
import com.flux.ui.common.SpaceSearchBar
import com.flux.ui.common.SpaceTopBar
import com.flux.ui.common.SpacesMenu
import com.flux.ui.events.SettingEvents
import com.flux.ui.events.TaskEvents
import com.flux.ui.screens.workspaces.SpacesToolBar
import com.flux.ui.state.EventState
import com.flux.ui.state.Settings
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

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
    onSettingEvents: (SettingEvents) -> Unit,
    onEvent: (TaskEvents) -> Unit
){
    val context = LocalContext.current
    val workspaceId = workspace.workspaceId
    val selectedDate = state.selectedDate
    val isLoading = state.isDatedEventLoading
    val is24HourFormat = settings.data.is24HourFormat
    val radius = settings.data.cornerRadius
    val selectedMonth = state.selectedYearMonth
    val isMonthlyView = settings.data.isCalendarMonthlyView
    var query by remember { mutableStateOf("") }
    val datedEvents = state.datedEvents
        .filter { it.workspaceId==workspaceId }
        .filter { it.title.contains(query, ignoreCase = true) }
        .sortedBy {
            Instant.ofEpochMilli(it.startDateTime)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
                .toSecondOfDay()
        }
    val monthlyEventCount = computeMonthlyEventDates(state.allEvent.filter { it.workspaceId == workspaceId }, selectedMonth)
    val allEventInstances = state.allEventInstances
    var showSearchBar by remember { mutableStateOf(false) }
    var showSpacesMenu by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val notificationPermissionLabel = stringResource(R.string.Notification_Permission)
    val reminderPermissionLabel = stringResource(R.string.Reminder_Permission)

    val pendingTasks = datedEvents.filter { task ->
        val instance = allEventInstances.find { it.eventId == task.id && it.instanceDate == selectedDate }
        instance == null
    }

    val completedTasks = datedEvents.filter { task ->
        val instance = allEventInstances.find { it.eventId == task.id && it.instanceDate == selectedDate }
        instance != null
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            SpaceTopBar(
                scrollBehavior   = scrollBehavior,
                title            = workspace.title,
                description      = workspace.description,
                cover            = workspace.cover,
                icon             = workspace.icon,
                isLocked         = workspace.passKey!=null,
                onBackPressed    = { navController.popBackStack() },
                onAddCover       = onAddCover,
                onRemoveCover    = onRemoveCover,
                onToggleLock     = onToggleLock,
                onDeleteWorkspace = onDeleteWorkspace,
                onEditWorkspace   = { navController.navigate(NavRoutes.NewWorkspace.withArgs(workspaceId)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton({
                if (!canScheduleReminder(context)) {
                    Toast.makeText(context, reminderPermissionLabel, Toast.LENGTH_SHORT).show()
                    requestExactAlarmPermission(context)
                }
                if (!isNotificationPermissionGranted(context)) {
                    Toast.makeText(context, notificationPermissionLabel, Toast.LENGTH_SHORT).show()
                    openAppNotificationSettings(context)
                }
                if (canScheduleReminder(context) && isNotificationPermissionGranted(context)) {
                    val localDate = LocalDate.ofEpochDay(selectedDate)
                    val currentTime = LocalTime.now()
                    val zonedDateTime = ZonedDateTime.of(localDate, currentTime, ZoneId.systemDefault())
                    val selectedDateMillis = zonedDateTime.toInstant().toEpochMilli()

                    navController.navigate(NavRoutes.NewEvent.withArgs(workspaceId, "", selectedDateMillis))
                }
            }) { Icon(Icons.Default.Add, null) }
        }
    ) { innerPadding ->
        when {
            isLoading -> { Loader() }
            else ->
                LazyColumn(Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(12.dp)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    item {
                        if(showSearchBar){ SpaceSearchBar(query, { query=it }, { showSearchBar=false }) }
                        else {
                            Row(
                                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
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

                                Row {
                                    IconButton({ showSearchBar = true }) {
                                        Icon(
                                            Icons.Default.Search,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    IconButton({ onSettingEvents(SettingEvents.UpdateSettings(
                                        settings.data.copy(isCalendarMonthlyView = !isMonthlyView)))
                                    }) {
                                        Icon(
                                            if (isMonthlyView) Icons.Default.CalendarViewDay else Icons.Default.CalendarViewMonth,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (isMonthlyView) {
                        item {
                            MonthlyViewCalendar(
                                selectedMonth, selectedDate, monthlyEventCount,
                                onMonthChange = { onEvent(TaskEvents.ChangeMonth(it)) },
                                onDateChange = { onEvent(TaskEvents.ChangeDate(it)) }
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    } else {
                        item {
                            DailyViewCalendar(selectedMonth, selectedDate){
                                onEvent(TaskEvents.ChangeDate(it))
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                    if(datedEvents.isEmpty()) item { EmptyData() }
                    if (pendingTasks.isNotEmpty()) {
                        items(pendingTasks) { task ->
                            EventCard(
                                radius = radius,
                                is24HourFormat = is24HourFormat,
                                isPending = true,
                                title = task.title,
                                repeat = task.recurrence,
                                startDateTime = task.startDateTime,
                                onChangeStatus = { onEvent(TaskEvents.ToggleStatus(true, task.id, workspaceId, selectedDate)) },
                                onClick = { navController.navigate(NavRoutes.EventDetails.withArgs(workspaceId, task.id, selectedDate)) }
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
                                onChangeStatus = { onEvent(TaskEvents.ToggleStatus(false, task.id, workspaceId, selectedDate)) },
                                onClick = { navController.navigate(NavRoutes.EventDetails.withArgs(workspaceId, task.id, selectedDate)) }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
        }
    }
}