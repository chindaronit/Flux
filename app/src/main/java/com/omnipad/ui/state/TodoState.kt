package com.omnipad.ui.state

import com.omnipad.data.model.TodoInstance
import com.omnipad.data.model.TodoModel

data class TodoState(
    val isLoading: Boolean = true,
    val workspaceId: String? = null,
    val allLists: List<TodoModel> = emptyList(),
    val allInstances: List<TodoInstance> = emptyList()
)