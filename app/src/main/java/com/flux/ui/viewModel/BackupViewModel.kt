package com.flux.ui.viewModel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.data.database.FluxBackup
import com.flux.data.database.FluxDatabase
import com.flux.data.model.RecurrenceRule
import com.flux.data.model.SettingsModel
import com.flux.data.model.toScheduleRequest
import com.flux.data.repository.SettingsRepository
import com.flux.other.BackupFrequency
import com.flux.other.BackupManager
import com.flux.other.Constants
import com.flux.other.getOrCreateDirectory
import com.flux.other.scheduleNextReminder
import com.flux.other.tryRestoreUriPermission
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlinx.serialization.json.Json

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val db: FluxDatabase,
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _backupResult = MutableSharedFlow<Result<Unit>>()
    val backupResult = _backupResult.asSharedFlow()
    private val backupJson = Json {
        ignoreUnknownKeys = true   // old backups won't have new fields → skip them
        coerceInputValues = true   // null where non-null expected → use default
        encodeDefaults = true      // ensure new fields are always written on export
        classDiscriminator = "type"
    }

    suspend fun exportBackup(context: Context) {
        val rootUri = settingsRepository.getStorageRoot()

        viewModelScope.launch(Dispatchers.IO) {
            val baseDir = getOrCreateDirectory(context, rootUri, Constants.File.FLUX)
            val backupDir = baseDir?.let { dir ->
                getOrCreateDirectory(context, dir.uri, Constants.File.FLUX_BACKUP)
            }

            backupDir?.let { dir ->
                val json = writeJsonBackup()
                try {
                    val fileName = "${System.currentTimeMillis()}.json"
                    val file = dir.createFile("application/json", fileName)

                    file?.let { docFile ->
                        saveToUri(context, docFile.uri, json)
                    }

                    _backupResult.emit(Result.success(Unit))
                } catch (e: Exception) {
                    e.printStackTrace()
                    _backupResult.emit(Result.failure(e))
                }
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

    private suspend fun writeJsonBackup(): String = withContext(Dispatchers.IO) {
        val backup = FluxBackup(
            workspaces = db.workspaceDao.getAll(),
            notes = db.notesDao.loadAllNotes(),
            todos = db.todoDao.loadAllLists(),
            todoInstances = db.todoInstanceDao.loadAllInstances(),
            habits = db.habitDao.loadAllHabits(),
            habitInstances = db.habitInstanceDao.loadAllInstances(),
            journals = db.journalDao.loadAllEntries(),
            labels = db.labelDao.getAll(),
            events = db.eventDao.loadAllEvents(),
            eventInstances = db.eventInstanceDao.getAll(),
            settings = db.settingsDao.loadSetting()?: SettingsModel(),
            progressBoardItems = db.progressBoardDao.getAllBoardItems()
        )
        backupJson.encodeToString(FluxBackup.serializer(), backup)
    }

    private suspend fun saveToUri(context: Context, uri: Uri, json: String) =
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray())
                outputStream.flush()
            } ?: throw IllegalStateException("Could not open OutputStream")
        }

    private suspend fun readFromUri(context: Context, uri: Uri): String =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: throw IllegalStateException("Could not open InputStream")
        }

    private suspend fun uploadBackupToDatabase(context: Context, json: String) = withContext(Dispatchers.IO) {
        val backup = backupJson.decodeFromString(FluxBackup.serializer(), json)

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
            if (!db.todoDao.exists(todo.id)) {
                if(todo.recurrence== RecurrenceRule.Weekly) scheduleNextReminder(context, todo.toScheduleRequest())
                db.todoDao.upsertList(todo)
            }
        }

        backup.todoInstances.forEach { instance ->
            if (!db.todoInstanceDao.exists(instance.todoId, instance.instanceDate))
                db.todoInstanceDao.upsertTodoInstance(instance)
        }

        // --- Habits ---
        backup.habits.forEach { habit ->
            if (!db.habitDao.exists(habit.id)) {
                scheduleNextReminder(context, habit.toScheduleRequest())
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
            if (!db.eventDao.exists(event.id)){
                scheduleNextReminder(context, event.toScheduleRequest())
                db.eventDao.upsertEvent(event)
            }
        }

        // --- Event Instances ---
        backup.eventInstances.forEach { ei ->
            if (!db.eventInstanceDao.exists(ei.eventId, ei.instanceDate))
                db.eventInstanceDao.upsertEventInstance(ei)
        }

        // --- Settings ---
        val current = db.settingsDao.loadSetting()

        val merged = if (backup.settings.storageRootUri != null) {
            backup.settings
        } else {
            backup.settings.copy(storageRootUri = current?.storageRootUri)
        }

        db.settingsDao.upsertSettings(merged)

        merged.storageRootUri?.let {
            tryRestoreUriPermission(context, it)
        }

        val frequency = merged.backupFrequency
        fun mapDaysToFrequency(day: Int): BackupFrequency = when (day) {
            0 -> BackupFrequency.NEVER
            1 -> BackupFrequency.DAILY
            7 -> BackupFrequency.WEEKLY
            30 -> BackupFrequency.MONTHLY
            else -> BackupFrequency.NEVER
        }

        backupManager.scheduleBackup(mapDaysToFrequency(frequency))

        // --- Progress Board ---
        backup.progressBoardItems.forEach { item ->
            if (!db.progressBoardDao.exists(item.workspaceId)) db.progressBoardDao.upsertBoardItem(item)
        }
    }
}
