package com.flux.ui.state

import com.flux.data.model.ProgressBoardModel

data class ProgressBoardState(
    val isLoading: Boolean = true,
    val allItems: List<ProgressBoardModel> = emptyList()
)
