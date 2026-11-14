package com.flux.other

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.flux.data.database.FluxBackup
import com.flux.data.database.FluxDatabase
import com.flux.data.model.RecurrenceRule
import com.flux.data.model.toScheduleRequest

internal class AndroidBackupDocument(
    private val context: Context,
    private val document: DocumentFile
) : BackupDocument {
    override val name: String?
        get() = document.name
    override val isDirectory: Boolean
        get() = document.isDirectory
    override val isFile: Boolean
        get() = document.isFile
    override val type: String?
        get() = document.type
    override val uri: String
        get() = document.uri.toString()

    override fun findFile(name: String): BackupDocument? =
        document.findFile(name)?.let { AndroidBackupDocument(context, it) }

    override fun listFiles(): List<BackupDocument> =
        document.listFiles().map { AndroidBackupDocument(context, it) }

    override fun createDirectory(name: String): BackupDocument? =
        document.createDirectory(name)?.let { AndroidBackupDocument(context, it) }

    override fun createFile(mimeType: String, name: String): BackupDocument? =
        document.createFile(mimeType, name)?.let { AndroidBackupDocument(context, it) }

    override fun openInputStream() = context.contentResolver.openInputStream(document.uri)

    override fun openOutputStream() = context.contentResolver.openOutputStream(document.uri)

    override fun delete(): Boolean = document.delete()
}

internal class RoomBackupDataStore(
    private val context: Context,
    private val db: FluxDatabase
) : BackupDataStore {
    override suspend fun loadBackup(): FluxBackup = FluxBackup(
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
        eventExceptions = db.eventExceptionDao.getAll(),
        eventOccurrenceColors = db.eventOccurrenceColorDao.getAll(),
        progressBoardItems = db.progressBoardDao.getAllBoardItems()
    )

    override suspend fun replaceWith(backup: FluxBackup) {
        val previousTodos = db.todoDao.loadAllLists()
            .filter { it.recurrence == RecurrenceRule.Weekly }
        val previousHabits = db.habitDao.loadAllHabits()
        val previousEvents = db.eventDao.loadAllEvents()
        db.withTransaction {
            db.todoInstanceDao.deleteAllInstances()
            db.eventExceptionDao.deleteAll()
            db.eventOccurrenceColorDao.deleteAll()
            db.eventInstanceDao.deleteAll()
            db.habitInstanceDao.deleteAllInstances()
            db.progressBoardDao.deleteAll()
            db.notesDao.deleteAll()
            db.journalDao.deleteAll()
            db.labelDao.deleteAll()
            db.todoDao.deleteAll()
            db.habitDao.deleteAll()
            db.eventDao.deleteAll()
            db.workspaceDao.deleteAll()

            db.workspaceDao.upsertWorkspaces(backup.workspaces)
            backup.notes.forEach { db.notesDao.upsertNote(it) }
            backup.todos.forEach { db.todoDao.upsertList(it) }
            backup.todoInstances.forEach { db.todoInstanceDao.upsertTodoInstance(it) }
            backup.habits.forEach { db.habitDao.upsertHabit(it) }
            backup.habitInstances.forEach { db.habitInstanceDao.upsertInstance(it) }
            backup.journals.forEach { db.journalDao.upsertEntry(it) }
            backup.labels.forEach { db.labelDao.upsertLabel(it) }
            backup.events.forEach { db.eventDao.upsertEvent(it) }
            backup.eventInstances.forEach { db.eventInstanceDao.upsertEventInstance(it) }
            backup.eventExceptions.forEach { db.eventExceptionDao.upsert(it) }
            backup.eventOccurrenceColors.forEach { db.eventOccurrenceColorDao.upsert(it) }
            backup.progressBoardItems.forEach { db.progressBoardDao.upsertBoardItem(it) }
        }

        previousTodos.forEach { cancelReminder(context, it.toScheduleRequest()) }
        previousHabits.forEach { cancelReminder(context, it.toScheduleRequest()) }
        previousEvents.forEach { cancelReminder(context, it.toScheduleRequest()) }
        backup.todos
            .filter { it.recurrence == RecurrenceRule.Weekly }
            .forEach { scheduleNextReminder(context, it.toScheduleRequest()) }
        backup.habits.forEach { scheduleNextReminder(context, it.toScheduleRequest()) }
        backup.events.forEach { scheduleNextReminder(context, it.toScheduleRequest()) }
    }
}
