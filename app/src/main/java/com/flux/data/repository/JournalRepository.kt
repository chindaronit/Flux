package com.flux.data.repository

import com.flux.data.model.JournalModel
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    suspend fun upsertEntry(entry: JournalModel)
    suspend fun deleteEntry(entry: JournalModel)
    suspend fun deleteAllWorkspaceEntry(workspaceId: String)
    fun loadJournalData(): Flow<List<JournalModel>>
}