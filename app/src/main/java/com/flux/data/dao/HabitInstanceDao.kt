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

    @Delete
    suspend fun deleteInstance(habitInstance: HabitInstanceModel)

    @Query("DELETE FROM HabitInstanceModel WHERE workspaceId = :workspaceId")
    suspend fun deleteAllWorkspaceInstance(workspaceId: String)

    @Query("SELECT * FROM HabitInstanceModel where workspaceId in (:workspaceId)")
    fun loadAllInstances(workspaceId: String): Flow<List<HabitInstanceModel>>

    @Query("SELECT * FROM HabitInstanceModel")
    suspend fun loadAllInstances(): List<HabitInstanceModel>
}