package com.flux.ui.events

import com.flux.data.model.TodoModel

sealed class TodoEvents {
    data class DeleteAllWorkspaceLists(val workspaceId: String) : TodoEvents()
    data class EnterWorkspace(val workspaceId: String) : TodoEvents()
    data class DeleteList(val data: TodoModel) : TodoEvents()
    data class UpsertList(val data: TodoModel) : TodoEvents()
}