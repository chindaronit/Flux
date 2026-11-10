package com.flux.ui.events

import android.content.Context
import com.flux.data.model.EventModel
import com.flux.data.model.EventExceptionModel

sealed class TaskEvents {
    data class DeleteAllWorkspaceEvents(val workspaceId: String, val context: Context) : TaskEvents()
    data class UpsertTask(val context: Context, val taskEvent: EventModel) : TaskEvents()
    data class ImportIcsEvents(val context: Context, val events: List<EventModel>) : TaskEvents()
    data class ExportIcsEvents(val context: Context, val event: EventModel) : TaskEvents()
    data class DeleteTask(val taskEvent: EventModel, val context: Context) : TaskEvents()
    data class ToggleStatus(val markDone: Boolean, val eventId: String, val workspaceId: String, val date: Long) : TaskEvents()
    data class DetachOccurrence(
        val context: Context,
        val exception: EventExceptionModel,
        val event: EventModel
    ) : TaskEvents()
    data class DeleteOccurrence(val exception: EventExceptionModel) : TaskEvents()
}