package com.omnipad.data.repository

import com.omnipad.data.model.HabitDataItem
import com.omnipad.data.model.HabitInstanceModel
import com.omnipad.data.model.HabitModel
import com.omnipad.data.model.HabitWithStatus
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    suspend fun getHabitInstance(habitId: String, instanceDate: Long): HabitInstanceModel?
    suspend fun upsertHabit(habit: HabitModel)
    suspend fun deleteHabit(habit: HabitModel)
    suspend fun deleteAllWorkspaceHabit(workspaceId: String)
    suspend fun deleteInstance(habitInstance: HabitInstanceModel)
    suspend fun upsertHabitInstance(habitInstance: HabitInstanceModel)
    suspend fun loadAllHabits(): List<HabitModel>
    suspend fun loadAllHabitsInstances(): List<HabitInstanceModel>
    suspend fun toggleHabit(habit: HabitModel, currentlyCompleted: Boolean)
    suspend fun updateOrder(habitIds: List<String>)
    fun observePublicHabits(): Flow<List<HabitModel>>
    fun observeTodayHabitStatuses(): Flow<List<HabitWithStatus>>
    fun observeHabitDataItem(habitId: String): Flow<HabitDataItem?>
    fun loadHabitData(): Flow<List<HabitModel>>
    fun loadHabitInstanceData(): Flow<List<HabitInstanceModel>>
}