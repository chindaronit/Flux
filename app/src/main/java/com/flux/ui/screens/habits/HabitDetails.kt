package com.flux.ui.screens.habits

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.data.model.HabitConfig
import com.flux.data.model.HabitInstanceModel
import com.flux.data.model.HabitModel
import com.flux.navigation.NavRoutes
import com.flux.ui.common.DeleteAlert
import com.flux.ui.common.HabitScaffold
import com.flux.ui.events.HabitEvents
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetails(
    navController: NavController,
    radius: Int,
    workspaceId: String,
    habit: HabitModel,
    habitInstances: List<HabitInstanceModel>,
    onHabitEvents: (HabitEvents) -> Unit
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    val todayEpoch = LocalDate.now().toEpochDay()
    val todayInstance = habitInstances.firstOrNull { it.instanceDate == todayEpoch }

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
        content = { innerPadding ->
            LazyColumn(
                Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { HabitDetailedInfo(radius, habit, habitInstances) }
                if(habit.habitConfig is HabitConfig.Counted) {
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
}