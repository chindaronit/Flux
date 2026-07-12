package com.flux.data.repository

import com.flux.data.model.TodoDisplayItem
import com.flux.data.model.TodoInstance
import com.flux.data.model.TodoModel
import kotlinx.coroutines.flow.Flow

interface TodoRepository {
    suspend fun upsertList(list: TodoModel)
    suspend fun deleteList(list: TodoModel)
    suspend fun deleteAllWorkspaceLists(workspaceId: String)
    suspend fun upsertInstance(todoInstance: TodoInstance)
    suspend fun deleteAllWorkspaceInstance(workspaceId: String)
    suspend fun existInstance(listId: String, instanceDate: Long): Boolean
    suspend fun loadAllLists(): List<TodoModel>
    suspend fun toggleTodoItem(item: TodoDisplayItem, itemId: String)
    fun observePublicTodos(): Flow<List<TodoModel>>
    fun observeTodoList(todoId: String): Flow<TodoDisplayItem?>
    fun loadAllTodoInstance(): Flow<List<TodoInstance>>
    fun loadTodoData(): Flow<List<TodoModel>>
}