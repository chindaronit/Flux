package com.flux.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("DELETE FROM TodoInstance WHERE workspaceId = :workspaceId")
    suspend fun deleteAllWorkspaceInstance(workspaceId: String)

    @Query("SELECT * FROM TodoInstance WHERE instanceDate = :date and todoId = :todoId")
    fun observeInstanceForDate(todoId: String, date: Long): Flow<List<TodoInstance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTodoInstance(instance: TodoInstance)

    @Query("DELETE FROM TodoInstance WHERE todoId = :listId")
    suspend fun deleteListInstances(listId: String)
}