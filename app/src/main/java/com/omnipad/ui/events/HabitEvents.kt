package com.omnipad.ui.events

import android.content.Context
import com.omnipad.data.model.HabitConfig
import com.omnipad.data.model.HabitInstanceModel
import com.omnipad.data.model.HabitModel

sealed class HabitEvents {
    data class DeleteAllWorkspaceHabits(val workspaceId: String, val context: Context) : HabitEvents()
    data class DeleteHabit(val habit: HabitModel, val context: Context) : HabitEvents()
    data class UpsertHabit(val context: Context, val habit: HabitModel) : HabitEvents()
    data class UpdateInstance(val habitInstance: HabitInstanceModel, val config: HabitConfig) : HabitEvents()
    data class UpdateOrder(val habitIds: List<String>) : HabitEvents()
}