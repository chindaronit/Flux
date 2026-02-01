package com.flux.ui.events

import android.content.Context
import android.net.Uri
import com.flux.data.model.WorkspaceModel

sealed class WorkspaceEvents {
    data class ChangeCover(val context: Context, val uri: Uri, val workspace: WorkspaceModel): WorkspaceEvents()
    data class DeleteSpace(val space: WorkspaceModel) : WorkspaceEvents()
    data class UpsertSpace(val space: WorkspaceModel) : WorkspaceEvents()
    data class UpsertSpaces(val spaces: List<WorkspaceModel>) : WorkspaceEvents()
}