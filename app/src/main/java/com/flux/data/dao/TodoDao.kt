package com.flux.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flux.data.model.TodoModel
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT EXISTS(SELECT 1 FROM TodoModel WHERE  id = :id)")
    suspend fun exists(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertList(list: TodoModel)

    @Delete
    suspend fun deleteList(list: TodoModel)

    @Query("SELECT * FROM TodoModel WHERE workspaceId IN (:workspaceIds)")
    fun observePublicTodos(workspaceIds: List<String>): Flow<List<TodoModel>>

    @Query("SELECT * FROM TodoModel WHERE id = (:todoId)")
    fun observeTodoList(todoId: String): Flow<TodoModel?>

    @Query("DELETE FROM TodoModel WHERE workspaceId = :workspaceId")
    fun deleteAllWorkspaceLists(workspaceId: String)

    @Query("SELECT * FROM TodoModel")
    fun loadTodoData(): Flow<List<TodoModel>>

    @Query("SELECT * FROM TodoModel")
    suspend fun loadAllLists(): List<TodoModel>
}
