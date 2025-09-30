package com.flux.ui.screens.habits

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.data.model.HabitInstanceModel
import com.flux.data.model.HabitModel
import com.flux.data.model.RecurrenceRule
import com.flux.navigation.Loader
import com.flux.navigation.NavRoutes
import com.flux.ui.components.EmptyHabits
import com.flux.ui.components.HabitPreviewCard
import com.flux.ui.events.HabitEvents
import com.flux.ui.state.Settings
import java.time.LocalDate

fun LazyListScope.habitsHomeItems(
    navController: NavController,
    isLoading: Boolean,
    radius: Int,
    workspaceId: String,
    allHabits: List<HabitModel>,
    allInstances: List<HabitInstanceModel>,
    settings: Settings,
    onHabitEvents: (HabitEvents) -> Unit
) {

    when {
        isLoading -> item { Loader() }
        allHabits.isEmpty() -> item { EmptyHabits() }
        else -> {
            items(allHabits) { habit ->
                val habitInstances = allInstances.filter { it.habitId == habit.id }
                HabitPreviewCard(
                    radius = radius,
                    habit = habit,
                    instances = habitInstances,
                    settings = settings,
                    onToggleDone = { date ->
                        if (isDateAllowedForHabit(habit.recurrence, date)) {
                            val existing = habitInstances.find { it.instanceDate == date }
                            if (existing != null) {
                                onHabitEvents(HabitEvents.MarkUndone(existing))
                            } else {
                                onHabitEvents(
                                    HabitEvents.MarkDone(
                                        HabitInstanceModel(
                                            instanceDate = date,
                                            habitId = habit.id,
                                            workspaceId = workspaceId
                                        )
                                    )
                                )
                            }
                        }
                    },
                    onAnalyticsClicked = {
                        navController.navigate(
                            NavRoutes.HabitDetails.withArgs(
                                workspaceId,
                                habit.id
                            )
                        )
                    }
                )
                Spacer(Modifier.height(8.dp))
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