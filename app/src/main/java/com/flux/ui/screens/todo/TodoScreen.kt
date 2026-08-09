package com.flux.ui.screens.todo

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.RecurrenceRule
import com.flux.data.model.WorkspaceModel
import com.flux.navigation.Loader
import com.flux.navigation.NavRoutes
import com.flux.ui.common.EmptyData
import com.flux.ui.common.SpaceSearchBar
import com.flux.ui.common.SpaceTopBar
import com.flux.ui.common.SpacesMenu
import com.flux.ui.events.TodoEvents
import com.flux.ui.screens.workspaces.SpacesToolBar
import com.flux.ui.state.Settings
import com.flux.ui.state.TodoState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

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
    onEvent: (TodoEvents) -> Unit
){
    val context = LocalContext.current
    val workspaceId = workspace.workspaceId
    val isLoading = state.isLoading
    val radius = settings.data.cornerRadius
    var showSpacesMenu by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
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

    var showSearchBar by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val expandedTODOIds = rememberSaveable(workspaceId) { mutableStateOf<Set<String>>(emptySet()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            SpaceTopBar(
                scrollBehavior = scrollBehavior,
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
            FloatingActionButton({ navController.navigate(NavRoutes.NewTodoList.withArgs(workspaceId, "")) }) {
                Icon(Icons.Default.AddTask, null)
            }
        }
    ) { innerPadding ->
        when {
            isLoading -> Loader()
            else -> {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize()
                        .padding(innerPadding)
                        .padding(12.dp)
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                ) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(bottom=8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if(showSearchBar){
                                SpaceSearchBar(
                                    query,
                                    { query=it },
                                    {
                                        showSearchBar=false
                                        expandedTODOIds.value=emptySet()
                                    }
                                )
                            }
                            else { 
                                SpacesToolBar(
                                    stringResource(R.string.To_Do),
                                    Icons.Default.TaskAlt,
                                    false,
                                    onMainClick = { showSpacesMenu = true },
                                    onEditClick = onShowSpaceBottomSheet
                                )
                                SpacesMenu(
                                    showSpacesMenu,
                                    workspace,
                                    onSpaceChange
                                ) { showSpacesMenu = false }

                                Row {
                                    IconButton({
                                        expandedTODOIds.value = emptySet()
                                        expandedTODOIds.value = allList.map { it.id }.toSet()
                                        showSearchBar = true
                                    }) {
                                        Icon(
                                            Icons.Default.Search,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    IconButton(onClick = { reorderEnabled = !reorderEnabled }) {
                                        Icon(
                                            imageVector =
                                                if (reorderEnabled) Icons.Default.LockOpen
                                                else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                            }
                        }
                    }

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
                                onTodoEvents = onEvent
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