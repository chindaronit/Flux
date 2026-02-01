package com.flux.ui.state

import com.flux.data.model.JournalModel
import com.flux.other.HeaderNode
import java.time.LocalDate
import java.time.YearMonth

data class JournalState(
    val isLoading: Boolean = false,
    val workspaceId: String? = null,
    val outline: HeaderNode = HeaderNode("", 0, IntRange.EMPTY),
    val textState: TextState = TextState(),
    val selectedYearMonth: YearMonth = YearMonth.now(),
    val selectedDate: Long = LocalDate.now().toEpochDay(),
    val allEntries: List<JournalModel> = emptyList(),
    val datedEntries: List<JournalModel> = emptyList()
)