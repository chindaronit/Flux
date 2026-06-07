package com.flux.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flux.data.dao.EventDao
import com.flux.data.dao.EventInstanceDao
import com.flux.data.dao.HabitInstanceDao
import com.flux.data.dao.HabitsDao
import com.flux.data.dao.JournalDao
import com.flux.data.dao.LabelDao
import com.flux.data.dao.NotesDao
import com.flux.data.dao.ProgressBoardDao
import com.flux.data.dao.SettingsDao
import com.flux.data.dao.TodoDao
import com.flux.data.dao.TodoInstanceDao
import com.flux.data.dao.WorkspaceDao
import com.flux.data.model.Converter
import com.flux.data.model.EventInstanceModel
import com.flux.data.model.EventModel
import com.flux.data.model.HabitInstanceModel
import com.flux.data.model.HabitModel
import com.flux.data.model.JournalModel
import com.flux.data.model.LabelModel
import com.flux.data.model.NotesModel
import com.flux.data.model.ProgressBoardModel
import com.flux.data.model.SettingsModel
import com.flux.data.model.TodoInstance
import com.flux.data.model.TodoModel
import com.flux.data.model.WorkspaceModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter
import kotlinx.serialization.json.Json
import java.util.UUID

@Database(
    entities = [EventModel::class, LabelModel::class, EventInstanceModel::class, SettingsModel::class, NotesModel::class, HabitModel::class, HabitInstanceModel::class, WorkspaceModel::class, TodoModel::class, JournalModel::class, ProgressBoardModel::class, TodoInstance::class],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converter::class)
abstract class FluxDatabase : RoomDatabase() {
    abstract val settingsDao: SettingsDao
    abstract val eventDao: EventDao
    abstract val notesDao: NotesDao
    abstract val workspaceDao: WorkspaceDao
    abstract val eventInstanceDao: EventInstanceDao
    abstract val habitDao: HabitsDao
    abstract val habitInstanceDao: HabitInstanceDao
    abstract val journalDao: JournalDao
    abstract val todoDao: TodoDao
    abstract val labelDao: LabelDao
    abstract val progressBoardDao: ProgressBoardDao
    abstract val todoInstanceDao: TodoInstanceDao
}

private fun SupportSQLiteDatabase.safeExec(sql: String) {
    try { execSQL(sql) } catch (_: Exception) {}
}

private fun SupportSQLiteDatabase.columnExists(table: String, column: String): Boolean {
    query("PRAGMA table_info($table)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (cursor.getString(nameIndex).equals(column, ignoreCase = true)) return true
        }
    }
    return false
}

private fun SupportSQLiteDatabase.tableExists(table: String): Boolean {
    query("SELECT name FROM sqlite_master WHERE type='table' AND name='$table'").use {
        return it.moveToFirst()
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        if (!db.columnExists("SettingsModel", "fontNumber"))
            db.safeExec("ALTER TABLE SettingsModel ADD COLUMN fontNumber INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        if (!db.columnExists("NotesModel", "images"))
            db.safeExec("ALTER TABLE NotesModel ADD COLUMN images TEXT NOT NULL DEFAULT '[]'")
        if (!db.columnExists("HabitModel", "endDateTime"))
            db.safeExec("ALTER TABLE HabitModel ADD COLUMN endDateTime INTEGER NOT NULL DEFAULT -1")
        if (!db.columnExists("EventModel", "endDateTime"))
            db.safeExec("ALTER TABLE EventModel ADD COLUMN endDateTime INTEGER NOT NULL DEFAULT -1")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // 1. WorkspaceModel selectedSpaces normalization
        if (!db.columnExists("WorkspaceModel", "selectedSpaces_new")) {
            db.safeExec("ALTER TABLE WorkspaceModel ADD COLUMN selectedSpaces_new TEXT NOT NULL DEFAULT ''")
        }

        val wsCursor = db.query("SELECT workspaceId, selectedSpaces FROM WorkspaceModel")
        while (wsCursor.moveToNext()) {
            val id = wsCursor.getString(0)
            val raw = wsCursor.getString(1) ?: ""
            val spaces = try {
                Json.decodeFromString<List<Int>>(raw).toMutableSet()
            } catch (_: Exception) {
                raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toMutableSet()
            }
            if (spaces.remove(4)) spaces.add(3)
            val normalized = spaces.map { if (it > 4) it - 1 else it }.toSet()
            db.safeExec("UPDATE WorkspaceModel SET selectedSpaces_new = '${normalized.joinToString(",")}' WHERE workspaceId = '$id'")
        }
        wsCursor.close()

        if (!db.tableExists("WorkspaceModel_new")) {
            db.safeExec("""
                CREATE TABLE WorkspaceModel_new (
                    workspaceId TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    colorInd INTEGER NOT NULL,
                    cover TEXT NOT NULL,
                    icon INTEGER NOT NULL,
                    passKey TEXT NOT NULL,
                    isPinned INTEGER NOT NULL,
                    selectedSpaces TEXT NOT NULL
                )
            """)
            db.safeExec("""
                INSERT INTO WorkspaceModel_new
                SELECT workspaceId, title, description, colorInd, cover, icon, passKey, isPinned, selectedSpaces_new
                FROM WorkspaceModel
            """)
            db.safeExec("DROP TABLE WorkspaceModel")
            db.safeExec("ALTER TABLE WorkspaceModel_new RENAME TO WorkspaceModel")
        }

        // 2. SettingsModel columns
        if (!db.columnExists("SettingsModel", "storageRootUri"))
            db.safeExec("ALTER TABLE SettingsModel ADD COLUMN storageRootUri TEXT")
        if (!db.columnExists("SettingsModel", "startWithReadView"))
            db.safeExec("ALTER TABLE SettingsModel ADD COLUMN startWithReadView INTEGER NOT NULL DEFAULT 0")
        if (!db.columnExists("SettingsModel", "isLineNumbersVisible"))
            db.safeExec("ALTER TABLE SettingsModel ADD COLUMN isLineNumbersVisible INTEGER NOT NULL DEFAULT 0")
        if (!db.columnExists("SettingsModel", "isLintValid"))
            db.safeExec("ALTER TABLE SettingsModel ADD COLUMN isLintValid INTEGER NOT NULL DEFAULT 0")

        // 3. TodoItem ID migration
        val todoCursor = db.query("SELECT id, items FROM TodoModel")
        val gson = Gson()
        while (todoCursor.moveToNext()) {
            val todoId = todoCursor.getString(0)
            val itemsJson = todoCursor.getString(1)
            try {
                data class OldTodoItem(val value: String, val isChecked: Boolean)
                data class NewTodoItem(val id: String, val value: String, val isChecked: Boolean)
                val type = object : TypeToken<List<OldTodoItem>>() {}.type
                val oldItems: List<OldTodoItem> = gson.fromJson(itemsJson, type) ?: continue
                // Skip if already migrated (items already have id field)
                if (itemsJson.contains("\"id\"")) continue
                val newItems = oldItems.map { NewTodoItem(UUID.randomUUID().toString(), it.value, it.isChecked) }
                db.execSQL("UPDATE TodoModel SET items = ? WHERE id = ?", arrayOf(gson.toJson(newItems), todoId))
            } catch (_: Exception) {}
        }
        todoCursor.close()

        // 4. Journal + Notes table rebuild
        if (!db.tableExists("journalmodel_new")) {
            db.safeExec("""
                CREATE TABLE journalmodel_new (
                    journalId TEXT NOT NULL,
                    workspaceId TEXT NOT NULL,
                    text TEXT NOT NULL,
                    dateTime INTEGER NOT NULL,
                    PRIMARY KEY(journalId)
                )
            """)
            db.safeExec("INSERT INTO journalmodel_new SELECT journalId, workspaceId, text, dateTime FROM JournalModel")
            db.safeExec("DROP TABLE JournalModel")
            db.safeExec("ALTER TABLE journalmodel_new RENAME TO JournalModel")
        }

        if (!db.tableExists("NotesModel_new")) {
            db.safeExec("""
                CREATE TABLE NotesModel_new (
                    notesId TEXT NOT NULL,
                    workspaceId TEXT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    isPinned INTEGER NOT NULL,
                    labels TEXT NOT NULL,
                    lastEdited INTEGER NOT NULL,
                    PRIMARY KEY(notesId)
                )
            """)
            db.safeExec("""
                INSERT INTO NotesModel_new
                SELECT notesId, workspaceId, title, description, isPinned, labels, lastEdited
                FROM NotesModel
            """)
            db.safeExec("DROP TABLE NotesModel")
            db.safeExec("ALTER TABLE NotesModel_new RENAME TO NotesModel")
        }

        // 5. HTML → Markdown migration
        val converter = FlexmarkHtmlConverter.builder().build()
        db.query("SELECT notesId, description FROM NotesModel").use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val html = cursor.getString(1) ?: continue
                if (html.contains("<")) {
                    db.execSQL("UPDATE NotesModel SET description = ? WHERE notesId = ?",
                        arrayOf(converter.convert(html), id))
                }
            }
        }
        db.query("SELECT journalId, text FROM JournalModel").use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val html = cursor.getString(1) ?: continue
                if (html.contains("<")) {
                    db.execSQL("UPDATE JournalModel SET text = ? WHERE journalId = ?",
                        arrayOf(converter.convert(html), id))
                }
            }
        }
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        if (!db.columnExists("SettingsModel", "backupFrequency"))
            db.safeExec("ALTER TABLE SettingsModel ADD COLUMN backupFrequency INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.safeExec("""
            CREATE TABLE IF NOT EXISTS `ProgressBoardModel` (
                `itemId` TEXT NOT NULL,
                `workspaceId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `startDate` INTEGER NOT NULL,
                `endDate` INTEGER NOT NULL,
                `icon` INTEGER NOT NULL,
                `status` INTEGER NOT NULL,
                PRIMARY KEY(`itemId`)
            )
        """)
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        if (!db.columnExists("SettingsModel", "useSystemTimeFormat"))
            db.safeExec("ALTER TABLE SettingsModel ADD COLUMN useSystemTimeFormat INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // ---------------------- HabitModel rebuild ----------------------
        if (!db.tableExists("HabitModel_new")) {

            db.safeExec("""
                CREATE TABLE HabitModel_new (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    recurrence TEXT NOT NULL,
                    startDateTime INTEGER NOT NULL,
                    endDateTime INTEGER NOT NULL,
                    notificationOffset INTEGER NOT NULL,
                    workspaceId TEXT NOT NULL,
                    habitConfig TEXT NOT NULL
                )
            """)

            // Default HabitConfig → Simple
            val defaultConfig = """{"type":"Simple"}"""

            db.safeExec("""
                INSERT INTO HabitModel_new (
                    id, title, description, recurrence,
                    startDateTime, endDateTime,
                    notificationOffset, workspaceId, habitConfig
                )
                SELECT 
                    id, title, description, recurrence,
                    startDateTime, endDateTime,
                    notificationOffset, workspaceId,
                    '$defaultConfig'
                FROM HabitModel
            """)

            db.safeExec("DROP TABLE HabitModel")
            db.safeExec("ALTER TABLE HabitModel_new RENAME TO HabitModel")
        }

        if (!db.columnExists("HabitInstanceModel", "timeSpent"))
            db.safeExec("ALTER TABLE HabitInstanceModel ADD COLUMN timeSpent INTEGER NOT NULL DEFAULT 0")

        if (!db.columnExists("HabitInstanceModel", "isRunning"))
            db.safeExec("ALTER TABLE HabitInstanceModel ADD COLUMN isRunning INTEGER NOT NULL DEFAULT 0")

        if (!db.columnExists("HabitInstanceModel", "count"))
            db.safeExec("ALTER TABLE HabitInstanceModel ADD COLUMN count INTEGER NOT NULL DEFAULT 0")

        if (!db.tableExists("WorkspaceModel_new")) {
            db.safeExec("""
                CREATE TABLE WorkspaceModel_new (
                    workspaceId TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    colorInd INTEGER NOT NULL,
                    cover TEXT NOT NULL,
                    icon INTEGER NOT NULL,
                    passKey TEXT,
                    isPinned INTEGER NOT NULL,
                    selectedSpaces TEXT NOT NULL
                )
            """.trimIndent())

            db.safeExec("""
                INSERT INTO WorkspaceModel_new (
                    workspaceId, title, description, colorInd,
                    cover, icon, passKey, isPinned, selectedSpaces
                )
                SELECT 
                    workspaceId, title, description, colorInd,
                    cover, icon,
                    CASE WHEN passKey = '' THEN NULL ELSE passKey END,
                    isPinned, selectedSpaces
                FROM WorkspaceModel
            """.trimIndent())

            db.safeExec("DROP TABLE WorkspaceModel")
            db.safeExec("ALTER TABLE WorkspaceModel_new RENAME TO WorkspaceModel")
        }

        if (!db.columnExists("JournalModel", "labels"))
            db.safeExec("ALTER TABLE JournalModel ADD COLUMN labels TEXT NOT NULL DEFAULT '[]'")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // 1. Create corrected TodoModel table with proper constraints
        db.safeExec("""
            CREATE TABLE TodoModel_new (
                id TEXT NOT NULL PRIMARY KEY,
                workspaceId TEXT NOT NULL DEFAULT '',
                title TEXT NOT NULL DEFAULT '',
                items TEXT NOT NULL DEFAULT '[]',
                startDateTime INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()},
                recurrence TEXT NOT NULL DEFAULT '{"type":"NONE"}'
            )
        """.trimIndent())

        // 2. Copy existing data, coercing any nulls
        db.safeExec("""
            INSERT INTO TodoModel_new (id, workspaceId, title, items, startDateTime, recurrence)
                SELECT 
                    id,
                    COALESCE(workspaceId, ''),
                    COALESCE(title, ''),
                    COALESCE(items, '[]'),
                    ${System.currentTimeMillis()},   
                    '{"type":"NONE"}'                
                FROM TodoModel
        """.trimIndent())

        // 3. Swap tables
        db.safeExec("DROP TABLE TodoModel")
        db.safeExec("ALTER TABLE TodoModel_new RENAME TO TodoModel")

        // 4. Create TodoInstance table
        if (!db.tableExists("TodoInstance")) {
            db.safeExec("""
                CREATE TABLE TodoInstance (
                    id TEXT NOT NULL PRIMARY KEY,
                    todoId TEXT NOT NULL,
                    instanceDate INTEGER NOT NULL,
                    workspaceId TEXT NOT NULL,
                    items TEXT NOT NULL DEFAULT '[]',
                    FOREIGN KEY (todoId) REFERENCES TodoModel(id) ON DELETE CASCADE
                )
            """.trimIndent())
        }

        // 5. Create unique index on TodoInstance
        db.safeExec("""
            CREATE UNIQUE INDEX IF NOT EXISTS 
            index_TodoInstance_todoId_instanceDate
            ON TodoInstance(todoId, instanceDate)
        """.trimIndent())
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        if (!db.columnExists("SettingsModel", "notesPreviewMode"))
            db.safeExec("ALTER TABLE SettingsModel ADD COLUMN notesPreviewMode INTEGER NOT NULL DEFAULT 1")
    }
}