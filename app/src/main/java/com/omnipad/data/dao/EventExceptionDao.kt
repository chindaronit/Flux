package com.omnipad.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.omnipad.data.model.EventExceptionModel
import kotlinx.coroutines.flow.Flow

@Dao
interface EventExceptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(exception: EventExceptionModel)

    @Query("SELECT * FROM EventExceptionModel")
    fun loadAll(): Flow<List<EventExceptionModel>>

    @Query("SELECT * FROM EventExceptionModel")
    suspend fun getAll(): List<EventExceptionModel>

    @Query("DELETE FROM EventExceptionModel WHERE eventId = :eventId")
    suspend fun deleteForEvent(eventId: String)

    @Query("DELETE FROM EventExceptionModel WHERE workspaceId = :workspaceId")
    suspend fun deleteForWorkspace(workspaceId: String)

    @Query("DELETE FROM EventExceptionModel")
    suspend fun deleteAll()
}
