package com.flux.ui.screens.todo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.flux.data.model.TodoModel
import com.flux.ui.common.DeleteAlert
import com.flux.ui.events.TodoEvents
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.flux.data.model.RecurrenceRule
import com.flux.data.model.TodoInstance
import com.flux.navigation.NavRoutes
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoDetail(
    navController: NavController,
    radius: Int,
    list: TodoModel,
    instances: List<TodoInstance>,
    workspaceId: String,
    onTodoEvents: (TodoEvents) -> Unit
) {
    val todayEpoch = LocalDate.now().toEpochDay()
    val context = LocalContext.current
    val isReminderOn = list.recurrence is RecurrenceRule.Weekly
    var showDeleteDialog by remember { mutableStateOf(false) }
    val todayInstance = instances.find { it.instanceDate == todayEpoch }

    if (showDeleteDialog) {
        DeleteAlert(
            { showDeleteDialog = false },
            {
                onTodoEvents(TodoEvents.DeleteList(context,list))
                navController.popBackStack()
                showDeleteDialog = false
            }
        )
    }

    fun isAllowed(epochDay: Long): Boolean {
        val localDate = LocalDate.ofEpochDay(epochDay)
        // Convert to Monday=0, Tuesday=1, ..., Sunday=6 format
        val dayOfWeek = (localDate.dayOfWeek.value + 6) % 7
        return dayOfWeek in (list.recurrence as RecurrenceRule.Weekly).daysOfWeek
    }

    LaunchedEffect(list.id, list.recurrence, todayInstance) {
        if(isReminderOn && isAllowed(todayEpoch) && todayInstance == null){
            onTodoEvents(TodoEvents.CreateInstance(list.id, workspaceId))
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(MaterialTheme.colorScheme.surfaceContainerLow),
                title = { },
                navigationIcon = {
                    IconButton({ navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton({ navController.navigate(NavRoutes.NewTodoList.withArgs(workspaceId, list.id)) }) {
                        Icon(Icons.Default.Edit, null)
                    }
                    IconButton({ showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            Modifier.padding(innerPadding).padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if(!isReminderOn){
                item{ TodoDetailedInfo(radius, list) }

                items(list.items) { todoItem ->
                    MaterialListItem(true, todoItem){
                        val updatedItems = list.items.map {
                            if (it.id == todoItem.id) it.copy(isChecked = !it.isChecked)
                            else it
                        }

                        if (updatedItems != list.items) {
                            onTodoEvents(
                                TodoEvents.UpsertList(
                                    context,
                                    false,
                                    list.copy(
                                        items = updatedItems,
                                        workspaceId = workspaceId
                                    )
                                )
                            )
                        }
                    }
                }
            }

            else if(isAllowed(todayEpoch) && todayInstance!=null) {
                item{ TodoDetailedInfo(radius, list, isReminderOn = true, isAllowedToday = true, todayInstance) }
                item{ TodoHeatMap(radius, list, instances) }

                items(todayInstance.items) { todoItem ->
                    MaterialListItem(true, todoItem){
                        val updatedItems = todayInstance.items.map {
                            if (it.id == todoItem.id) it.copy(isChecked = !it.isChecked)
                            else it
                        }

                        if (updatedItems != todayInstance.items) {
                            onTodoEvents(TodoEvents.UpsertInstance(todayInstance.copy(items = updatedItems)))
                        }
                    }
                }
            }

            else if(!isAllowed(todayEpoch)){
                item{
                    TodoDetailedInfo(radius, list,
                    isReminderOn = true,
                    isAllowedToday = false,
                    todayInstance = todayInstance)
                }

                item{ TodoHeatMap(radius, list, instances) }

                items(list.items) { todoItem ->
                    MaterialListItem(false, todoItem){}
                }
            }
        }
    }
}
