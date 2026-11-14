package com.omnipad.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.omnipad.data.model.EventOccurrenceColorModel
import kotlinx.coroutines.flow.Flow

@Dao
interface EventOccurrenceColorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(color: EventOccurrenceColorModel)

    @Query("SELECT * FROM EventOccurrenceColorModel")
    fun loadAll(): Flow<List<EventOccurrenceColorModel>>

    @Query("SELECT * FROM EventOccurrenceColorModel")
    suspend fun getAll(): List<EventOccurrenceColorModel>

    @Query("DELETE FROM EventOccurrenceColorModel WHERE eventId = :eventId")
    suspend fun deleteForEvent(eventId: String)

    @Query("DELETE FROM EventOccurrenceColorModel WHERE eventId = :eventId AND occurrenceDate = :occurrenceDate")
    suspend fun deleteForOccurrence(eventId: String, occurrenceDate: Long)

    @Query("DELETE FROM EventOccurrenceColorModel WHERE workspaceId = :workspaceId")
    suspend fun deleteForWorkspace(workspaceId: String)

    @Query("DELETE FROM EventOccurrenceColorModel")
    suspend fun deleteAll()
}
