package com.omnipad.data.repository

import com.omnipad.data.dao.LabelDao
import com.omnipad.data.model.LabelModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LabelRepositoryImpl @Inject constructor (
    private val dao: LabelDao
) : LabelRepository {
    override suspend fun upsertLabel(label: LabelModel) {
        return withContext(Dispatchers.IO) { dao.upsertLabel(label) }
    }

    override suspend fun deleteLabel(label: LabelModel) {
        return withContext(Dispatchers.IO) { dao.deleteLabel(label) }
    }

    override suspend fun deleteAllWorkspaceLabels(workspaceId: String) {
        return withContext(Dispatchers.IO) { dao.deleteAllWorkspaceLabels(workspaceId) }
    }

    override fun loadAllLabels(): Flow<List<LabelModel>> {
        return dao.loadAllLabels()
    }
}