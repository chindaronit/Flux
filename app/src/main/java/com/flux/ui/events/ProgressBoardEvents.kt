package com.flux.ui.events

import com.flux.data.model.ProgressBoardModel

sealed class ProgressBoardEvents {
    data class DeleteBoardItemsByWorkspace(val workspaceId: String) : ProgressBoardEvents()
    data class DeleteProgressItem(val data: ProgressBoardModel) : ProgressBoardEvents()
    data class UpsertProgressItem(val data: ProgressBoardModel) : ProgressBoardEvents()
}