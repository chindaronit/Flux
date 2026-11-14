package com.omnipad.ui.state

import com.omnipad.data.model.ProgressBoardModel

data class ProgressBoardState(
    val isLoading: Boolean = true,
    val allItems: List<ProgressBoardModel> = emptyList()
)
