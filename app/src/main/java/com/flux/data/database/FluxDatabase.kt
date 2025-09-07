package com.flux.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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

@Database(
    entities = [EventModel::class, LabelModel::class, EventInstanceModel::class, SettingsModel::class, NotesModel::class, HabitModel::class, HabitInstanceModel::class, WorkspaceModel::class, TodoModel::class, JournalModel::class],
    version = 1,
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
