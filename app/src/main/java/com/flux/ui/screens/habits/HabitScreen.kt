package com.flux.ui.screens.habits

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.flux.data.model.HabitInstanceModel
import com.flux.data.model.RecurrenceRule
import com.flux.data.model.WorkspaceModel
import com.flux.data.model.isCounted
import com.flux.data.model.isLive
import com.flux.navigation.Loader
import com.flux.navigation.NavRoutes
import com.flux.other.canScheduleReminder
import com.flux.other.isNotificationPermissionGranted
import com.flux.other.openAppNotificationSettings
import com.flux.other.requestExactAlarmPermission
import com.flux.ui.common.EmptyHabits
import com.flux.ui.common.SpaceSearchBar
import com.flux.ui.common.SpaceTopBar
import com.flux.ui.common.SpacesMenu
import com.flux.ui.events.HabitEvents
import com.flux.ui.screens.workspaces.SpacesToolBar
import com.flux.ui.state.HabitState
import com.flux.ui.state.Settings
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(
    navController: NavController,
    state: HabitState,
    settings: Settings,
    workspace: WorkspaceModel,
    onShowSpaceBottomSheet: () -> Unit,
    onSpaceChange: (Int) -> Unit,
    onAddCover: () -> Unit,
    onRemoveCover: () -> Unit,
    onDeleteWorkspace: () -> Unit,
    onToggleLock: () -> Unit,
    onEvent: (HabitEvents) -> Unit
){
    val context = LocalContext.current
    val workspaceId = workspace.workspaceId
    val isLoading = state.isLoading
    val allInstances = state.allInstances
    val radius = settings.data.cornerRadius
    val is24HourFormat = settings.data.is24HourFormat
    var query by remember { mutableStateOf("") }
    val allHabits = state.allHabits
        .filter { it.workspaceId == workspaceId }
        .filter { it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) }
    val currentHabits = allHabits.filter { it.isLive() }
    val pastHabits = allHabits.filter { !it.isLive() }
    var showSearchBar by remember { mutableStateOf(false) }
    var showSpacesMenu by remember { mutableStateOf(false) }
    val notificationPermissionLabel = stringResource(R.string.Notification_Permission)
    val reminderPermissionLabel = stringResource(R.string.Reminder_Permission)
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
                    navController.navigate(NavRoutes.NewHabit.withArgs(workspaceId, ""))
                }
            }) { Icon(Icons.Default.Add, null) }
        }
    ) { innerPadding ->
        when {
            isLoading -> Loader()
            else -> {
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(12.dp)
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if(showSearchBar){ SpaceSearchBar(query, { query=it }, { showSearchBar=false }) }
                            else {
                                SpacesToolBar(
                                    stringResource(R.string.Habits),
                                    Icons.Default.EventAvailable,
                                    false,
                                    onMainClick = { showSpacesMenu = true },
                                    onEditClick = onShowSpaceBottomSheet
                                )
                                SpacesMenu(showSpacesMenu, workspace, onSpaceChange) { showSpacesMenu = false }
                                Row{
                                    IconButton({ showSearchBar = true }) {
                                        Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                    if(allHabits.isEmpty())  item { EmptyHabits() }
                    items(currentHabits) { habit ->
                        val habitInstances = allInstances.filter { it.habitId == habit.id }
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
                                        workspaceId = workspaceId,
                                        count = count
                                    )
                                    onEvent(HabitEvents.UpdateInstance(newInstance, habit.habitConfig))
                                }
                            },
                            onAnalyticsClicked = { navController.navigate(NavRoutes.HabitDetails.withArgs(workspaceId, habit.id)) }
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
                        val habitInstances = allInstances.filter { it.habitId == habit.id }
                        HabitPreviewCard (
                            radius = radius,
                            is24HourFormat = is24HourFormat,
                            habit = habit,
                            instances = habitInstances,
                            onClick = {},
                            onAnalyticsClicked = {
                                navController.navigate(
                                    NavRoutes.HabitDetails.withArgs(
                                        workspaceId,
                                        habit.id
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

// Helper function to check if a date is allowed for the habit's recurrence
fun isDateAllowedForHabit(recurrence: RecurrenceRule, epochDay: Long): Boolean {
    return when (recurrence) {
        is RecurrenceRule.Weekly -> {
            // Convert epoch day to LocalDate to get day of week
            val localDate = LocalDate.ofEpochDay(epochDay)
            // Convert to Monday=0, Tuesday=1, ..., Sunday=6 format
            val dayOfWeek = (localDate.dayOfWeek.value + 6) % 7
            dayOfWeek in recurrence.daysOfWeek
        }
        is RecurrenceRule.Custom -> true
        is RecurrenceRule.Once -> true
        is RecurrenceRule.Monthly -> true
        is RecurrenceRule.Yearly -> true
    }
}