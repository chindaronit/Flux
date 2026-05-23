package com.flux.data.repository

import com.flux.data.dao.LabelDao
import com.flux.data.dao.NotesDao
import com.flux.data.model.NotesModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val notesDao: NotesDao,
    private val labelDao: LabelDao
) : NoteRepository {
    override suspend fun upsertNote(note: NotesModel) {
        return withContext(Dispatchers.IO) { notesDao.upsertNote(note) }
    }

    override suspend fun upsertNotes(notes: List<NotesModel>) {
        return withContext(Dispatchers.IO) { notesDao.upsertNotes(notes) }
    }

    override suspend fun deleteNote(note: NotesModel) {
        return withContext(Dispatchers.IO) { notesDao.deleteNote(note) }
    }

    override suspend fun deleteNotes(notes: List<String>) {
        return withContext(Dispatchers.IO) { notesDao.deleteNotes(notes) }
    }

    override fun loadNotesData(): Flow<List<NotesModel>> {
        return notesDao.loadNotesData()
    }

    override suspend fun deleteAllWorkspaceNotes(workspaceId: String) {
        return (withContext(Dispatchers.IO) {
            labelDao.deleteAllWorkspaceLabels(workspaceId)
            notesDao.deleteAllWorkspaceNotes(workspaceId)
        })
    }
}