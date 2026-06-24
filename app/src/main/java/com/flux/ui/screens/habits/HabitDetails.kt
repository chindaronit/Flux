package com.flux.ui.screens.habits

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flux.R
import androidx.navigation.NavController
import com.flux.data.model.HabitConfig
import com.flux.data.model.HabitInstanceModel
import com.flux.data.model.HabitModel
import com.flux.data.model.WorkspaceModel
import com.flux.navigation.NavRoutes
import com.flux.other.DataCopyType
import com.flux.ui.common.DataCopyDialog
import com.flux.ui.common.DeleteAlert
import com.flux.ui.common.HabitScaffold
import com.flux.ui.events.HabitEvents
import com.flux.ui.events.WorkspaceEvents
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetails(
    navController: NavController,
    radius: Int,
    workspaceId: String,
    habit: HabitModel,
    workspaces: List<WorkspaceModel>,
    habitInstances: List<HabitInstanceModel>,
    onHabitEvents: (HabitEvents) -> Unit,
    onWorkspaceEvents: (WorkspaceEvents) -> Unit
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    val todayEpoch = LocalDate.now().toEpochDay()
    val todayInstance = habitInstances.firstOrNull { it.instanceDate == todayEpoch }
    val isAllowedByRecurrence = isDateAllowedForHabit(habit.recurrence, todayEpoch)
    var showDataCopyDialog by remember { mutableStateOf(false) }
    val cloneString = stringResource(R.string.clone_created_successfully)
    val contentCopiedString = stringResource(R.string.content_copied)
    val contentMovedString = stringResource(R.string.content_moved)

    if(showDeleteDialog){
        DeleteAlert({
            showDeleteDialog=false
        }, {
            navController.popBackStack()
            onHabitEvents(HabitEvents.DeleteHabit(habit, context))
            showDeleteDialog=false
        })
    }

    HabitScaffold(
        onBackPressed = { navController.popBackStack() },
        onDeleteClicked = { showDeleteDialog=true },
        onEditClicked = { navController.navigate(NavRoutes.NewHabit.withArgs(workspaceId, habit.id)) },
        onCloneNote = {
            onHabitEvents(HabitEvents.UpsertHabit(context, HabitModel(workspaceId = habit.workspaceId, habitConfig = habit.habitConfig, title = "Clone ${habit.title}", description = habit.description, endDateTime = habit.endDateTime, notificationOffset = habit.notificationOffset, recurrence = habit.recurrence)))
            Toast.makeText(context, cloneString, Toast.LENGTH_SHORT).show()
        },
        onCopyNote = { showDataCopyDialog=true },
        content = { innerPadding ->
            LazyColumn(
                Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { HabitDetailedInfo(radius, habit, habitInstances) }
                if(habit.habitConfig is HabitConfig.Counted && isAllowedByRecurrence) {
                    item { CountedHabitStatus(radius, habit, todayInstance, onHabitEvents) }
                }
                if(habit.habitConfig is HabitConfig.Timed) {
                    item { TimedHabitStatus(radius, habit, todayInstance, onHabitEvents) }
                }
                item { HabitCalendarCard(radius, habit, habitInstances, onHabitEvents) }
                item { WeeklyHabitAnalyticsCard(radius, habit, habitInstances) }
                if(habit.habitConfig is HabitConfig.Timed || habit.habitConfig is HabitConfig.Counted){
                    item { SingleHabitWeeklyProgressChart(radius, habit, habitInstances) }
                }
                item { MonthlyHabitAnalyticsCard(radius, habit, habitInstances) }
                item { SingleHabitHeatMap(radius, habit, habitInstances) }
            }
        }
    )

    if(showDataCopyDialog){
        DataCopyDialog(
            workspaces.filterNot { it.workspaceId == workspaceId },
            { dataCopyType, selectedWorkspaces ->
                if(selectedWorkspaces.isEmpty()) return@DataCopyDialog
                when(dataCopyType){
                    DataCopyType.COPY -> {
                        selectedWorkspaces.forEach { workspace ->
                            if(!workspace.selectedSpaces.contains(5)){
                                onWorkspaceEvents(WorkspaceEvents.UpsertSpace(workspace.copy(selectedSpaces = workspace.selectedSpaces + 5)))
                            }
                            onHabitEvents(HabitEvents.UpsertHabit(context, HabitModel(workspaceId = workspace.workspaceId, habitConfig = habit.habitConfig, title = habit.title, description = habit.description, endDateTime = habit.endDateTime, notificationOffset = habit.notificationOffset, recurrence = habit.recurrence)))
                        }

                        Toast.makeText(context, contentCopiedString, Toast.LENGTH_SHORT).show()
                    }
                    DataCopyType.MOVE -> {
                        selectedWorkspaces.forEach { workspace ->
                            if(!workspace.selectedSpaces.contains(5)){
                                onWorkspaceEvents(WorkspaceEvents.UpsertSpace(workspace.copy(selectedSpaces = workspace.selectedSpaces + 5)))
                            }

                            onHabitEvents(HabitEvents.UpsertHabit(context, HabitModel(workspaceId = workspace.workspaceId, habitConfig = habit.habitConfig, title = habit.title, description = habit.description, endDateTime = habit.endDateTime, notificationOffset = habit.notificationOffset, recurrence = habit.recurrence)))

                        }

                        navController.popBackStack()
                        Toast.makeText(context, contentMovedString, Toast.LENGTH_SHORT).show()
                        onHabitEvents(HabitEvents.DeleteHabit(habit, context))
                    }
                }
            }
        ) { showDataCopyDialog = false }
    }
}