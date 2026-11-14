package com.omnipad.ui.state

import com.omnipad.data.model.HabitInstanceModel
import com.omnipad.data.model.HabitModel

data class HabitState(
    val isLoading: Boolean = true,
    val allHabits: List<HabitModel> = emptyList(),
    val allInstances: List<HabitInstanceModel> = emptyList()
)