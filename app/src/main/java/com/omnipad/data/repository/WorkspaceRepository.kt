package com.omnipad.data.repository

import com.omnipad.data.model.WorkspaceModel
import kotlinx.coroutines.flow.Flow

interface WorkspaceRepository {
    suspend fun upsertWorkspace(workspace: WorkspaceModel)
    suspend fun deleteWorkspace(workspace: WorkspaceModel)
    suspend fun upsertWorkspaces(spaces: List<WorkspaceModel>)
    suspend fun loadData(): List<WorkspaceModel>
    fun observePublicWorkspaceIds(): Flow<List<String>>
    fun loadAllWorkspaces(): Flow<List<WorkspaceModel>>
}