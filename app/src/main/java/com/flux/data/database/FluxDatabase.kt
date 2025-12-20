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
import kotlinx.serialization.json.Json

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
