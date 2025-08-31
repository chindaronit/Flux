package com.flux.data.repository

import com.flux.data.dao.WorkspaceDao
import com.flux.data.model.WorkspaceModel
import com.flux.di.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WorkspaceRepositoryImpl @Inject constructor(
    private val dao: WorkspaceDao,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : WorkspaceRepository {
    override suspend fun upsertWorkspace(workspace: WorkspaceModel) {
        return withContext(ioDispatcher) { dao.upsertWorkspace(workspace) }
    }

    override suspend fun upsertWorkspaces(spaces: List<WorkspaceModel>) {
        return withContext(ioDispatcher) { dao.upsertWorkspaces(spaces) }
    }

    override suspend fun deleteWorkspace(workspace: WorkspaceModel) {
        return withContext(ioDispatcher) { dao.deleteWorkspace(workspace) }
    }

    override fun loadAllWorkspaces(): Flow<List<WorkspaceModel>> {
        return dao.loadAllWorkspaces()
    }
}