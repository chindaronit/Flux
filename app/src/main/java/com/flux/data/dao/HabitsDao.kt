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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: HabitModel)

    @Delete
    suspend fun deleteHabit(habit: HabitModel)

    @Query("DELETE FROM HabitModel WHERE workspaceId = :workspaceId")
    suspend fun deleteAllWorkspaceHabit(workspaceId: String)

    @Query("SELECT * FROM HabitModel WHERE workspaceId = :workspaceId")
    fun loadAllHabitsOfWorkspace(workspaceId: String): Flow<List<HabitModel>>

    @Query("Select * FROM HabitModel")
    suspend fun loadAllHabits(): List<HabitModel>
}
