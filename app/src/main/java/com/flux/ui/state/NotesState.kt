package com.flux.ui.state

import com.flux.data.model.NotesModel
import com.flux.other.HeaderNode

data class NotesState(
    val isLoading: Boolean = true,
    val aboutNote: TextState = TextState(),
    val outline: HeaderNode = HeaderNode("", 0, IntRange.EMPTY),
    val textState: TextState = TextState(),
    val selectedNotes: List<String> = emptyList(),
    val allNotes: List<NotesModel> = emptyList(),
)
