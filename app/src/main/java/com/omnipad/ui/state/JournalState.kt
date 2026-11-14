package com.omnipad.ui.state

import com.omnipad.data.model.JournalModel
import com.omnipad.other.HeaderNode
import java.time.LocalDate

data class JournalState(
    val isLoading: Boolean = false,
    val monthlyJournalCount:  Map<LocalDate, Int> = emptyMap(),
    val outline: HeaderNode = HeaderNode("", 0, IntRange.EMPTY),
    val textState: TextState = TextState(),
    val data: List<JournalModel> = emptyList(),
)