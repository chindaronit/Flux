package com.omnipad.ui.state

import com.omnipad.data.model.LabelModel

data class LabelState(
    val isLoading: Boolean = true,
    val allLabels: List<LabelModel> = emptyList()
)
