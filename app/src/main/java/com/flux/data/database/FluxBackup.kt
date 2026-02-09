package com.flux.data.database

import com.flux.data.model.EventInstanceModel
import com.flux.data.model.EventModel
import com.flux.data.model.HabitInstanceModel
import com.flux.data.model.HabitModel
import com.flux.data.model.JournalModel
import com.flux.data.model.LabelModel
import com.flux.data.model.NotesModel
import com.flux.data.model.SettingsModel
import com.flux.data.model.TodoModel
import com.flux.data.model.WorkspaceModel
import kotlinx.serialization.Serializable

@Serializable
data class FluxBackup(
    val workspaces: List<WorkspaceModel> = emptyList(),
    val notes: List<NotesModel> = emptyList(),
    val todos: List<TodoModel> = emptyList(),
    val habits: List<HabitModel> = emptyList(),
    val habitInstances: List<HabitInstanceModel> = emptyList(),
    val journals: List<JournalModel> = emptyList(),
    val labels: List<LabelModel> = emptyList(),
    val events: List<EventModel> = emptyList(),
    val eventInstances: List<EventInstanceModel> = emptyList(),
    val settings: SettingsModel = SettingsModel()
)