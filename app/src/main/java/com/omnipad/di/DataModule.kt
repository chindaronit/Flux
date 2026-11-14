package com.omnipad.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.omnipad.data.dao.EventDao
import com.omnipad.data.dao.EventExceptionDao
import com.omnipad.data.dao.EventInstanceDao
import com.omnipad.data.dao.EventOccurrenceColorDao
import com.omnipad.data.dao.HabitInstanceDao
import com.omnipad.data.dao.HabitsDao
import com.omnipad.data.dao.JournalDao
import com.omnipad.data.dao.LabelDao
import com.omnipad.data.dao.NotesDao
import com.omnipad.data.dao.ProgressBoardDao
import com.omnipad.data.dao.SettingsDao
import com.omnipad.data.dao.TodoDao
import com.omnipad.data.dao.TodoInstanceDao
import com.omnipad.data.dao.WorkspaceDao
import com.omnipad.data.database.OmniPadDatabase
import com.omnipad.data.database.MIGRATION_10_11
import com.omnipad.data.database.MIGRATION_11_12
import com.omnipad.data.database.MIGRATION_12_13
import com.omnipad.data.database.MIGRATION_13_14
import com.omnipad.data.database.MIGRATION_14_15
import com.omnipad.data.database.MIGRATION_15_16
import com.omnipad.data.database.MIGRATION_16_17
import com.omnipad.data.database.MIGRATION_17_18
import com.omnipad.data.database.MIGRATION_18_19
import com.omnipad.data.database.MIGRATION_19_20
import com.omnipad.data.database.MIGRATION_20_21
import com.omnipad.data.database.MIGRATION_21_22
import com.omnipad.data.database.MIGRATION_1_2
import com.omnipad.data.database.MIGRATION_2_3
import com.omnipad.data.database.MIGRATION_3_4
import com.omnipad.data.database.MIGRATION_4_5
import com.omnipad.data.database.MIGRATION_5_6
import com.omnipad.data.database.MIGRATION_6_7
import com.omnipad.data.database.MIGRATION_7_8
import com.omnipad.data.database.MIGRATION_8_9
import com.omnipad.data.database.MIGRATION_9_10
import com.omnipad.other.BackupManager
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
    fun provideOmniPadDatabase(
        @ApplicationContext app: Context
    ): OmniPadDatabase = Room.databaseBuilder(
        app,
        OmniPadDatabase::class.java,
        "OmniPadDatabase"
    )
        .addMigrations(MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14,
            MIGRATION_14_15,
            MIGRATION_15_16,
            MIGRATION_16_17,
            MIGRATION_17_18,
            MIGRATION_18_19,
            MIGRATION_19_20,
            MIGRATION_20_21,
            MIGRATION_21_22
        )
        .build()

    @Singleton
    @Provides
    fun provideWorkspaceDao(db: OmniPadDatabase): WorkspaceDao = db.workspaceDao

    @Singleton
    @Provides
    fun provideSettingsDao(db: OmniPadDatabase): SettingsDao = db.settingsDao

    @Singleton
    @Provides
    fun provideHabitDao(db: OmniPadDatabase): HabitsDao = db.habitDao

    @Singleton
    @Provides
    fun provideHabitInstanceDao(db: OmniPadDatabase): HabitInstanceDao = db.habitInstanceDao

    @Singleton
    @Provides
    fun provideEventDao(db: OmniPadDatabase): EventDao = db.eventDao

    @Singleton
    @Provides
    fun provideEventInstanceDao(db: OmniPadDatabase): EventInstanceDao = db.eventInstanceDao

    @Singleton
    @Provides
    fun provideEventExceptionDao(db: OmniPadDatabase): EventExceptionDao = db.eventExceptionDao

    @Singleton
    @Provides
    fun provideEventOccurrenceColorDao(db: OmniPadDatabase): EventOccurrenceColorDao = db.eventOccurrenceColorDao

    @Singleton
    @Provides
    fun provideJournalDao(db: OmniPadDatabase): JournalDao = db.journalDao

    @Singleton
    @Provides
    fun provideNotesDao(db: OmniPadDatabase): NotesDao = db.notesDao

    @Singleton
    @Provides
    fun provideTodoDao(db: OmniPadDatabase): TodoDao = db.todoDao

    @Singleton
    @Provides
    fun provideTodoInstanceDao(db: OmniPadDatabase): TodoInstanceDao = db.todoInstanceDao

    @Singleton
    @Provides
    fun provideLabelDao(db: OmniPadDatabase): LabelDao = db.labelDao

    @Singleton
    @Provides
    fun provideProgressBoardDao(db: OmniPadDatabase): ProgressBoardDao = db.progressBoardDao

    @Singleton
    @Provides
    fun provideBackupManager(@ApplicationContext context: Context): BackupManager = BackupManager(WorkManager.getInstance(context))
}
