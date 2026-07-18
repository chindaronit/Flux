package com.flux.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.flux.data.model.TodoInstance
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoInstanceDao {
    @Query("SELECT EXISTS(SELECT 1 FROM TodoInstance WHERE todoId = :todoId and instanceDate = :instanceDate)")
    suspend fun exists(todoId: String, instanceDate: Long): Boolean

    @Query("SELECT * FROM TodoInstance")
    fun loadAll(): Flow<List<TodoInstance>>

    @Query("SELECT * FROM TodoInstance")
    fun loadAllInstances(): List<TodoInstance>

    @Query("SELECT * FROM TodoInstance WHERE todoId = :todoId AND instanceDate = :date LIMIT 1")
    suspend fun loadInstanceForDate(todoId: String, date: Long): TodoInstance?

    @Query("DELETE FROM TodoInstance WHERE workspaceId = :workspaceId")
    suspend fun deleteAllWorkspaceInstance(workspaceId: String)

    @Query("SELECT * FROM TodoInstance WHERE instanceDate = :date and todoId = :todoId")
    fun observeInstanceForDate(todoId: String, date: Long): Flow<List<TodoInstance>>

    @Upsert
    suspend fun upsertTodoInstance(instance: TodoInstance)

    @Query("DELETE FROM TodoInstance WHERE todoId = :listId")
    suspend fun deleteListInstances(listId: String)
}