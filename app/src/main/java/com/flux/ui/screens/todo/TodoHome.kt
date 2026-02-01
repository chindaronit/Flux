package com.flux.ui.screens.todo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.flux.R
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
    onTodoEvents: (TodoEvents) -> Unit
) {
    when {
        isLoading -> item { Loader() }
        allList.isEmpty() -> item { EmptyTodoList() }
        else -> {
            item {
                TodoExpandableCard(
                    navController = navController,
                    radius = radius,
                    allList = allList,
                    workspaceId = workspaceId,
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
    allList: List<TodoModel>,
    workspaceId: String,
    onTodoEvents: (TodoEvents) -> Unit
) {
    var expandedIds by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<TodoModel?>(null) }

    if (showDeleteDialog && selectedItem != null) {
        DeleteAlert(
            onConfirmation = {
                expandedIds = expandedIds - selectedItem?.id.orEmpty()
                onTodoEvents(TodoEvents.DeleteList(selectedItem!!))
                selectedItem = null
                showDeleteDialog = false
            },
            onDismissRequest = { showDeleteDialog = false }
        )
    }

    ElevatedCard(
        modifier = Modifier.padding(top = 8.dp),
        shape = shapeManager(radius = radius * 2),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        )
    ) {
        Column {
            allList.forEach { item ->
                val isExpanded = item.id in expandedIds

                TodoHeaderRow(
                    item = item,
                    isExpanded = isExpanded,
                    onExpandToggle = {
                        expandedIds = if (isExpanded) {
                            expandedIds - item.id
                        } else {
                            expandedIds + item.id
                        }
                    },
                    onDelete = {
                        selectedItem = item
                        showDeleteDialog = true
                    },
                    onNavigate = {
                        navController.navigate(
                            NavRoutes.TodoDetail.withArgs(workspaceId, item.id)
                        )
                    }
                )

                if (isExpanded) {
                    TodoItems(
                        item = item,
                        workspaceId = workspaceId,
                        onTodoEvents = onTodoEvents
                    )
                }

                HorizontalDivider(Modifier.alpha(0.5f))
            }
        }
    }
}

@Composable
private fun TodoHeaderRow(
    item: TodoModel,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onDelete: () -> Unit,
    onNavigate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onExpandToggle) {
                Icon(
                    if (isExpanded)
                        Icons.Default.KeyboardArrowDown
                    else
                        Icons.AutoMirrored.Default.KeyboardArrowRight,
                    contentDescription = null
                )
            }

            Text(
                text = item.title,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Remove,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun TodoItems(
    item: TodoModel,
    workspaceId: String,
    onTodoEvents: (TodoEvents) -> Unit
) {
    Column {
        item.items.forEach { checkItem ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checkItem.isChecked,
                    onCheckedChange = { checked ->
                        val updatedItems = item.items.map {
                            if (it.id == checkItem.id)
                                it.copy(isChecked = checked)
                            else it
                        }

                        if (updatedItems != item.items) {
                            onTodoEvents(
                                TodoEvents.UpsertList(
                                    item.copy(
                                        items = updatedItems,
                                        workspaceId = workspaceId
                                    )
                                )
                            )
                        }
                    }
                )

                Text(
                    text = checkItem.value,
                    style = MaterialTheme.typography.labelLarge
                )
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
