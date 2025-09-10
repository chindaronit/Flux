package com.flux.ui.viewModel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.data.database.FluxBackup
import com.flux.data.database.FluxDatabase
import com.flux.di.IODispatcher
import com.flux.other.scheduleReminder
import com.flux.ui.components.getAdjustedTime
import com.flux.ui.screens.events.getNextValidReminderTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlinx.serialization.json.Json

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val db: FluxDatabase,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _backupResult = MutableSharedFlow<Result<Unit>>()
    val backupResult = _backupResult.asSharedFlow()

    fun exportBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val json = writeJsonBackup()
                saveToUri(context, uri, json)
                _backupResult.emit(Result.success(Unit))
            } catch (e: Exception) {
                e.printStackTrace()
                _backupResult.emit(Result.failure(e))
            }
        }
    }

    fun importBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val json = readFromUri(context, uri)
                uploadBackupToDatabase(context, json)
                _backupResult.emit(Result.success(Unit))
            } catch (e: Exception) {
                e.printStackTrace()
                _backupResult.emit(Result.failure(e))
            }
        }
    }

    private suspend fun writeJsonBackup(): String = withContext(ioDispatcher) {
        val backup = FluxBackup(
            workspaces = db.workspaceDao.getAll(),
            notes = db.notesDao.loadAllNotes(),
            todos = db.todoDao.loadAllLists(),
            habits = db.habitDao.loadAllHabits(),
            habitInstances = db.habitInstanceDao.loadAllInstances(),
            journals = db.journalDao.loadAllEntries(),
            labels = db.labelDao.getAll(),
            events = db.eventDao.loadAllEvents(),
            eventInstances = db.eventInstanceDao.getAll()
        )
        Json.encodeToString(FluxBackup.serializer(), backup)
    }

    private suspend fun saveToUri(context: Context, uri: Uri, json: String) =
        withContext(ioDispatcher) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray())
                outputStream.flush()
            } ?: throw IllegalStateException("Could not open OutputStream")
        }

    private suspend fun readFromUri(context: Context, uri: Uri): String =
        withContext(ioDispatcher) {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: throw IllegalStateException("Could not open InputStream")
        }

    private suspend fun uploadBackupToDatabase(context: Context, json: String) = withContext(ioDispatcher) {
        // Explicit serializer here too
        val backup = Json.decodeFromString(FluxBackup.serializer(), json)

        // --- Workspaces ---
        backup.workspaces.forEach { ws ->
            if (!db.workspaceDao.exists(ws.workspaceId)) db.workspaceDao.upsertWorkspace(ws)
        }

        // --- Notes ---
        backup.notes.forEach { note ->
            if (!db.notesDao.exists(note.notesId)) db.notesDao.upsertNote(note)
        }

        // --- Todos ---
        backup.todos.forEach { todo ->
            if (!db.todoDao.exists(todo.id)) db.todoDao.upsertList(todo)
        }

        // --- Habits ---
        backup.habits.forEach { habit ->
            if (!db.habitDao.exists(habit.habitId)){
                scheduleReminder(
                    context = context,
                    id = habit.habitId,
                    type = "HABIT",
                    repeat = "DAILY",
                    timeInMillis = getAdjustedTime(habit.startDateTime),
                    title = habit.title,
                    description = habit.description
                )
                db.habitDao.upsertHabit(habit)
            }
        }

        // --- Habit Instances ---
        backup.habitInstances.forEach { hi ->
            if (!db.habitInstanceDao.exists(hi.habitId, hi.instanceDate)) db.habitInstanceDao.upsertInstance(hi)
        }

        // --- Journals ---
        backup.journals.forEach { journal ->
            if (!db.journalDao.exists(journal.journalId)) db.journalDao.upsertEntry(journal)
        }

        // --- Labels ---
        backup.labels.forEach { label ->
            if (!db.labelDao.exists(label.labelId)) db.labelDao.upsertLabel(label)
        }

        // --- Events ---
        backup.events.forEach { event ->
            if (!db.eventDao.exists(event.eventId)){
                scheduleReminder(
                    context = context,
                    id = event.eventId,
                    type = "EVENT",
                    repeat = event.repetition,
                    timeInMillis = getNextValidReminderTime(event.startDateTime, 0L, event.repetition),
                    title = event.title,
                    description = event.description
                )
                db.eventDao.upsertEvent(event)
            }
        }

        // --- Event Instances ---
        backup.eventInstances.forEach { ei ->
            if (!db.eventInstanceDao.exists(ei.eventId, ei.instanceDate)) db.eventInstanceDao.upsertEventInstance(ei)
        }
    }
}
