package com.flux.data.repository

import com.flux.data.model.ProgressBoardModel
import kotlinx.coroutines.flow.Flow

interface ProgressBoardRepository {
    suspend fun upsertBoardItem(item: ProgressBoardModel)
    suspend fun deleteBoardItem(item: ProgressBoardModel)
    suspend fun deleteBoardItemsByWorkspace(workspaceId: String)
    fun getProgressBoardData(): Flow<List<ProgressBoardModel>>
}