package com.flux.data.repository

import com.flux.data.dao.HabitInstanceDao
import com.flux.data.dao.HabitsDao
import com.flux.data.model.HabitInstanceModel
import com.flux.data.model.HabitModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class HabitRepositoryImpl @Inject constructor(
    private val dao: HabitsDao,
    private val instanceDao: HabitInstanceDao
) : HabitRepository {

    override suspend fun getHabitInstance(habitId: String, instanceDate: Long): HabitInstanceModel? {
        return withContext(Dispatchers.IO) { instanceDao.getHabitInstance(habitId, instanceDate) }
    }

    override suspend fun upsertHabit(habit: HabitModel) {
        return withContext(Dispatchers.IO) { dao.upsertHabit(habit) }
    }

    override suspend fun deleteInstance(habitInstance: HabitInstanceModel) {
        return withContext(Dispatchers.IO) { instanceDao.deleteInstance(habitInstance) }
    }

    override suspend fun upsertHabitInstance(habitInstance: HabitInstanceModel) {
        return withContext(Dispatchers.IO) { instanceDao.upsertInstance(habitInstance) }
    }

    override suspend fun loadAllHabits(): List<HabitModel> {
        return withContext(Dispatchers.IO) { dao.loadAllHabits() }
    }

    override fun loadHabitInstanceData(): Flow<List<HabitInstanceModel>> {
        return instanceDao.loadHabitInstanceData()
    }

    override fun loadHabitData(): Flow<List<HabitModel>> {
        return dao.loadHabitData()
    }

    override suspend fun deleteHabit(habit: HabitModel) {
        return withContext(Dispatchers.IO) {
            instanceDao.deleteAllInstances(habit.id)
            dao.deleteHabit(habit)
        }
    }

    override suspend fun deleteAllWorkspaceHabit(workspaceId: String) {
        return withContext(Dispatchers.IO) {
            dao.deleteAllWorkspaceHabit(workspaceId)
            instanceDao.deleteAllWorkspaceInstance(workspaceId)
        }
    }
}