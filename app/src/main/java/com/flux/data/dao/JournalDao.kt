package com.flux.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flux.data.model.JournalModel
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT EXISTS(SELECT 1 FROM JournalModel WHERE journalId = :journalId)")
    suspend fun exists(journalId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: JournalModel)

    @Delete
    suspend fun deleteEntry(entry: JournalModel)

    @Query("Delete FROM JournalModel where workspaceId = :workspaceId")
    suspend fun deleteAllWorkspaceEntries(workspaceId: String)

    @Query(
        """
        SELECT * FROM JournalModel 
        WHERE workspaceId = :workspaceId
        ORDER BY dateTime DESC
    """
    )
    fun loadAllEntries(workspaceId: String): Flow<List<JournalModel>>

    @Query("SELECT * FROM JournalModel")
    suspend fun loadAllEntries(): List<JournalModel>
}