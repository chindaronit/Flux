package com.omnipad.data.database

import com.omnipad.data.model.EventInstanceModel
import com.omnipad.data.model.EventExceptionModel
import com.omnipad.data.model.EventOccurrenceColorModel
import com.omnipad.data.model.EventModel
import com.omnipad.data.model.HabitInstanceModel
import com.omnipad.data.model.HabitModel
import com.omnipad.data.model.JournalModel
import com.omnipad.data.model.LabelModel
import com.omnipad.data.model.NotesModel
import com.omnipad.data.model.ProgressBoardModel
import com.omnipad.data.model.TodoInstance
import com.omnipad.data.model.TodoModel
import com.omnipad.data.model.WorkspaceModel
import kotlinx.serialization.Serializable

@Serializable
data class BackupMediaFile(
    val directory: String,
    val name: String,
    val archivedName: String,
    val mimeType: String = "application/octet-stream"
)

@Serializable
data class OmniPadBackup(
    val formatVersion: Int = 4,
    val workspaces: List<WorkspaceModel> = emptyList(),
    val workspaceCovers: Map<String, String> = emptyMap(),
    val mediaFiles: List<BackupMediaFile> = emptyList(),
    val notes: List<NotesModel> = emptyList(),
    val todos: List<TodoModel> = emptyList(),
    val todoInstances: List<TodoInstance> = emptyList(),
    val habits: List<HabitModel> = emptyList(),
    val habitInstances: List<HabitInstanceModel> = emptyList(),
    val journals: List<JournalModel> = emptyList(),
    val labels: List<LabelModel> = emptyList(),
    val events: List<EventModel> = emptyList(),
    val eventInstances: List<EventInstanceModel> = emptyList(),
    val eventExceptions: List<EventExceptionModel> = emptyList(),
    val eventOccurrenceColors: List<EventOccurrenceColorModel> = emptyList(),
    val progressBoardItems: List<ProgressBoardModel> = emptyList()
)