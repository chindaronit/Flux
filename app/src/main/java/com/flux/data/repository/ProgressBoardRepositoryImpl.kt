package com.flux.data.repository

import com.flux.data.dao.ProgressBoardDao
import com.flux.data.model.ProgressBoardModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ProgressBoardRepositoryImpl @Inject constructor(
    private val dao: ProgressBoardDao
): ProgressBoardRepository {
    override suspend fun upsertBoardItem(item: ProgressBoardModel) {
        return withContext(Dispatchers.IO) { dao.upsertBoardItem(item) }
    }

    override suspend fun deleteBoardItem(item: ProgressBoardModel) {
        return withContext(Dispatchers.IO) { dao.deleteBoardItem(item) }
    }

    override suspend fun deleteBoardItemsByWorkspace(workspaceId: String) {
        return withContext(Dispatchers.IO) { dao.deleteBoardItemsByWorkspace(workspaceId) }
    }

    override fun getProgressBoardData(): Flow<List<ProgressBoardModel>> {
        return dao.getProgressBoardData()
    }
}