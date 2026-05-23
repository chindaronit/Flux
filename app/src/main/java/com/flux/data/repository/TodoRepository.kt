package com.flux.data.repository

import com.flux.data.model.TodoModel
import kotlinx.coroutines.flow.Flow

interface TodoRepository {
    suspend fun upsertList(list: TodoModel)
    suspend fun deleteList(list: TodoModel)
    suspend fun deleteAllWorkspaceLists(workspaceId: String)
    fun loadTodoData(): Flow<List<TodoModel>>
}