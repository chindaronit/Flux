package com.flux.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flux.data.model.EventInstanceModel
import kotlinx.coroutines.flow.Flow

@Dao
interface EventInstanceDao {
    @Query("SELECT EXISTS(SELECT 1 FROM EventInstanceModel WHERE eventId = :eventId and instanceDate = :instanceDate)")
    suspend fun exists(eventId: String, instanceDate: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEventInstance(event: EventInstanceModel)

    @Delete
    suspend fun deleteEventInstance(eventInstanceModel: EventInstanceModel)

    @Query("DELETE FROM EventInstancemodel WHERE eventId IN (:eventId)")
    suspend fun deleteAllEventInstance(eventId: String)

    @Query("DELETE FROM EventInstanceModel WHERE workspaceId = :workspaceId")
    suspend fun deleteAllWorkspaceInstance(workspaceId: String)

    @Query("SELECT * FROM EventInstanceModel where workspaceId in (:workspaceId)")
    fun loadAllWorkspaceInstances(workspaceId: String): Flow<List<EventInstanceModel>>

    @Query("SELECT * FROM EventInstanceModel")
    suspend fun getAll(): List<EventInstanceModel>
}