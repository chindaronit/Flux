package com.flux.data.repository

import com.flux.data.model.HabitInstanceModel
import com.flux.data.model.HabitModel
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    suspend fun upsertHabit(habit: HabitModel): Long
    suspend fun deleteHabit(habit: HabitModel)
    suspend fun deleteAllWorkspaceHabit(workspaceId: Long)
    suspend fun deleteInstance(habitInstance: HabitInstanceModel)
    suspend fun upsertHabitInstance(habitInstance: HabitInstanceModel)
    fun loadAllHabits(workspaceId: Long): Flow<List<HabitModel>>
    fun loadAllHabitInstance(workspaceId: Long): Flow<List<HabitInstanceModel>>
}