package com.omnipad.di

import com.omnipad.data.repository.EventRepository
import com.omnipad.data.repository.EventRepositoryImpl
import com.omnipad.data.repository.HabitRepository
import com.omnipad.data.repository.HabitRepositoryImpl
import com.omnipad.data.repository.JournalRepository
import com.omnipad.data.repository.JournalRepositoryImpl
import com.omnipad.data.repository.LabelRepository
import com.omnipad.data.repository.LabelRepositoryImpl
import com.omnipad.data.repository.NoteRepository
import com.omnipad.data.repository.NoteRepositoryImpl
import com.omnipad.data.repository.ProgressBoardRepository
import com.omnipad.data.repository.ProgressBoardRepositoryImpl
import com.omnipad.data.repository.SettingsRepository
import com.omnipad.data.repository.SettingsRepositoryImpl
import com.omnipad.data.repository.TodoRepository
import com.omnipad.data.repository.TodoRepositoryImpl
import com.omnipad.data.repository.WorkspaceRepository
import com.omnipad.data.repository.WorkspaceRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNoteRepository(
        impl: NoteRepositoryImpl
    ): NoteRepository

    @Binds
    @Singleton
    abstract fun bindWorkspaceRepository(
        impl: WorkspaceRepositoryImpl
    ): WorkspaceRepository

    @Binds
    @Singleton
    abstract fun bindEventRepository(
        impl: EventRepositoryImpl
    ): EventRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindHabitsRepository(
        impl: HabitRepositoryImpl
    ): HabitRepository

    @Binds
    @Singleton
    abstract fun bindTodoRepository(
        impl: TodoRepositoryImpl
    ): TodoRepository

    @Binds
    @Singleton
    abstract fun bindJournalRepository(
        impl: JournalRepositoryImpl
    ): JournalRepository

    @Binds
    @Singleton
    abstract fun bindProgressBoardRepository(
        impl: ProgressBoardRepositoryImpl
    ): ProgressBoardRepository

    @Binds
    @Singleton
    abstract fun bindLabelRepository(
        impl: LabelRepositoryImpl
    ): LabelRepository
}