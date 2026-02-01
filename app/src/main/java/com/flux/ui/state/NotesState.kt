package com.flux.ui.state

import com.flux.data.model.LabelModel
import com.flux.data.model.NotesModel
import com.flux.other.HeaderNode

data class NotesState(
    val isNotesLoading: Boolean = true,
    val isLabelsLoading: Boolean = true,
    val workspaceId: String? = null,
    val aboutNote: TextState = TextState(),
    val outline: HeaderNode = HeaderNode("", 0, IntRange.EMPTY),
    val textState: TextState = TextState(),
    val selectedNotes: List<String> = emptyList(),
    val allNotes: List<NotesModel> = emptyList(),
    val allLabels: List<LabelModel> = emptyList()
)
