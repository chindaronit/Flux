package com.flux.di

import android.content.Context
import androidx.room.Room
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
import com.flux.data.database.FluxDatabase
import com.flux.data.database.MIGRATION_1_2
import com.flux.data.database.MIGRATION_2_3
import com.flux.data.database.MIGRATION_3_4
import com.flux.data.database.Migration_4_5
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Singleton
    @Provides
    fun provideFluxDatabase(
        @ApplicationContext app: Context
    ): FluxDatabase = Room.databaseBuilder(
        app,
        FluxDatabase::class.java,
        "FluxDatabase"
    )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, Migration_4_5)
        .build()

    @Singleton
    @Provides
    fun provideWorkspaceDao(db: FluxDatabase): WorkspaceDao = db.workspaceDao

    @Singleton
    @Provides
    fun provideSettingsDao(db: FluxDatabase): SettingsDao = db.settingsDao

    @Singleton
    @Provides
    fun provideHabitDao(db: FluxDatabase): HabitsDao = db.habitDao

    @Singleton
    @Provides
    fun provideHabitInstanceDao(db: FluxDatabase): HabitInstanceDao = db.habitInstanceDao

    @Singleton
    @Provides
    fun provideEventDao(db: FluxDatabase): EventDao = db.eventDao

    @Singleton
    @Provides
    fun provideEventInstanceDao(db: FluxDatabase): EventInstanceDao = db.eventInstanceDao

    @Singleton
    @Provides
    fun provideJournalDao(db: FluxDatabase): JournalDao = db.journalDao

    @Singleton
    @Provides
    fun provideNotesDao(db: FluxDatabase): NotesDao = db.notesDao

    @Singleton
    @Provides
    fun provideTodoDao(db: FluxDatabase): TodoDao = db.todoDao

    @Singleton
    @Provides
    fun provideLabelDao(db: FluxDatabase): LabelDao = db.labelDao
}
