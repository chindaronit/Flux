package com.flux.ui.state

import com.flux.data.model.HabitInstanceModel
import com.flux.data.model.HabitModel

data class HabitState(
    val isLoading: Boolean = true,
    val allHabits: List<HabitModel> = emptyList(),
    val allInstances: List<HabitInstanceModel> = emptyList()
)