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
import kotlinx.serialization.json.Json

@Database(
    entities = [EventModel::class, LabelModel::class, EventInstanceModel::class, SettingsModel::class, NotesModel::class, HabitModel::class, HabitInstanceModel::class, WorkspaceModel::class, TodoModel::class, JournalModel::class],
    version = 8,
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

        // Add temp column
        db.execSQL("""
            ALTER TABLE WorkspaceModel
            ADD COLUMN selectedSpaces_new TEXT NOT NULL DEFAULT '[]'
        """)

        val cursor = db.query(
            "SELECT workspaceId, selectedSpaces FROM WorkspaceModel"
        )

        while (cursor.moveToNext()) {
            val id = cursor.getString(0)
            val raw = cursor.getString(1)

            val spaces = Json.decodeFromString<List<Int>>(raw)
                .toMutableSet()

            // 1. Merge Calendar (4) → Events (3)
            if (spaces.remove(4)) {
                spaces.add(3)
            }

            // 2. Downgrade IDs above 4
            val normalized = spaces.map {
                if (it > 4) it - 1 else it
            }.toSet()

            val newJson = Json.encodeToString(normalized.toList())

            db.execSQL(
                "UPDATE WorkspaceModel SET selectedSpaces_new = ? WHERE workspaceId = ?",
                arrayOf(newJson, id)
            )
        }

        cursor.close()

        // Recreate table (SQLite cannot drop columns)
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
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add storageRootUri column with default value null
        db.execSQL("ALTER TABLE SettingsModel ADD COLUMN storageRootUri TEXT")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE SettingsModel
            ADD COLUMN startWithReadView INTEGER NOT NULL DEFAULT 0
            """.trimIndent()
        )

        db.execSQL(
            """
            ALTER TABLE SettingsModel
            ADD COLUMN isLineNumbersVisible INTEGER NOT NULL DEFAULT 0
            """.trimIndent()
        )

        db.execSQL(
            """
            ALTER TABLE SettingsModel
            ADD COLUMN isLintValid INTEGER NOT NULL DEFAULT 0
            """.trimIndent()
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Migration to add 'id' field to TodoItem objects within the items JSON array
        // Since TodoItem is stored as JSON in the 'items' column of TodoModel,
        // we need to parse, update, and re-save the JSON data

        val cursor = db.query("SELECT id, items FROM TodoModel")
        val gson = Gson()

        while (cursor.moveToNext()) {
            val todoId = cursor.getString(0)
            val itemsJson = cursor.getString(1)

            try {
                // Define a temporary class to represent old TodoItem without id
                data class OldTodoItem(
                    val value: String,
                    val isChecked: Boolean
                )

                // Define new TodoItem with id field
                data class NewTodoItem(
                    val id: String,
                    val value: String,
                    val isChecked: Boolean
                )

                // Parse old format
                val oldItemsType = object : TypeToken<List<OldTodoItem>>() {}.type
                val oldItems: List<OldTodoItem> = try {
                    gson.fromJson(itemsJson, oldItemsType)
                } catch (_: Exception) {
                    // If it already has the new format, skip
                    continue
                }

                // Convert to new format with generated IDs
                val newItems = oldItems.map { oldItem ->
                    NewTodoItem(
                        id = java.util.UUID.randomUUID().toString(),
                        value = oldItem.value,
                        isChecked = oldItem.isChecked
                    )
                }

                // Serialize back to JSON
                val newItemsJson = gson.toJson(newItems)

                // Update the database
                db.execSQL(
                    "UPDATE TodoModel SET items = ? WHERE id = ?",
                    arrayOf(newItemsJson, todoId)
                )
            } catch (e: Exception) {
                // If migration fails for this record, log or skip
                // The TypeConverter will handle it at runtime as fallback
                e.printStackTrace()
            }
        }

        cursor.close()
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // 1. Create new table without `images`
        db.execSQL("""
            CREATE TABLE journalmodel_new (
                journalId TEXT NOT NULL,
                workspaceId TEXT NOT NULL,
                text TEXT NOT NULL,
                dateTime INTEGER NOT NULL,
                PRIMARY KEY(journalId)
            )
        """.trimIndent())

        // 2. Copy data (ignore `images`)
        db.execSQL("""
            INSERT INTO journalmodel_new (journalId, workspaceId, text, dateTime)
            SELECT journalId, workspaceId, text, dateTime
            FROM JournalModel
        """.trimIndent())

        // 3. Drop old table
        db.execSQL("DROP TABLE JournalModel")

        // 4. Rename new table
        db.execSQL("ALTER TABLE journalmodel_new RENAME TO JournalModel")

        db.execSQL(
            """
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
            """.trimIndent()
        )

        // 2. Copy data (skip `images`)
        db.execSQL(
            """
            INSERT INTO NotesModel_new (
                notesId,
                workspaceId,
                title,
                description,
                isPinned,
                labels,
                lastEdited
            )
            SELECT
                notesId,
                workspaceId,
                title,
                description,
                isPinned,
                labels,
                lastEdited
            FROM NotesModel
            """.trimIndent()
        )

        // 3. Drop old table
        db.execSQL("DROP TABLE NotesModel")

        // 4. Rename new table
        db.execSQL("ALTER TABLE NotesModel_new RENAME TO NotesModel")
    }
}
