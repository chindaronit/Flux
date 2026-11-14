package com.omnipad.ui.events

import com.omnipad.data.model.ProgressBoardModel

sealed class ProgressBoardEvents {
    data class DeleteBoardItemsByWorkspace(val workspaceId: String) : ProgressBoardEvents()
    data class DeleteProgressItem(val data: ProgressBoardModel) : ProgressBoardEvents()
    data class UpsertProgressItem(val data: ProgressBoardModel) : ProgressBoardEvents()
}