package com.flux.ui.screens.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.data.model.HabitInstanceModel
import com.flux.data.model.HabitModel
import com.flux.navigation.NavRoutes
import com.flux.ui.components.HabitCalendarCard
import com.flux.ui.components.HabitScaffold
import com.flux.ui.components.HabitStartCard
import com.flux.ui.components.HabitStreakCard
import com.flux.ui.components.MonthlyHabitAnalyticsCard
import com.flux.ui.events.HabitEvents

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

    HabitScaffold(
        title = habit.title,
        description = habit.description,
        onBackPressed = { navController.popBackStack() },
        onDeleteClicked = {
            navController.popBackStack()
            onHabitEvents(HabitEvents.DeleteHabit(habit, context))
        },
        onEditClicked = { navController.navigate(NavRoutes.NewHabit.withArgs(workspaceId, habit.id)) },
        content = { innerPadding ->
            LazyColumn(
                Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { HabitStartCard(habit.startDateTime, radius) }
                item { HabitStreakCard(habit, habitInstances, radius) }
                item {
                    HabitCalendarCard(
                        radius,
                        habit.id,
                        workspaceId,
                        habit.startDateTime,
                        habit.recurrence,
                        habitInstances,
                        onHabitEvents
                    )
                }
                item { MonthlyHabitAnalyticsCard(radius, habitInstances) }
            }
        }
    )
}