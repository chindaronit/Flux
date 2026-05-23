package com.flux.data.repository

import com.flux.data.dao.TodoDao
import com.flux.data.model.TodoModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TodoRepositoryImpl @Inject constructor(
    val dao: TodoDao
) : TodoRepository {
    override fun loadTodoData(): Flow<List<TodoModel>> {
        return dao.loadTodoData()
    }

    override suspend fun upsertList(list: TodoModel) {
        return withContext(Dispatchers.IO) { dao.upsertList(list) }
    }

    override suspend fun deleteList(list: TodoModel) {
        return withContext(Dispatchers.IO) { dao.deleteList(list) }
    }

    override suspend fun deleteAllWorkspaceLists(workspaceId: String) {
        return withContext(Dispatchers.IO) { dao.deleteAllWorkspaceLists(workspaceId) }
    }
}