package com.flux.ui.screens.todo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.TodoItem
import com.flux.data.model.TodoModel
import com.flux.navigation.Loader
import com.flux.navigation.NavRoutes
import com.flux.ui.components.DeleteAlert
import com.flux.ui.components.shapeManager
import com.flux.ui.events.TodoEvents

@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.todoHomeItems(
    navController: NavController,
    radius: Int,
    allList: List<TodoModel>,
    workspaceId: String,
    isLoading: Boolean,
    expandedTODOIds: Set<String>,
    onExpandToggle: (String) -> Unit,
    onTodoEvents: (TodoEvents) -> Unit
) {
    when {
        isLoading -> item { Loader() }
        allList.isEmpty() -> item { EmptyTodoList() }
        else -> {
            items(allList, key = {it.id}) { todoItem->
                TodoExpandableCard(
                    navController = navController,
                    radius = radius,
                    item = todoItem,
                    workspaceId = workspaceId,
                    isExpanded = todoItem.id in expandedTODOIds,
                    onExpandToggle = onExpandToggle,
                    onTodoEvents = onTodoEvents
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoExpandableCard(
    navController: NavController,
    radius: Int,
    item: TodoModel,
    isExpanded: Boolean,
    workspaceId: String,
    onExpandToggle: (String) -> Unit,
    onTodoEvents: (TodoEvents) -> Unit
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<TodoModel?>(null) }

    if (showDeleteDialog && selectedItem != null) {
        DeleteAlert(
            onConfirmation = {
                onTodoEvents(TodoEvents.DeleteList(selectedItem!!))
                selectedItem = null
                showDeleteDialog = false
            },
            onDismissRequest = { showDeleteDialog = false }
        )
    }

    Card(
        modifier = Modifier.padding(top = 4.dp),
        shape = if(isExpanded) shapeManager(isBoth = true, radius=radius) else RoundedCornerShape(50),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column {
            TodoHeaderRow(
                id = item.id,
                title = item.title,
                onExpandToggle = onExpandToggle,
                onNavigate = {
                    navController.navigate(
                        NavRoutes.TodoDetail.withArgs(workspaceId, item.id)
                    )
                }
            )

            if (isExpanded) {
                TodoItems(
                    todoList = item,
                    workspaceId = workspaceId,
                    onTodoEvents = onTodoEvents
                )
            }
        }
    }
}

@Composable
private fun TodoHeaderRow(
    id: String,
    title: String,
    onExpandToggle: (String) -> Unit,
    onNavigate: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .fillMaxWidth()
            .clickable { onExpandToggle(id) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 20.dp, end = 3.dp),
        )
        IconButton(onClick = onNavigate) {
            Icon(Icons.Default.Edit, null)
        }
    }
}

@Composable
private fun TodoItems(
    todoList: TodoModel,
    workspaceId: String,
    onTodoEvents: (TodoEvents) -> Unit
) {
    fun onToggleCheck (todoItem: TodoItem) {
        val updatedItems = todoList.items.map {
            if (it.id == todoItem.id)
                it.copy(isChecked = !it.isChecked)
            else it
        }

        if (updatedItems != todoList.items) {
            onTodoEvents(
                TodoEvents.UpsertList(
                    todoList.copy(
                        items = updatedItems,
                        workspaceId = workspaceId
                    )
                )
            )
        }
    }
    val checkedItems = todoList.items.filter { it.isChecked }
    val unCheckedItems = todoList.items.filter { !it.isChecked }
    val allSortedItems = unCheckedItems + checkedItems
    LazyColumn(
        Modifier.padding(horizontal = 6.dp).padding(top = 4.dp, bottom = 12.dp).heightIn(max = 400.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(allSortedItems) { todoItem ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = CardDefaults.cardColors(
                    containerColor = if (todoItem.isChecked) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerHighest
                ),
                onClick = { onToggleCheck(todoItem) }
            ) {
                Row (verticalAlignment = Alignment.CenterVertically) {
                    IconButton({onToggleCheck(todoItem)}, colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if(todoItem.isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = if(todoItem.isChecked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )) { Icon(if(todoItem.isChecked) Icons.Default.Verified else Icons.Outlined.Circle, null) }
                    Text(
                        text = todoItem.value,
                        style = MaterialTheme.typography.labelLarge,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyTodoList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Checklist,
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
        Text(stringResource(R.string.Empty_Lists))
    }
}
