package com.flux.ui.screens.todo

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.RecurrenceRule
import com.flux.data.model.TodoItem
import com.flux.data.model.TodoModel
import com.flux.ui.common.DeleteAlert
import com.flux.ui.events.TodoEvents
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTodoList(
    navController: NavController,
    is24HoursFormat: Boolean,
    list: TodoModel,
    workspaceId: String,
    onTodoEvents: (TodoEvents) -> Unit
) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf(list.title) }
    val itemList = remember { list.items.toMutableStateList() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var recurrence by remember { mutableStateOf(list.recurrence) }
    var reminderTime by remember { mutableLongStateOf(list.startDateTime) }
    val deleteQueue = remember {
        Channel<Pair<Int, TodoItem>>(Channel.UNLIMITED)
    }
    // True = normal editing mode (locked ordering)
    // False = reorder mode (unlocked ordering, drag handles visible)
    var isReordering by remember { mutableStateOf(false) }

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

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState
    ) { from, to -> itemList.move(from.index, to.index) }

    var isReminderDialogVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val itemRemovedLabel = stringResource(R.string.item_removed)
    val undoLabel = stringResource(R.string.undo)

    LaunchedEffect(Unit) {
        for ((index, removedItem) in deleteQueue) {

            val result = snackbarHostState.showSnackbar(
                message = itemRemovedLabel,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short
            )

            if (result == SnackbarResult.ActionPerformed) {
                val insertIndex = index.coerceAtMost(itemList.size)
                itemList.add(insertIndex, removedItem)
            }
        }
    }

    fun saveTodoIfPossible(): Boolean {
        val hasContent = title.isNotBlank() || itemList.isNotEmpty()

        if (!hasContent) { return true }

        if (title.isBlank()) {
            Toast.makeText(context, "Title is Required", Toast.LENGTH_SHORT).show()
            return false
        }

        onTodoEvents(
            TodoEvents.UpsertList(
                context,
                list.recurrence is RecurrenceRule.Weekly && recurrence is RecurrenceRule.NONE,
                list.copy(
                    title = title,
                    items = itemList.toList(),
                    workspaceId = workspaceId,
                    recurrence = recurrence,
                    startDateTime = reminderTime
                )
            )
        )

        return true
    }

    BackHandler {
        if (saveTodoIfPossible()) {
            isReordering = false
            navController.popBackStack()
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(MaterialTheme.colorScheme.surfaceContainerLow),
                title = {
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        readOnly = isReordering,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            Box {
                                if (title.isBlank()) {
                                    Text(
                                        text = stringResource(R.string.Title),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton({
                        if (saveTodoIfPossible()) {
                            isReordering = false
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                    }
                },
                actions = {
                    // Reminder
                    IconButton(onClick = { isReminderDialogVisible = true }) {
                        Icon(Icons.Default.Alarm, null)
                    }

                    // Lock/unlock reorder button
                    IconButton(onClick = { isReordering = !isReordering }) {
                        Icon(if(!isReordering) Icons.Default.Lock else Icons.Default.LockOpen, null)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.padding(innerPadding)
        ) {
            itemsIndexed(
                itemList,
                key = { _, item -> item.id }
            ) { _, item ->

                ReorderableItem(
                    state = reorderableState,
                    key = item.id
                ) { _ ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .animateItem(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {

                        // Checkbox: only in editing (locked) mode
                        if (!isReordering) {
                            Checkbox(
                                modifier = Modifier
                                    .scale(0.75f)
                                    .size(32.dp),
                                checked = item.isChecked,
                                onCheckedChange = { checked ->
                                    val i = itemList.indexOfFirst { it.id == item.id }
                                    if (i >= 0) {
                                        itemList[i] = item.copy(isChecked = checked)
                                    }
                                }
                            )
                        }

                        BasicTextField(
                            value = item.value,
                            onValueChange = { newText ->
                                val i = itemList.indexOfFirst { it.id == item.id }
                                if (i >= 0) {
                                    itemList[i] = item.copy(value = newText)
                                }
                            },
                            // Editable only in editing mode, read-only during reorder or view
                            readOnly = isReordering,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (item.value.isBlank()) {
                                        Text(
                                            text = stringResource(R.string.Title),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        // Drag handle: only in reorder (unlocked) mode
                        if (isReordering) {
                            IconButton(
                                onClick = {},
                                modifier = Modifier
                                    .size(32.dp)
                                    .draggableHandle()
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = null)
                            }
                        }

                        // Remove button: only in editing (locked) mode
                        if (!isReordering) {
                            IconButton(
                                onClick = {
                                    val index = itemList.indexOfFirst { it.id == item.id }

                                    if (index >= 0) {
                                        val removedItem = itemList[index]
                                        itemList.removeAt(index)
                                        scope.launch { deleteQueue.send(index to removedItem) }
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Add item button: only in editing (locked) mode
            if (!isReordering) {
                item {
                    TextButton(
                        onClick = { itemList.add(TodoItem()) },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SubdirectoryArrowRight, null)
                            Text(stringResource(R.string.Add_Item))
                        }
                    }
                }
            }
        }
    }

    if (isReminderDialogVisible) {
        TodoReminderDialog(
            is24HourFormat = is24HoursFormat,
            reminderTime = reminderTime,
            recurrence = recurrence,
            onDismiss = { isReminderDialogVisible = false }
        ) { newRecurrence, newReminderTime ->
            recurrence=newRecurrence
            reminderTime = newReminderTime
        }
    }
}

fun <T> MutableList<T>.move(from: Int, to: Int) {
    if (from == to) return
    if (from !in indices) return

    val adjustedTo = when {
        to < 0 -> 0
        to > lastIndex -> lastIndex
        else -> to
    }

    val item = removeAt(from)
    add(adjustedTo, item)
}