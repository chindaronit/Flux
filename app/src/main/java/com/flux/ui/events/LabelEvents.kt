package com.flux.ui.events

import com.flux.data.model.LabelModel

sealed class LabelEvents {
    data class DeleteLabel(val data: LabelModel) : LabelEvents()
    data class UpsertLabel(val data: LabelModel) : LabelEvents()
    data class DeleteAllWorkspaceLabels(val workspaceId: String) : LabelEvents()
}