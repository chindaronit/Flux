package com.omnipad.ui.screens.todo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.omnipad.R
import com.omnipad.data.model.RecurrenceRule
import com.omnipad.data.model.WorkspaceModel
import com.omnipad.navigation.Loader
import com.omnipad.navigation.NavRoutes
import com.omnipad.ui.common.EmptyData
import com.omnipad.ui.common.SpaceSearchBar
import com.omnipad.ui.common.SpaceTopBar
import com.omnipad.ui.common.SpacesMenu
import com.omnipad.ui.events.TodoEvents
import com.omnipad.ui.screens.workspaces.SpacesToolBar
import com.omnipad.ui.state.Settings
import com.omnipad.ui.state.TodoState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    navController: NavController,
    state: TodoState,
    settings: Settings,
    workspace: WorkspaceModel,
    onShowSpaceBottomSheet: () -> Unit,
    onSpaceChange: (Int) -> Unit,
    onAddCover: () -> Unit,
    onRemoveCover: () -> Unit,
    onDeleteWorkspace: () -> Unit,
    onToggleLock: () -> Unit,
    onEvent: (TodoEvents) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    showSearchBar: Boolean,
    onSearchBarVisibilityChange: (Boolean) -> Unit,
    expandedTODOIds: MutableState<Set<String>>
){
    val context = LocalContext.current
    val workspaceId = workspace.workspaceId
    val isLoading = state.isLoading
    val radius = settings.data.cornerRadius
    var showSpacesMenu by remember { mutableStateOf(false) }
    val allList = state.allLists.filter {
        it.workspaceId == workspaceId && (
            it.title.contains(query, ignoreCase = true) ||
            it.items.any { item -> item.value.contains(query, ignoreCase = true) }
        )
    }
    var reorderEnabled by rememberSaveable { mutableStateOf(false) }
    val todoList = remember(allList) { allList.toMutableStateList() }
    val lazyListState = rememberLazyListState()

    val headerOffset = 1
    val reorderState = rememberReorderableLazyListState(lazyListState = lazyListState) { from, to ->
        if (!reorderEnabled) return@rememberReorderableLazyListState

        val fromIndex = from.index - headerOffset
        val toIndex = to.index - headerOffset

        if (fromIndex in todoList.indices && toIndex in todoList.indices) {
            todoList.move(fromIndex, toIndex)
            onEvent(TodoEvents.UpdateTodoOrder(todoList.map { it.id }))
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            SpaceTopBar(
                title            = workspace.title,
                description      = workspace.description,
                cover            = workspace.cover,
                icon             = workspace.icon,
                isLocked         = workspace.isLocked,
                onBackPressed    = { navController.popBackStack() },
                onAddCover       = onAddCover,
                onRemoveCover = onRemoveCover,
                onToggleLock = onToggleLock,
                onDeleteWorkspace = onDeleteWorkspace,
                onEditWorkspace = { navController.navigate(NavRoutes.NewWorkspace.withArgs(workspaceId)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton({
                val newId = UUID.randomUUID().toString()
                navController.navigate(NavRoutes.NewTodoList.withArgs(workspaceId, newId))
            }) {
                Icon(Icons.Default.AddTask, null)
            }
        }
    ) { innerPadding ->
        when {
            isLoading -> Loader()
            else -> Column(
                Modifier.fillMaxSize().padding(innerPadding).padding(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SpacesToolBar(
                        stringResource(R.string.To_Do),
                        Icons.Default.TaskAlt,
                        false,
                        onMainClick = { showSpacesMenu = true },
                        onEditClick = onShowSpaceBottomSheet
                    )
                    SpacesMenu(showSpacesMenu, workspace, onSpaceChange) { showSpacesMenu = false }
                    IconButton(onClick = { reorderEnabled = !reorderEnabled }) {
                        Icon(
                            imageVector = if (reorderEnabled) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                SpaceSearchBar(
                    query,
                    onQueryChange,
                    { expandedTODOIds.value = emptySet() },
                    modifier = Modifier.padding(bottom = 12.dp),
                    horizontalPadding = 0.dp
                )
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.weight(1f)
                ) {
                    if(allList.isEmpty()) item { EmptyData() }
                    itemsIndexed(
                        todoList,
                        key = { _, item -> item.id }
                    ) { _, todoItem ->
                        ReorderableItem (
                            state = reorderState,
                            key = todoItem.id
                        ) {
                            val cardDragModifier = if (reorderEnabled) {
                                Modifier.longPressDraggableHandle()
                            } else {
                                Modifier
                            }
                            val iconDragModifier = if (reorderEnabled) {
                                Modifier.draggableHandle()
                            } else {
                                Modifier
                            }

                            TodoExpandableCard(
                                modifier = Modifier.animateItem().then(cardDragModifier),
                                dragHandleModifier = iconDragModifier,
                                navController = navController,
                                radius = radius,
                                item = todoItem,
                                context = context,
                                workspaceId = workspaceId,
                                isReordering = reorderEnabled,
                                isExpanded = todoItem.id in expandedTODOIds.value,
                                onExpandToggle = { id->
                                    if(todoItem.recurrence is RecurrenceRule.NONE){
                                        expandedTODOIds.value =
                                            if (id in expandedTODOIds.value) expandedTODOIds.value - id
                                            else expandedTODOIds.value + id
                                    }
                                    else{
                                        navController.navigate(NavRoutes.TodoDetail.withArgs(workspaceId, id))
                                    }
                                },
                                onTodoEvents = onEvent,
                                onLongPressed = {}
                            )
                        }
                    }
                    item {
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}