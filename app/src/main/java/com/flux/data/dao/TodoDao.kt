package com.flux.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.flux.data.model.TodoModel
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT EXISTS(SELECT 1 FROM TodoModel WHERE  id = :id)")
    suspend fun exists(id: String): Boolean

    @Upsert
    suspend fun upsertList(list: TodoModel)

    @Query("SELECT * FROM TodoModel WHERE id = :todoId")
    suspend fun getTodoById(todoId: String): TodoModel?

    @Query("UPDATE TodoModel SET `order` = :order WHERE id = :id")
    suspend fun updateOrder(id: String, order: Int)

    @Query("SELECT COALESCE(MAX(`order`), -1) FROM HabitModel WHERE workspaceId = :workspaceId")
    suspend fun getMaxOrder(workspaceId: String): Int

    @Delete
    suspend fun deleteList(list: TodoModel)

    @Query("SELECT * FROM TodoModel WHERE workspaceId IN (:workspaceIds)")
    fun observePublicTodos(workspaceIds: List<String>): Flow<List<TodoModel>>

    @Query("SELECT * FROM TodoModel WHERE id = (:todoId)")
    fun observeTodoList(todoId: String): Flow<TodoModel?>

    @Query("DELETE FROM TodoModel WHERE workspaceId = :workspaceId")
    fun deleteAllWorkspaceLists(workspaceId: String)

    @Query("SELECT * FROM TodoModel ORDER BY `order` ASC")
    fun loadTodoData(): Flow<List<TodoModel>>

    @Query("SELECT * FROM TodoModel ORDER BY `order` ASC")
    suspend fun loadAllLists(): List<TodoModel>
}
