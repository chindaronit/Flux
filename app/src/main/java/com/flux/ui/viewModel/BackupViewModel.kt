package com.flux.ui.viewModel

import android.content.Context
import android.net.Uri
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
import com.flux.other.crypto.BackupCredentialsStore
import com.flux.other.crypto.BackupCryptoException
import com.flux.other.crypto.BackupEncryptor
import com.flux.other.crypto.BackupFileFormat
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
    private val backupManager: BackupManager,
    private val backupEncryptor: BackupEncryptor,
    private val credentialsStore: BackupCredentialsStore
) : androidx.lifecycle.ViewModel() {

    private val _backupResult = MutableSharedFlow<Result<Unit>>()
    val backupResult = _backupResult.asSharedFlow()

    /** Fired when an encrypted file can't be opened with whatever password is currently stored (or none). */
    private val _passwordRequired = MutableSharedFlow<Uri>()
    val passwordRequired = _passwordRequired.asSharedFlow()

    /** Fired after a successful import — lets the UI re-check whether auto-backup is now unsafe. */
    private val _importCompleted = MutableSharedFlow<Unit>()
    val importCompleted = _importCompleted.asSharedFlow()

    private val backupJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    suspend fun exportBackup(context: Context) {
        val rootUri = settingsRepository.getStorageRoot()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val baseDir = getOrCreateDirectory(context, rootUri, Constants.File.FLUX)
                val backupDir = baseDir?.let { getOrCreateDirectory(context, it.uri, Constants.File.FLUX_BACKUP) }

                backupDir?.let { dir ->
                    val plainJson = writeJsonBackup()
                    val password = credentialsStore.getPassword()

                    val fileName = "${System.currentTimeMillis()}.json"
                    val file = dir.createFile("application/json", fileName)

                    val bytes = if (password != null) {
                        backupEncryptor.encrypt(plainJson.toByteArray(Charsets.UTF_8), password)
                            .also { password.fill('\u0000') }
                    } else {
                        plainJson.toByteArray(Charsets.UTF_8) // no password set → plaintext, unchanged legacy behavior
                    }

                    file?.let { saveBytesToUri(context, it.uri, bytes) }
                    _backupResult.emit(Result.success(Unit))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _backupResult.emit(Result.failure(e))
            }
        }
    }

    /** Entry point from the file picker. Tries the currently stored password first, silently. */
    fun importBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            attemptImport(context, uri, credentialsStore.getPassword())
        }
    }

    /** Entry point after the user manually supplies a password (stored one didn't work, or none exists). */
    fun importBackupWithPassword(context: Context, uri: Uri, password: CharArray) {
        viewModelScope.launch {
            attemptImport(context, uri, password)
        }
    }

    private suspend fun attemptImport(context: Context, uri: Uri, password: CharArray?) {
        try {
            val bytes = readBytesFromUri(context, uri)

            val plainJson = if (BackupFileFormat.isEncrypted(bytes)) {
                if (password == null) {
                    _passwordRequired.emit(uri)
                    return
                }
                try {
                    String(backupEncryptor.decrypt(bytes, password), Charsets.UTF_8)
                } catch (e: BackupCryptoException.WrongPasswordOrCorrupted) {
                    _passwordRequired.emit(uri) // wrong/old password — ask the user for this file's actual one
                    return
                } finally {
                    password.fill('\u0000')
                }
            } else {
                String(bytes, Charsets.UTF_8) // legacy plaintext backup, decoded exactly as before
            }

            uploadBackupToDatabase(context, plainJson)
            _backupResult.emit(Result.success(Unit))
            _importCompleted.emit(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            _backupResult.emit(Result.failure(e))
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
            settings = db.settingsDao.loadSetting() ?: SettingsModel(),
            progressBoardItems = db.progressBoardDao.getAllBoardItems()
        )
        backupJson.encodeToString(FluxBackup.serializer(), backup)
    }

    private suspend fun saveBytesToUri(context: Context, uri: Uri, bytes: ByteArray) =
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes); it.flush() }
                ?: throw IllegalStateException("Could not open OutputStream")
        }

    private suspend fun readBytesFromUri(context: Context, uri: Uri): ByteArray =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Could not open InputStream")
        }

    private suspend fun uploadBackupToDatabase(context: Context, json: String) = withContext(Dispatchers.IO) {
        val backup = backupJson.decodeFromString(FluxBackup.serializer(), json)

        backup.workspaces.forEach { ws -> if (!db.workspaceDao.exists(ws.workspaceId)) db.workspaceDao.upsertWorkspace(ws) }
        backup.notes.forEach { note -> if (!db.notesDao.exists(note.notesId)) db.notesDao.upsertNote(note) }

        backup.todos.forEach { todo ->
            if (!db.todoDao.exists(todo.id)) {
                if (todo.recurrence == RecurrenceRule.Weekly) scheduleNextReminder(context, todo.toScheduleRequest())
                db.todoDao.upsertList(todo)
            }
        }
        backup.todoInstances.forEach { instance ->
            if (!db.todoInstanceDao.exists(instance.todoId, instance.instanceDate)) db.todoInstanceDao.upsertTodoInstance(instance)
        }

        backup.habits.forEach { habit ->
            if (!db.habitDao.exists(habit.id)) {
                scheduleNextReminder(context, habit.toScheduleRequest())
                db.habitDao.upsertHabit(habit)
            }
        }
        backup.habitInstances.forEach { hi -> if (!db.habitInstanceDao.exists(hi.habitId, hi.instanceDate)) db.habitInstanceDao.upsertInstance(hi) }

        backup.journals.forEach { journal -> if (!db.journalDao.exists(journal.journalId)) db.journalDao.upsertEntry(journal) }
        backup.labels.forEach { label -> if (!db.labelDao.exists(label.labelId)) db.labelDao.upsertLabel(label) }

        backup.events.forEach { event ->
            if (!db.eventDao.exists(event.id)) {
                scheduleNextReminder(context, event.toScheduleRequest())
                db.eventDao.upsertEvent(event)
            }
        }
        backup.eventInstances.forEach { ei -> if (!db.eventInstanceDao.exists(ei.eventId, ei.instanceDate)) db.eventInstanceDao.upsertEventInstance(ei) }

        // --- Settings ---
        val current = db.settingsDao.loadSetting()
        val merged = if (backup.settings.storageRootUri != null) {
            backup.settings
        } else {
            backup.settings.copy(storageRootUri = current?.storageRootUri)
        }
        db.settingsDao.upsertSettings(merged)

        merged.storageRootUri?.let { tryRestoreUriPermission(context, it) }

        // NOTE: we schedule the WorkManager job based on the imported frequency regardless of
        // whether a password exists — BackupWorker itself is the guard that skips unsafe runs.
        // The UI-level check (isAutoBackupUnsafe) is what prompts the user afterward via importCompleted.
        val frequency = merged.backupFrequency
        fun mapDaysToFrequency(day: Int): BackupFrequency = when (day) {
            0 -> BackupFrequency.NEVER
            1 -> BackupFrequency.DAILY
            7 -> BackupFrequency.WEEKLY
            30 -> BackupFrequency.MONTHLY
            else -> BackupFrequency.NEVER
        }
        backupManager.scheduleBackup(mapDaysToFrequency(frequency))

        backup.progressBoardItems.forEach { item -> if (!db.progressBoardDao.exists(item.workspaceId)) db.progressBoardDao.upsertBoardItem(item) }
    }
}