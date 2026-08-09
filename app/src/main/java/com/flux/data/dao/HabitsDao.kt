package com.flux.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flux.data.model.HabitModel
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitsDao {
    @Query("SELECT EXISTS(SELECT 1 FROM HabitModel WHERE id = :habitId)")
    suspend fun exists(habitId: String): Boolean

    @Query("SELECT * FROM HabitModel WHERE id = :habitId")
    suspend fun getHabitById(habitId: String): HabitModel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: HabitModel)

    @Query("UPDATE HabitModel SET `order` = :order WHERE id = :id")
    suspend fun updateOrder(id: String, order: Int)

    @Query("SELECT COALESCE(MAX(`order`), -1) FROM HabitModel WHERE workspaceId = :workspaceId")
    suspend fun getMaxOrder(workspaceId: String): Int

    @Delete
    suspend fun deleteHabit(habit: HabitModel)

    @Query("DELETE FROM HabitModel WHERE workspaceId = :workspaceId")
    suspend fun deleteAllWorkspaceHabit(workspaceId: String)

    @Query("""
        SELECT * FROM HabitModel
        WHERE workspaceId IN (:workspaceIds)
        AND (endDateTime = -1 OR endDateTime > :todayEpoch)
        AND startDateTime <= :todayEndMillis
    """)
    fun loadCandidateHabits(
        workspaceIds: List<String>,
        todayEpoch: Long,
        todayEndMillis: Long,
    ): Flow<List<HabitModel>>

    @Query("SELECT * FROM HabitModel ORDER BY `order` ASC")
    fun loadHabitData(): Flow<List<HabitModel>>

    @Query("Select * FROM HabitModel ORDER BY `order` ASC")
    fun loadAllHabits(): List<HabitModel>
}
