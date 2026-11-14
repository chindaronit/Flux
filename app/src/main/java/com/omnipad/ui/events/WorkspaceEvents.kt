package com.omnipad.ui.events

import android.content.Context
import android.net.Uri
import com.omnipad.data.model.WorkspaceModel

sealed class WorkspaceEvents {
    data class ChangeCover(val context: Context, val uri: Uri, val workspace: WorkspaceModel): WorkspaceEvents()
    data class DeleteSpace(val workspace: WorkspaceModel) : WorkspaceEvents()
    data class UpsertSpace(val workspace: WorkspaceModel) : WorkspaceEvents()
    data class UpsertSpaces(val workspaces: List<WorkspaceModel>) : WorkspaceEvents()
    data class ChangeWorkspace(val workspace: WorkspaceModel) : WorkspaceEvents()
}