package com.omnipad.ui.viewModel

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnipad.data.model.EventInstanceModel
import com.omnipad.data.model.EventExceptionModel
import com.omnipad.data.model.EventOccurrenceColorModel
import com.omnipad.data.model.EventModel
import com.omnipad.data.model.toScheduleRequest
import com.omnipad.data.repository.EventRepository
import com.omnipad.data.repository.SettingsRepository
import com.omnipad.other.Constants
import com.omnipad.other.IcsExporter
import com.omnipad.other.cancelReminder
import com.omnipad.other.copyDocumentAttachment
import com.omnipad.other.getOrCreateDirectory
import com.omnipad.other.scheduleNextReminder
import com.omnipad.R
import com.omnipad.ui.events.TaskEvents
import com.omnipad.ui.state.EventState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
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
                combine(
                    repository.loadEventData(),
                    repository.loadEventInstanceData(),
                    repository.loadEventExceptionData()
                ) { allEvents, allInstances, exceptions ->
                    Triple(allEvents, allInstances, exceptions)
                },
                repository.loadEventOccurrenceColorData()
            ) { eventData, occurrenceColors ->
                eventData to occurrenceColors
            }.collect { (eventData, occurrenceColors) ->
                val (allEvents, allInstances, exceptions) = eventData
                updateState {
                    it.copy(
                        isAllEventsLoading = false,
                        allEvent = allEvents,
                        allEventInstances = allInstances,
                        eventExceptions = exceptions,
                        eventOccurrenceColors = occurrenceColors
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
            is TaskEvents.SetEventColor -> setEventColor(event)
            is TaskEvents.DeleteAllWorkspaceEvents -> deleteWorkspaceEvents(event.workspaceId, event.context)
            is TaskEvents.ImportIcsEvents -> importIcsEvents(event.context, event.events)
            is TaskEvents.ExportIcsEvents -> exportICSEvents(event.context, event.event)
            is TaskEvents.ImportEventAttachments ->
                importEventAttachments(event.context, event.uris, event.event)
            is TaskEvents.RemoveEventAttachment ->
                removeEventAttachment(event.eventId, event.fileName)
        }
    }

    private fun detachOccurrence(event: TaskEvents.DetachOccurrence) {
        viewModelScope.launch(Dispatchers.IO) {
            val colorArgb = state.value.eventOccurrenceColors.firstOrNull {
                it.eventId == event.exception.eventId &&
                    it.occurrenceDate == event.exception.occurrenceDate
            }?.colorArgb ?: state.value.allEvent.firstOrNull {
                it.id == event.exception.eventId
            }?.colorArgb
            val detachedEvent = event.event.copy(colorArgb = colorArgb)
            repository.detachEventOccurrence(event.exception, detachedEvent)
            scheduleNextReminder(event.context, detachedEvent.toScheduleRequest())
        }
    }

    private fun deleteOccurrence(event: TaskEvents.DeleteOccurrence) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteEventOccurrence(event.exception)
        }
    }

    private fun setEventColor(event: TaskEvents.SetEventColor) {
        viewModelScope.launch(Dispatchers.IO) {
            if (event.applyToSeries) {
                repository.setEventColorForSeries(event.event.copy(colorArgb = event.colorArgb))
            } else if (event.colorArgb == null) {
                repository.deleteEventOccurrenceColor(event.event.id, event.occurrenceDate)
            } else {
                repository.upsertEventOccurrenceColor(
                    EventOccurrenceColorModel(
                        eventId = event.event.id,
                        workspaceId = event.event.workspaceId,
                        occurrenceDate = event.occurrenceDate,
                        colorArgb = event.colorArgb
                    )
                )
            }
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
            val existingEvent = repository.loadAllEvents().firstOrNull { it.id == data.id }
            val isNewEvent = existingEvent == null
            val eventToSave = if (isNewEvent && data.colorArgb == null) {
                data.copy(
                    colorArgb = settingsRepository.loadSettings()
                        .firstOrNull()
                        ?.defaultNewEventColorArgb
                )
            } else {
                data
            }
            existingEvent?.let { cancelReminder(context, it.toScheduleRequest()) }
            cancelReminder(context, eventToSave.toScheduleRequest())
            repository.upsertEvent(eventToSave)
            scheduleNextReminder(context = context, eventToSave.toScheduleRequest())
        }
    }

    private fun importIcsEvents(context: Context, data: List<EventModel>) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingEvents = repository.loadAllEvents().associateBy { it.id }
            data.forEach { imported ->
                existingEvents[imported.id]?.let { cancelReminder(context, it.toScheduleRequest()) }
                cancelReminder(context, imported.toScheduleRequest())
            }
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

    private fun importEventAttachments(context: Context, uris: List<Uri>, event: EventModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentEvent = repository.loadAllEvents().firstOrNull { it.id == event.id } ?: event
            val importedFileNames = mutableListOf<String>()
            val failures = mutableListOf<String>()

            uris.forEach { uri ->
                try {
                    importedFileNames += copyDocumentAttachment(
                        context,
                        settingsRepository.getStorageRoot(),
                        uri
                    ).fileName
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    failures += exception.localizedMessage ?: "Could not attach a selected file"
                }
            }

            if (importedFileNames.isNotEmpty()) {
                repository.upsertEvent(
                    currentEvent.copy(
                        attachments = (currentEvent.attachments + importedFileNames).distinct()
                    )
                )
            }

            withContext(Dispatchers.Main) {
                val message = when {
                    failures.isEmpty() -> context.getString(
                        R.string.event_attachments_added,
                        importedFileNames.size
                    )
                    importedFileNames.isEmpty() -> context.getString(
                        R.string.event_attachments_failed,
                        failures.joinToString("\n")
                    )
                    else -> context.getString(
                        R.string.event_attachments_partial_failure,
                        importedFileNames.size,
                        failures.joinToString("\n")
                    )
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun removeEventAttachment(eventId: String, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val event = repository.loadAllEvents().firstOrNull { it.id == eventId } ?: return@launch
            repository.upsertEvent(
                event.copy(attachments = event.attachments.filterNot { it == fileName })
            )
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
            Constants.File.OMNIPAD
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