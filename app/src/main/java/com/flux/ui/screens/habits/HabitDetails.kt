package com.flux.ui.screens.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.data.model.HabitInstanceModel
import com.flux.data.model.HabitModel
import com.flux.ui.components.HabitBottomSheet
import com.flux.ui.components.HabitCalendarCard
import com.flux.ui.components.HabitScaffold
import com.flux.ui.components.HabitStartCard
import com.flux.ui.components.HabitStreakCard
import com.flux.ui.components.MonthlyHabitAnalyticsCard
import com.flux.ui.components.calculateStreaks
import com.flux.ui.events.HabitEvents
import com.flux.ui.state.Settings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetails(
    navController: NavController,
    radius: Int,
    workspaceId: String,
    habit: HabitModel,
    habitInstances: List<HabitInstanceModel>,
    settings: Settings,
    onHabitEvents: (HabitEvents) -> Unit
) {
    var showHabitDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val (currentStreak, bestStreak) = calculateStreaks(habitInstances)
    val context = LocalContext.current

    HabitScaffold(
        title = habit.title,
        description = habit.description,
        onBackPressed = { navController.popBackStack() },
        onDeleteClicked = {
            navController.popBackStack()
            onHabitEvents(HabitEvents.DeleteHabit(habit, context))
        },
        onEditClicked = { showHabitDialog = true },
        content = { innerPadding ->
            LazyColumn(
                Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { HabitStartCard(habit.startDateTime, radius) }
                item { HabitStreakCard(currentStreak, bestStreak, radius) }
                item {
                    HabitCalendarCard(
                        radius,
                        habit.habitId,
                        workspaceId,
                        habit.startDateTime,
                        habitInstances,
                        onHabitEvents
                    )
                }
                item { MonthlyHabitAnalyticsCard(radius, habitInstances) }
            }
        }
    )

    HabitBottomSheet(
        isEditing = true,
        habit = habit,
        isVisible = showHabitDialog,
        sheetState = sheetState,
        settings = settings,
        onDismissRequest = {
            scope.launch { sheetState.hide() }.invokeOnCompletion { showHabitDialog = false }
        },
        onConfirm = { newHabit, adjustedTime ->
            onHabitEvents(HabitEvents.UpsertHabit(context, newHabit, adjustedTime))
            scope.launch { sheetState.hide() }.invokeOnCompletion { showHabitDialog = false }
        }
    )
}