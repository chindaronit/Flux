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

    @Delete
    suspend fun deleteNote(note: NotesModel)

    @Query("DELETE FROM NotesModel WHERE notesId IN (:ids)")
    suspend fun deleteNotes(ids: List<String>)

    @Query("DELETE FROM NotesModel WHERE workspaceId = :workspaceId")
    suspend fun deleteAllWorkspaceNotes(workspaceId: String)

    @Query("SELECT * FROM NotesModel ORDER by lastEdited DESC")
    fun loadNotesData(): Flow<List<NotesModel>>

    @Query("SELECT * FROM NotesModel")
    fun loadAllNotes(): List<NotesModel>
}
