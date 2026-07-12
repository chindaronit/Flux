package com.flux.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flux.data.model.HabitInstanceModel
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitInstanceDao {
    @Query("SELECT EXISTS(SELECT 1 FROM HabitInstanceModel WHERE habitId = :habitId and instanceDate = :instanceDate)")
    suspend fun exists(habitId: String, instanceDate: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInstance(habitInstance: HabitInstanceModel)

    @Query("DELETE FROM HabitInstanceModel WHERE habitId IN (:habitId)")
    suspend fun deleteAllInstances(habitId: String)

    @Query("SELECT * FROM HabitInstanceModel WHERE habitId = :habitId AND instanceDate = :date LIMIT 1")
    suspend fun getHabitInstance(habitId: String, date: Long): HabitInstanceModel?

    @Query("SELECT * FROM HabitInstanceModel WHERE instanceDate = :date")
    fun loadInstancesForDate(date: Long): Flow<List<HabitInstanceModel>>

    @Delete
    suspend fun deleteInstance(habitInstance: HabitInstanceModel)

    @Query("DELETE FROM HabitInstanceModel WHERE workspaceId = :workspaceId")
    suspend fun deleteAllWorkspaceInstance(workspaceId: String)

    @Query("SELECT * FROM HabitInstanceModel")
    fun loadHabitInstanceData(): Flow<List<HabitInstanceModel>>

    @Query("SELECT * FROM HabitInstanceModel")
    suspend fun loadAllInstances(): List<HabitInstanceModel>
}