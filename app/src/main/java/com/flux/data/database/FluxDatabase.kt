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
import com.flux.data.dao.SettingsDao
import com.flux.data.dao.TodoDao
import com.flux.data.dao.WorkspaceDao
import com.flux.data.model.Converter
import com.flux.data.model.EventInstanceModel
import com.flux.data.model.EventModel
import com.flux.data.model.HabitInstanceModel
import com.flux.data.model.HabitModel
import com.flux.data.model.JournalModel
import com.flux.data.model.LabelModel
import com.flux.data.model.NotesModel
import com.flux.data.model.SettingsModel
import com.flux.data.model.TodoModel
import com.flux.data.model.WorkspaceModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter
import kotlinx.serialization.json.Json
import java.util.UUID

@Database(
    entities = [EventModel::class, LabelModel::class, EventInstanceModel::class, SettingsModel::class, NotesModel::class, HabitModel::class, HabitInstanceModel::class, WorkspaceModel::class, TodoModel::class, JournalModel::class],
    version = 4,
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
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add fontNumber column with default value 0
        db.execSQL("ALTER TABLE SettingsModel ADD COLUMN fontNumber INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE NotesModel ADD COLUMN images TEXT NOT NULL DEFAULT '[]'")
        db.execSQL("ALTER TABLE HabitModel ADD COLUMN endDateTime INTEGER NOT NULL DEFAULT -1")
        db.execSQL("ALTER TABLE EventModel ADD COLUMN endDateTime INTEGER NOT NULL DEFAULT -1")
    }
}
val MIGRATION_3_4 = object : Migration(3, 4) {

    override fun migrate(db: SupportSQLiteDatabase) {

        /* ============================================================
           1. WorkspaceModel — selectedSpaces normalization (CRASH FIX)
           ============================================================ */

        db.execSQL("""
            ALTER TABLE WorkspaceModel
            ADD COLUMN selectedSpaces_new TEXT NOT NULL DEFAULT ''
        """)

        val wsCursor = db.query(
            "SELECT workspaceId, selectedSpaces FROM WorkspaceModel"
        )

        while (wsCursor.moveToNext()) {

            val id = wsCursor.getString(0)
            val raw = wsCursor.getString(1) ?: ""

            // ---- SAFE PARSE (CSV + JSON tolerant) ----
            val spaces = try {
                Json.decodeFromString<List<Int>>(raw).toMutableSet()
            } catch (_: Exception) {
                raw.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .toMutableSet()
            }

            // Merge Calendar (4) → Events (3)
            if (spaces.remove(4)) spaces.add(3)

            // Shift IDs > 4
            val normalized = spaces.map {
                if (it > 4) it - 1 else it
            }.toSet()

            // Store as CSV (since converter still CSV)
            val newCsv = normalized.joinToString(",")

            db.execSQL(
                "UPDATE WorkspaceModel SET selectedSpaces_new = ? WHERE workspaceId = ?",
                arrayOf(newCsv, id)
            )
        }

        wsCursor.close()

        db.execSQL("""
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

        db.execSQL("""
            INSERT INTO WorkspaceModel_new
            SELECT
                workspaceId, title, description, colorInd,
                cover, icon, passKey, isPinned, selectedSpaces_new
            FROM WorkspaceModel
        """)

        db.execSQL("DROP TABLE WorkspaceModel")
        db.execSQL("ALTER TABLE WorkspaceModel_new RENAME TO WorkspaceModel")


        /* ============================================================
           2. SettingsModel columns (4→6)
           ============================================================ */

        db.execSQL(
            "ALTER TABLE SettingsModel ADD COLUMN storageRootUri TEXT"
        )

        db.execSQL("""
            ALTER TABLE SettingsModel
            ADD COLUMN startWithReadView INTEGER NOT NULL DEFAULT 0
        """)

        db.execSQL("""
            ALTER TABLE SettingsModel
            ADD COLUMN isLineNumbersVisible INTEGER NOT NULL DEFAULT 0
        """)

        db.execSQL("""
            ALTER TABLE SettingsModel
            ADD COLUMN isLintValid INTEGER NOT NULL DEFAULT 0
        """)


        /* ============================================================
           3. TodoItem ID migration (6→7)
           ============================================================ */

        val todoCursor = db.query("SELECT id, items FROM TodoModel")
        val gson = Gson()

        while (todoCursor.moveToNext()) {

            val todoId = todoCursor.getString(0)
            val itemsJson = todoCursor.getString(1)

            try {
                data class OldTodoItem(
                    val value: String,
                    val isChecked: Boolean
                )

                data class NewTodoItem(
                    val id: String,
                    val value: String,
                    val isChecked: Boolean
                )

                val type = object : TypeToken<List<OldTodoItem>>() {}.type

                val oldItems: List<OldTodoItem> =
                    gson.fromJson(itemsJson, type) ?: continue

                val newItems = oldItems.map {
                    NewTodoItem(
                        id = UUID.randomUUID().toString(),
                        value = it.value,
                        isChecked = it.isChecked
                    )
                }

                db.execSQL(
                    "UPDATE TodoModel SET items = ? WHERE id = ?",
                    arrayOf(gson.toJson(newItems), todoId)
                )

            } catch (_: Exception) {
                // Skip malformed rows
            }
        }

        todoCursor.close()


        /* ============================================================
           4. Journal + Notes table rebuild (7→8)
           ============================================================ */

        db.execSQL("""
            CREATE TABLE journalmodel_new (
                journalId TEXT NOT NULL,
                workspaceId TEXT NOT NULL,
                text TEXT NOT NULL,
                dateTime INTEGER NOT NULL,
                PRIMARY KEY(journalId)
            )
        """)

        db.execSQL("""
            INSERT INTO journalmodel_new
            SELECT journalId, workspaceId, text, dateTime
            FROM JournalModel
        """)

        db.execSQL("DROP TABLE JournalModel")
        db.execSQL(
            "ALTER TABLE journalmodel_new RENAME TO JournalModel"
        )


        db.execSQL("""
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

        db.execSQL("""
            INSERT INTO NotesModel_new
            SELECT
                notesId,
                workspaceId,
                title,
                description,
                isPinned,
                labels,
                lastEdited
            FROM NotesModel
        """)

        db.execSQL("DROP TABLE NotesModel")
        db.execSQL(
            "ALTER TABLE NotesModel_new RENAME TO NotesModel"
        )


        /* ============================================================
           5. HTML → Markdown migration (8→9)
           ============================================================ */

        val converter = FlexmarkHtmlConverter.builder().build()

        db.query(
            "SELECT notesId, description FROM NotesModel"
        ).use { cursor ->

            while (cursor.moveToNext()) {

                val id = cursor.getString(0)
                val html = cursor.getString(1) ?: continue

                if (html.contains("<")) {

                    val markdown = converter.convert(html)

                    db.execSQL(
                        "UPDATE NotesModel SET description = ? WHERE notesId = ?",
                        arrayOf(markdown, id)
                    )
                }
            }
        }

        db.query(
            "SELECT journalId, text FROM JournalModel"
        ).use { cursor ->

            while (cursor.moveToNext()) {

                val id = cursor.getString(0)
                val html = cursor.getString(1) ?: continue

                if (html.contains("<")) {

                    val markdown = converter.convert(html)

                    db.execSQL(
                        "UPDATE JournalModel SET text = ? WHERE journalId = ?",
                        arrayOf(markdown, id)
                    )
                }
            }
        }
    }
}

val Migration_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE SettingsModel ADD COLUMN backupFrequency INTEGER NOT NULL DEFAULT 0")
    }
}
