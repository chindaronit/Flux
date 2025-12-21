package com.flux.ui.screens.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
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


    if (isLoading) item { Loader() }
    else if (allList.isEmpty()) item { EmptyTodoList() }
    else {
        item {
            var expanded by remember { mutableStateOf("") }
            var showDeleteDialog by remember { mutableStateOf(false) }
            var selectedItem by remember { mutableStateOf<TodoModel?>(null) }

            if(showDeleteDialog){
                DeleteAlert(onConfirmation = {
                    onTodoEvents(TodoEvents.DeleteList(selectedItem!!))
                    selectedItem=null
                    showDeleteDialog=false
                }, onDismissRequest = {
                    showDeleteDialog=false
                })
            }

            ElevatedCard(
                modifier = Modifier.padding(top = 8.dp),
                shape = shapeManager(radius = radius * 2),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        6.dp
                    )
                )
            ) {
                Column {
                    allList.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate(
                                        NavRoutes.TodoDetail.withArgs(
                                            workspaceId,
                                            item.id
                                        )
                                    )
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).padding(end = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(onClick = {
                                    expanded = if (expanded != item.id) item.id else ""
                                }) {
                                    Icon(if (expanded == item.id) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Default.KeyboardArrowRight, null)
                                }
                                Text(
                                    item.title,
                                    fontSize = 16.sp,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1
                                )
                            }
                            IconButton(onClick = {
                                selectedItem=item
                                showDeleteDialog=true
                            }) {
                                Icon(
                                    Icons.Default.Remove,
                                    null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        if (expanded == item.id) {
                            item.items.take(3).forEach { checkItem ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .padding(start = 64.dp, end = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(50))
                                            .size(8.dp)
                                            .background(
                                                MaterialTheme.colorScheme.onSurface.copy(0.75f)
                                            )
                                    )
                                    Text(
                                        checkItem.value,
                                        textDecoration = if (checkItem.isChecked) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                }
                            }
                            if (item.items.size > 3) {
                                Text(
                                    stringResource(R.string.more),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .padding(start = 70.dp, end = 8.dp)
                                )
                            }
                        }
                        HorizontalDivider(Modifier.alpha(0.5f))
                    }
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
