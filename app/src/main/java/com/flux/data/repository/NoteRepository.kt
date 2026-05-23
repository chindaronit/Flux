package com.flux.data.repository

import com.flux.data.model.NotesModel
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    suspend fun upsertNote(note: NotesModel)
    suspend fun upsertNotes(notes: List<NotesModel>)
    suspend fun deleteNote(note: NotesModel)
    suspend fun deleteNotes(notes: List<String>)
    suspend fun deleteAllWorkspaceNotes(workspaceId: String)
    fun loadNotesData(): Flow<List<NotesModel>>
}
