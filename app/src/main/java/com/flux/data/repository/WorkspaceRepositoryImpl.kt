package com.flux.data.repository

import com.flux.data.dao.WorkspaceDao
import com.flux.data.model.WorkspaceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WorkspaceRepositoryImpl @Inject constructor(
    private val dao: WorkspaceDao
): WorkspaceRepository {
    override suspend fun upsertWorkspace(workspace: WorkspaceModel) { return withContext(Dispatchers.IO) { dao.upsertWorkspace(workspace) } }
    override suspend fun upsertWorkspaces(spaces: List<WorkspaceModel>) { return withContext(Dispatchers.IO) { dao.upsertWorkspaces(spaces) } }
    override suspend fun deleteWorkspace(workspace: WorkspaceModel) { return withContext(Dispatchers.IO) { dao.deleteWorkspace(workspace) } }
    override suspend fun loadData(): List<WorkspaceModel> { return withContext(Dispatchers.IO) { dao.getAll() } }
    override fun observePublicWorkspaceIds(): Flow<List<String>> { return dao.observePublicWorkspaceIds() }
    override fun loadAllWorkspaces(): Flow<List<WorkspaceModel>> { return dao.loadAllWorkspaces() }
}