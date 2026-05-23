package com.flux.ui.state

import com.flux.data.model.JournalModel
import com.flux.other.HeaderNode
import java.time.LocalDate

data class JournalState(
    val isLoading: Boolean = false,
    val monthlyJournalCount:  Map<LocalDate, Int> = emptyMap(),
    val outline: HeaderNode = HeaderNode("", 0, IntRange.EMPTY),
    val textState: TextState = TextState(),
    val data: List<JournalModel> = emptyList(),
)