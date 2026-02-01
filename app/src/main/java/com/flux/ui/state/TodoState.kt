package com.flux.ui.state

import com.flux.data.model.TodoModel

data class TodoState(
    val isLoading: Boolean = true,
    val workspaceId: String? = null,
    val allLists: List<TodoModel> = emptyList()
)