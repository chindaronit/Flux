package com.omnipad.ui.events

import com.omnipad.data.model.LabelModel

sealed class LabelEvents {
    data class DeleteLabel(val data: LabelModel) : LabelEvents()
    data class UpsertLabel(val data: LabelModel) : LabelEvents()
    data class DeleteAllWorkspaceLabels(val workspaceId: String) : LabelEvents()
}