package com.flux.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flux.data.model.EventModel
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT EXISTS(SELECT 1 FROM EventModel WHERE eventId = :id)")
    suspend fun exists(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvent(task: EventModel): Long

    @Delete
    suspend fun deleteEvent(task: EventModel)

    @Query("Delete FROM EventModel where workspaceId = :workspaceId")
    suspend fun deleteAllWorkspaceEvents(workspaceId: String)

    @Query("SELECT * FROM EventModel WHERE workspaceId = :workspaceId")
    fun loadAllEvents(workspaceId: String): Flow<List<EventModel>>

    @Query("SELECT * FROM EventModel")
    suspend fun loadAllEvents(): List<EventModel>
}