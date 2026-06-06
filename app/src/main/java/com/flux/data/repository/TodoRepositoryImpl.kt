package com.flux.data.repository

import com.flux.data.dao.TodoDao
import com.flux.data.dao.TodoInstanceDao
import com.flux.data.model.TodoInstance
import com.flux.data.model.TodoModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TodoRepositoryImpl @Inject constructor(
    val dao: TodoDao,
    val instanceDao: TodoInstanceDao
) : TodoRepository {
    override fun loadTodoData(): Flow<List<TodoModel>> {
        return dao.loadTodoData()
    }

    override suspend fun upsertList(list: TodoModel) {
        return withContext(Dispatchers.IO) { dao.upsertList(list) }
    }

    override suspend fun deleteList(list: TodoModel) {
        return withContext(Dispatchers.IO) {
            instanceDao.deleteListInstances(list.id)
            dao.deleteList(list)
        }
    }

    override suspend fun deleteAllWorkspaceLists(workspaceId: String) {
        return withContext(Dispatchers.IO) { dao.deleteAllWorkspaceLists(workspaceId) }
    }

    override suspend fun upsertInstance(todoInstance: TodoInstance) {
        return withContext(Dispatchers.IO) { instanceDao.upsertTodoInstance(todoInstance) }
    }

    override suspend fun deleteAllWorkspaceInstance(workspaceId: String) {
        return withContext(Dispatchers.IO) { instanceDao.deleteAllWorkspaceInstance(workspaceId) }
    }

    override suspend fun existInstance(listId: String, instanceDate: Long): Boolean {
        return withContext(Dispatchers.IO) { instanceDao.exists(listId, instanceDate) }
    }

    override suspend fun loadAllLists(): List<TodoModel> {
        return withContext(Dispatchers.IO) { dao.loadAllLists() }
    }

    override fun loadAllTodoInstance(): Flow<List<TodoInstance>> {
        return instanceDao.loadAll()
    }
}