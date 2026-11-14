package com.omnipad.ui.state

import com.omnipad.data.model.NotesModel
import com.omnipad.other.HeaderNode

data class NotesState(
    val isLoading: Boolean = true,
    val aboutNote: TextState = TextState(),
    val outline: HeaderNode = HeaderNode("", 0, IntRange.EMPTY),
    val textState: TextState = TextState(),
    val selectedNotes: List<String> = emptyList(),
    val allNotes: List<NotesModel> = emptyList(),
)
