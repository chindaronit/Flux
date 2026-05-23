package com.flux.ui.state

import com.flux.data.model.LabelModel

data class LabelState(
    val isLoading: Boolean = true,
    val allLabels: List<LabelModel> = emptyList()
)
