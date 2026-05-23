package com.flux.ui.state

data class States(
    val notesState: NotesState,
    val eventState: EventState,
    val habitState: HabitState,
    val todoState: TodoState,
    val workspaceState: WorkspaceState,
    val journalState: JournalState,
    val progressBoardState: ProgressBoardState,
    val labelState: LabelState,
    val settings: Settings
)