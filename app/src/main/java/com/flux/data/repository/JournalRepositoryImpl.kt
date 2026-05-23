package com.flux.data.repository

import com.flux.data.dao.JournalDao
import com.flux.data.model.JournalModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class JournalRepositoryImpl @Inject constructor(
    private val dao: JournalDao
) : JournalRepository {
    override suspend fun upsertEntry(entry: JournalModel) {
        return withContext(Dispatchers.IO) { dao.upsertEntry(entry) }
    }

    override suspend fun deleteEntry(entry: JournalModel) {
        return withContext(Dispatchers.IO) { dao.deleteEntry(entry) }
    }

    override suspend fun deleteAllWorkspaceEntry(workspaceId: String) {
        return withContext(Dispatchers.IO) { dao.deleteAllWorkspaceEntries(workspaceId) }
    }

    override fun loadJournalData(): Flow<List<JournalModel>> {
        return dao.loadJournalData()
    }
}