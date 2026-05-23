package com.flux.di

import com.flux.data.repository.EventRepository
import com.flux.data.repository.EventRepositoryImpl
import com.flux.data.repository.HabitRepository
import com.flux.data.repository.HabitRepositoryImpl
import com.flux.data.repository.JournalRepository
import com.flux.data.repository.JournalRepositoryImpl
import com.flux.data.repository.LabelRepository
import com.flux.data.repository.LabelRepositoryImpl
import com.flux.data.repository.NoteRepository
import com.flux.data.repository.NoteRepositoryImpl
import com.flux.data.repository.ProgressBoardRepository
import com.flux.data.repository.ProgressBoardRepositoryImpl
import com.flux.data.repository.SettingsRepository
import com.flux.data.repository.SettingsRepositoryImpl
import com.flux.data.repository.TodoRepository
import com.flux.data.repository.TodoRepositoryImpl
import com.flux.data.repository.WorkspaceRepository
import com.flux.data.repository.WorkspaceRepositoryImpl
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