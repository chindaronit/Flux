package com.flux.other

import android.content.Context
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.flux.R
import com.flux.data.database.FluxBackup
import com.flux.data.model.SettingsModel
import com.flux.di.DataModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter

enum class BackupFrequency(val days: Int, val textRes: Int) {
    NEVER(0, R.string.never),
    DAILY(1, R.string.daily),
    WEEKLY(7, R.string.weekly),
    MONTHLY(30, R.string.monthly);
}

class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val context = applicationContext
            val fluxDatabase = DataModule.provideFluxDatabase(context)
            val settingsDao = DataModule.provideSettingsDao(fluxDatabase)
            val notesDao = DataModule.provideNotesDao(fluxDatabase)
            val workspaceDao = DataModule.provideWorkspaceDao(fluxDatabase)
            val todoDao = DataModule.provideTodoDao(fluxDatabase)
            val habitDao = DataModule.provideHabitDao(fluxDatabase)
            val habitInstanceDao = DataModule.provideHabitInstanceDao(fluxDatabase)
            val journalDao = DataModule.provideJournalDao(fluxDatabase)
            val labelDao = DataModule.provideLabelDao(fluxDatabase)
            val eventDao = DataModule.provideEventDao(fluxDatabase)
            val eventInstanceDao = DataModule.provideEventInstanceDao(fluxDatabase)
            val settings = settingsDao.loadSetting()
            val rootUri = settings?.storageRootUri?.toUri()?: "".toUri()

            val openNoteDir = getOrCreateDirectory(context, rootUri, Constants.File.FLUX)
            val backupDir = openNoteDir?.let { dir ->
                getOrCreateDirectory(context, dir.uri, Constants.File.FLUX_BACKUP)
            }
            backupDir?.let { dir ->
                val backup = FluxBackup(
                    workspaces = workspaceDao.getAll(),
                    notes = notesDao.loadAllNotes(),
                    todos = todoDao.loadAllLists(),
                    habits = habitDao.loadAllHabits(),
                    habitInstances = habitInstanceDao.loadAllInstances(),
                    journals = journalDao.loadAllEntries(),
                    labels = labelDao.getAll(),
                    events = eventDao.loadAllEvents(),
                    eventInstances = eventInstanceDao.getAll(),
                    settings = settingsDao.loadSetting()?: SettingsModel()
                )
                val json = Json.encodeToString(FluxBackup.serializer(), backup)

                val fileName = "${System.currentTimeMillis()}.json"
                val file = dir.createFile("application/json", fileName)

                file?.let { docFile ->
                    context.contentResolver.openOutputStream(docFile.uri)
                        ?.use { outputStream ->
                            OutputStreamWriter(outputStream).use { writer ->
                                writer.write(json)
                            }
                        }
                }
            }

            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }
}