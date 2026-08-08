package com.flux.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flux.data.model.NotesModel
import kotlinx.coroutines.flow.Flow

@Dao
interface NotesDao {
    @Query("SELECT EXISTS(SELECT 1 FROM NotesModel WHERE notesId = :notesId)")
    suspend fun exists(notesId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(notes: NotesModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotes(notes: List<NotesModel>)

    @Query("SELECT * FROM NotesModel WHERE notesId = :notesId")
    suspend fun getNotesById(notesId: String): NotesModel?

    @Query("UPDATE NotesModel SET `order` = :order WHERE notesId = :id")
    suspend fun updateOrder(id: String, order: Int)

    @Query("SELECT COALESCE(MAX(`order`), -1) FROM NotesModel WHERE workspaceId = :workspaceId")
    suspend fun getMaxOrder(workspaceId: String): Int

    @Delete
    suspend fun deleteNote(note: NotesModel)

    @Query("DELETE FROM NotesModel WHERE notesId IN (:ids)")
    suspend fun deleteNotes(ids: List<String>)

    @Query("DELETE FROM NotesModel WHERE workspaceId = :workspaceId")
    suspend fun deleteAllWorkspaceNotes(workspaceId: String)

    @Query("SELECT * FROM NotesModel ORDER BY `order` ASC")
    fun loadNotesData(): Flow<List<NotesModel>>

    @Query("SELECT * FROM NotesModel ORDER BY `order` ASC")
    fun loadAllNotes(): List<NotesModel>
}
