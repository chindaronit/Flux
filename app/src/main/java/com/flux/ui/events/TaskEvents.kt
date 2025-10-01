package com.flux.ui.events

import android.content.Context
import com.flux.data.model.EventModel
import java.time.YearMonth

sealed class TaskEvents {
    data class DeleteAllWorkspaceEvents(val workspaceId: String, val context: Context) : TaskEvents()
    data class LoadAllInstances(val workspaceId: String) : TaskEvents()
    data class LoadAllTask(val workspaceId: String) : TaskEvents()
    data class LoadDateTask(val workspaceId: String, val selectedDate: Long) : TaskEvents()
    data class UpsertTask(val context: Context, val taskEvent: EventModel) : TaskEvents()
    data class DeleteTask(val taskEvent: EventModel, val context: Context) : TaskEvents()
    data class ToggleStatus(val markDone: Boolean, val eventId: String, val workspaceId: String, val date: Long) : TaskEvents()
    data class ChangeMonth(val newYearMonth: YearMonth) : TaskEvents()
    data class ChangeDate(val newLocalDate: Long) : TaskEvents()
}