package com.flux.data.repository

import com.flux.data.model.LabelModel
import kotlinx.coroutines.flow.Flow

interface LabelRepository {
    suspend fun upsertLabel(label: LabelModel)
    suspend fun deleteLabel(label: LabelModel)
    suspend fun deleteAllWorkspaceLabels(workspaceId: String)
    fun loadAllLabels(): Flow<List<LabelModel>>
}