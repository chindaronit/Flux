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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: HabitModel): Long

    @Delete
    suspend fun deleteHabit(habit: HabitModel)

    @Query("DELETE FROM HabitModel WHERE workspaceId = :workspaceId")
    suspend fun deleteAllWorkspaceHabit(workspaceId: Long)

    @Query("SELECT * FROM HabitModel WHERE workspaceId = :workspaceId")
    fun loadAllHabits(workspaceId: Long): Flow<List<HabitModel>>
}
