package com.flux.ui.screens.todo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.TodoItem
import com.flux.data.model.TodoModel
import com.flux.ui.components.DeleteAlert
import com.flux.ui.events.TodoEvents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoDetail(
    navController: NavController,
    list: TodoModel,
    workspaceId: String,
    onTodoEvents: (TodoEvents) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(list.title) }
    val itemList = rememberSaveable { list.items.toMutableStateList() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if(showDeleteDialog){
        DeleteAlert({
            showDeleteDialog=false
        }, {
            onTodoEvents(TodoEvents.DeleteList(list))
            navController.popBackStack()
            showDeleteDialog=false
        })
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(MaterialTheme.colorScheme.surfaceContainerLow),
                title = { Text(stringResource(R.string.Edit_list)) },
                navigationIcon = {
                    IconButton({ navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            null
                        )
                    }
                },
                actions = {
                    IconButton(
                        enabled = title.isNotBlank(),
                        onClick = {
                            navController.popBackStack()
                            onTodoEvents(
                                TodoEvents.UpsertList(
                                    list.copy(
                                        title = title,
                                        items = itemList.toList(),
                                        workspaceId = workspaceId
                                    )
                                )
                            )
                        }) { Icon(Icons.Default.Check, null) }
                    IconButton({ showDeleteDialog=true }) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
    ) { innerPadding ->
        LazyColumn(Modifier.padding(innerPadding)) {
            item {
                TextField(
                    value = title,
                    singleLine = true,
                    onValueChange = { title = it },
                    placeholder = { Text(stringResource(R.string.Title)) },
                    textStyle = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedTextColor = MaterialTheme.colorScheme.primary,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            items(itemList) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(0.85f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(item.isChecked, onCheckedChange = { checked ->
                            val index = itemList.indexOf(item)
                            if (index != -1) {
                                itemList[index] = item.copy(isChecked = checked)
                            }
                        })
                        TextField(
                            value = item.value,
                            singleLine = true,
                            onValueChange = { newText ->
                                val index = itemList.indexOf(item)
                                if (index != -1) {
                                    itemList[index] = item.copy(value = newText)
                                }
                            },
                            placeholder = { Text(stringResource(R.string.Title)) },
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedTextColor = MaterialTheme.colorScheme.primary,
                                focusedPlaceholderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    IconButton({ itemList.remove(item) }) {
                        Icon(
                            Icons.Default.Remove,
                            null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item {
                TextButton(
                    onClick = { itemList.add(TodoItem()) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
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