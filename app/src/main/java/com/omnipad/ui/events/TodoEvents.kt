package com.omnipad.ui.events

import android.content.Context
import com.omnipad.data.model.TodoInstance
import com.omnipad.data.model.TodoModel

sealed class TodoEvents {
    data class DeleteAllWorkspaceLists(val context: Context, val workspaceId: String) : TodoEvents()
    data class DeleteList(val context: Context, val data: TodoModel) : TodoEvents()
    data class UpsertList(val context: Context, val isRemovingReminder: Boolean, val data: TodoModel) : TodoEvents()
    data class CreateInstance(val listId: String, val workspaceId: String) : TodoEvents()
    data class UpsertInstance(val instance: TodoInstance) : TodoEvents()
    data class UpdateTodoOrder(val todoIds: List<String>) : TodoEvents()
}