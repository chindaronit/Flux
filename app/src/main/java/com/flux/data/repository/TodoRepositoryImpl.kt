package com.flux.data.repository

import com.flux.data.dao.TodoDao
import com.flux.data.dao.TodoInstanceDao
import com.flux.data.dao.WorkspaceDao
import com.flux.data.model.RecurrenceRule
import com.flux.data.model.TodoDisplayItem
import com.flux.data.model.TodoInstance
import com.flux.data.model.TodoModel
import com.flux.data.model.isActiveOn
import com.flux.data.model.startDateAsLocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class TodoRepositoryImpl @Inject constructor(
    val dao: TodoDao,
    val instanceDao: TodoInstanceDao,
    val workspaceDao: WorkspaceDao
) : TodoRepository {
    override fun loadTodoData(): Flow<List<TodoModel>> {
        return dao.loadTodoData()
    }

    override suspend fun upsertList(list: TodoModel) {
        return withContext(Dispatchers.IO) { dao.upsertList(list) }
    }

    override suspend fun deleteList(list: TodoModel) {
        return withContext(Dispatchers.IO) {
            instanceDao.deleteListInstances(list.id)
            dao.deleteList(list)
        }
    }

    override suspend fun deleteAllWorkspaceLists(workspaceId: String) {
        return withContext(Dispatchers.IO) { dao.deleteAllWorkspaceLists(workspaceId) }
    }

    override suspend fun upsertInstance(todoInstance: TodoInstance) {
        return withContext(Dispatchers.IO) { instanceDao.upsertTodoInstance(todoInstance) }
    }

    override suspend fun deleteAllWorkspaceInstance(workspaceId: String) {
        return withContext(Dispatchers.IO) { instanceDao.deleteAllWorkspaceInstance(workspaceId) }
    }

    override suspend fun existInstance(listId: String, instanceDate: Long): Boolean {
        return withContext(Dispatchers.IO) { instanceDao.exists(listId, instanceDate) }
    }

    override suspend fun loadAllLists(): List<TodoModel> {
        return withContext(Dispatchers.IO) { dao.loadAllLists() }
    }

    override fun loadAllTodoInstance(): Flow<List<TodoInstance>> {
        return instanceDao.loadAll()
    }

    override fun observeTodoList(todoId: String): Flow<TodoDisplayItem?> {
        val today = LocalDate.now()
        val todayEpoch = today.toEpochDay()
        val zone = ZoneId.systemDefault()

        return combine(
            dao.observeTodoList(todoId),
            instanceDao.observeInstanceForDate(todoId, todayEpoch)
        ) { todo, instances ->

            if (todo == null)
                return@combine null

            when {
                todo.recurrence == RecurrenceRule.NONE -> TodoDisplayItem.Static(todo)

                todo.recurrence.isActiveOn(
                    today,
                    todo.startDateAsLocalDate(zone)
                ) -> {

                    val instance =
                        instances.firstOrNull()
                            ?: TodoInstance(
                                todoId = todo.id,
                                workspaceId = todo.workspaceId,
                                instanceDate = todayEpoch,
                                items = todo.items
                            )

                    TodoDisplayItem.Recurring(
                        todo = todo,
                        instance = instance
                    )
                }

                else -> null
            }
        }.flowOn(Dispatchers.Default)
    }

    override suspend fun toggleTodoItem(
        item: TodoDisplayItem,
        itemId: String
    ) = withContext(Dispatchers.IO) {

        when (item) {
            is TodoDisplayItem.Static -> {
                val updated =
                    item.todo.items.map {
                        if (it.id == itemId) it.copy(isChecked = !it.isChecked)
                        else it
                    }

                dao.upsertList(item.todo.copy(items = updated))
            }

            is TodoDisplayItem.Recurring -> {
                val instance = item.instance

                val updated =
                    instance.items.map {
                        if (it.id == itemId) it.copy(isChecked = !it.isChecked)
                        else it
                    }

                instanceDao.upsertTodoInstance(instance.copy(items = updated))
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observePublicTodos(): Flow<List<TodoModel>> =
        workspaceDao.observePublicWorkspaceIds()
            .flatMapLatest { workspaceIds ->
                if (workspaceIds.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    dao.observePublicTodos(workspaceIds)
                }
            }
}