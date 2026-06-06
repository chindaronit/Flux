package com.flux.ui.events

import android.content.Context
import com.flux.data.model.TodoInstance
import com.flux.data.model.TodoModel

sealed class TodoEvents {
    data class DeleteAllWorkspaceLists(val context: Context, val workspaceId: String) : TodoEvents()
    data class DeleteList(val context: Context, val data: TodoModel) : TodoEvents()
    data class UpsertList(val context: Context, val isRemovingReminder: Boolean, val data: TodoModel) : TodoEvents()
    data class CreateInstance(val listId: String, val workspaceId: String) : TodoEvents()
    data class UpsertInstance(val instance: TodoInstance) : TodoEvents()
}