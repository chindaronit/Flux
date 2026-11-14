package com.omnipad.ui.state

import com.omnipad.data.model.WorkspaceModel

data class WorkspaceState(
    val isLoading: Boolean = true,
    val currentWorkspace: WorkspaceModel? = null,
    val allWorkspaces: List<WorkspaceModel> = emptyList()
)
