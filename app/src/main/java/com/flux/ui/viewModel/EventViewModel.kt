package com.flux.ui.viewModel

import android.content.Context
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.data.model.EventInstanceModel
import com.flux.data.model.EventExceptionModel
import com.flux.data.model.EventModel
import com.flux.data.model.toScheduleRequest
import com.flux.data.repository.EventRepository
import com.flux.data.repository.SettingsRepository
import com.flux.other.Constants
import com.flux.other.IcsExporter
import com.flux.other.cancelReminder
import com.flux.other.getOrCreateDirectory
import com.flux.other.scheduleNextReminder
import com.flux.ui.events.TaskEvents
import com.flux.ui.state.EventState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import javax.inject.Inject

@HiltViewModel
class EventViewModel @Inject constructor(
    private val repository: EventRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EventState())
    val state: StateFlow<EventState> = _state.asStateFlow()

    fun onEvent(event: TaskEvents) {
        viewModelScope.launch { reduce(event) }
    }

    private fun updateState(reducer: (EventState) -> EventState) {
        _state.update(reducer)
    }

    init {
        viewModelScope.launch {
            combine(
                repository.loadEventData(),
                repository.loadEventInstanceData(),
                repository.loadEventExceptionData()
            ) { allEvents, allInstances, exceptions ->
                Triple(allEvents, allInstances, exceptions)
            }.collect { (allEvents, allInstances, exceptions) ->

                updateState {
                    it.copy(
                        isAllEventsLoading = false,
                        allEvent = allEvents,
                        allEventInstances = allInstances,
                        eventExceptions = exceptions
                    )
                }
            }
        }
    }

    private fun reduce(event: TaskEvents) {
        when (event) {
            is TaskEvents.UpsertTask -> upsertEvent(event.context, event.taskEvent)
            is TaskEvents.DeleteTask -> deleteEvent(event.taskEvent, event.context)
            is TaskEvents.ToggleStatus ->
                toggleStatus(
                    event.markDone,
                    event.eventId,
                    event.workspaceId,
                    event.date
                )
            is TaskEvents.DetachOccurrence -> detachOccurrence(event)
            is TaskEvents.DeleteOccurrence -> deleteOccurrence(event)
            is TaskEvents.DeleteAllWorkspaceEvents -> deleteWorkspaceEvents(event.workspaceId, event.context)
            is TaskEvents.ImportIcsEvents -> importIcsEvents(event.context, event.events)
            is TaskEvents.ExportIcsEvents -> exportICSEvents(event.context, event.event)
        }
    }

    private fun detachOccurrence(event: TaskEvents.DetachOccurrence) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.detachEventOccurrence(event.exception, event.event)
            scheduleNextReminder(event.context, event.event.toScheduleRequest())
        }
    }

    private fun deleteOccurrence(event: TaskEvents.DeleteOccurrence) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteEventOccurrence(event.exception)
        }
    }

    private fun toggleStatus(
        markDone: Boolean,
        eventId: String,
        workspaceId: String,
        date: Long
    ) {
        val instance =
            state.value.allEventInstances.find {
                it.eventId == eventId && it.instanceDate == date
            } ?: EventInstanceModel(eventId, workspaceId, date)

        viewModelScope.launch(Dispatchers.IO) {
            if (markDone) repository.upsertEventInstance(instance)
            else repository.deleteEventInstance(instance)
        }
    }

    private fun deleteEvent(data: EventModel, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            cancelReminder(context, data.toScheduleRequest())
            repository.deleteEvent(data)
        }
    }

    private fun upsertEvent(context: Context, data: EventModel) {
        viewModelScope.launch(Dispatchers.IO) {
            cancelReminder(context, data.toScheduleRequest())
            repository.upsertEvent(data)
            scheduleNextReminder(context = context, data.toScheduleRequest())
        }
    }

    private fun importIcsEvents(context: Context, data: List<EventModel>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertEvents(data)
            data.forEach { event -> scheduleNextReminder(context, event.toScheduleRequest()) }
        }
    }

    private fun deleteWorkspaceEvents(workspaceId: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            state.value.allEvent.forEach { event -> cancelReminder(context, event.toScheduleRequest()) }
            repository.deleteAllWorkspaceEvent(workspaceId)
        }
    }

    private fun exportICSEvents(context: Context, event: EventModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = exportIcsToStorage(
                context = context,
                title = event.title,
                content = IcsExporter.export(listOf(event))
            )
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    if (success) {
                        "Event exported successfully"
                    } else {
                        "Failed to export event"
                    },
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun exportIcsToStorage(
        context: Context,
        title: String,
        content: String
    ): Boolean {
        val rootUri = settingsRepository.getStorageRoot()

        val dir = getOrCreateDirectory(
            context,
            rootUri,
            Constants.File.FLUX
        ) ?: return false

        return exportAsIcs(dir, context, title, content)
    }

    private fun exportAsIcs(
        dir: DocumentFile,
        context: Context,
        eventTitle: String,
        content: String
    ): Boolean {
        val safeName = eventTitle
            .trim()
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .ifBlank { "event" }

        val file = dir.createFile("text/calendar", "$safeName.ics") ?: return false

        return try {
            context.contentResolver
                .openOutputStream(file.uri)
                ?.use { stream ->
                    OutputStreamWriter(stream, Charsets.UTF_8).use { it.write(content) }
                } ?: return false

            true
        } catch (_: Exception) {
            false
        }
    }

}