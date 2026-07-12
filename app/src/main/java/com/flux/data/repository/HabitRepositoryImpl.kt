package com.flux.data.repository

import com.flux.data.dao.HabitInstanceDao
import com.flux.data.dao.HabitsDao
import com.flux.data.dao.WorkspaceDao
import com.flux.data.model.HabitConfig
import com.flux.data.model.HabitInstanceModel
import com.flux.data.model.HabitModel
import com.flux.data.model.HabitWithStatus
import com.flux.data.model.isActiveOn
import com.flux.data.model.isCompleted
import com.flux.data.model.startDateAsLocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class HabitRepositoryImpl @Inject constructor(
    private val dao: HabitsDao,
    private val instanceDao: HabitInstanceDao,
    private val workspaceDao: WorkspaceDao
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

    override suspend fun loadAllHabitsInstances(): List<HabitInstanceModel> {
        return withContext(Dispatchers.IO) { instanceDao.loadAllInstances() }
    }

    override suspend fun toggleHabit(habit: HabitModel, currentlyCompleted: Boolean) {
        val todayEpoch = LocalDate.now().toEpochDay()
        val oldInstance = instanceDao.getHabitInstance(habit.id, todayEpoch)

        when (habit.habitConfig) {
            is HabitConfig.Simple -> toggleSimpleHabit(habit, todayEpoch, currentlyCompleted, oldInstance)
            is HabitConfig.Counted -> incrementCountedHabit(habit, todayEpoch, oldInstance)
            is HabitConfig.Timed -> Unit
        }
    }

    private suspend fun toggleSimpleHabit(
        habit: HabitModel,
        todayEpoch: Long,
        currentlyCompleted: Boolean,
        oldInstance: HabitInstanceModel?,
    ) {
        if (currentlyCompleted) {
            oldInstance?.let { instanceDao.deleteInstance(it) }
        } else if (oldInstance == null) {
            instanceDao.upsertInstance(
                HabitInstanceModel(
                    habitId = habit.id,
                    workspaceId = habit.workspaceId,
                    instanceDate = todayEpoch,
                )
            )
        }
    }

    private suspend fun incrementCountedHabit(
        habit: HabitModel,
        todayEpoch: Long,
        oldInstance: HabitInstanceModel?,
    ) {
        instanceDao.upsertInstance(
            HabitInstanceModel(
                habitId = habit.id,
                workspaceId = habit.workspaceId,
                instanceDate = todayEpoch,
                count = (oldInstance?.count ?: 0) + 1,
            )
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeTodayHabitStatuses(): Flow<List<HabitWithStatus>> {
        val today = LocalDate.now()
        val todayEpoch = today.toEpochDay()
        val zoneId = ZoneId.systemDefault()
        val todayEndMillis = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        return workspaceDao.observePublicWorkspaceIds()
            .flatMapLatest { publicWorkspaceIds ->
                if (publicWorkspaceIds.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        dao.loadCandidateHabits(publicWorkspaceIds, todayEpoch, todayEndMillis),
                        instanceDao.loadInstancesForDate(todayEpoch),
                    ) { candidateHabits, instances ->
                        val instanceByHabitId = instances.associateBy { it.habitId }

                        candidateHabits.asSequence()
                            .filter { it.recurrence.isActiveOn(today, it.startDateAsLocalDate(zoneId)) }
                            .map { habit ->
                                val instance = instanceByHabitId[habit.id]
                                HabitWithStatus(habit, instance?.isCompleted(habit) ?: false)
                            }
                            .toList()
                    }
                }
            }
            .flowOn(Dispatchers.Default)
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