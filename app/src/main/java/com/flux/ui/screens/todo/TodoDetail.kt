package com.flux.ui.screens.todo

import android.app.Activity
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flux.R
import com.flux.data.model.JournalModel
import com.flux.data.model.NotesModel
import com.flux.data.model.RecurrenceRule
import com.flux.data.model.TodoInstance
import com.flux.data.model.WorkspaceModel
import com.flux.data.model.toHtml
import com.flux.data.model.toMarkdown
import com.flux.data.model.toMarkdownContent
import com.flux.navigation.NavRoutes
import com.flux.other.ConvertType
import com.flux.other.DataCopyType
import com.flux.other.printPdf
import com.flux.other.shareTodo
import com.flux.ui.common.DataCopyDialog
import com.flux.ui.common.TodoDropdownMenu
import com.flux.ui.events.JournalEvents
import com.flux.ui.events.NotesEvents
import com.flux.ui.events.WorkspaceEvents
import java.time.LocalDate
import com.flux.ui.screens.notes.ShareDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoDetail(
    navController: NavController,
    radius: Int,
    list: TodoModel,
    workspaces: List<WorkspaceModel>,
    instances: List<TodoInstance>,
    workspaceId: String,
    onTodoEvents: (TodoEvents) -> Unit,
    onNotesEvents: (NotesEvents) -> Unit,
    onJournalEvents: (JournalEvents) -> Unit,
    onWorkspaceEvents: (WorkspaceEvents) -> Unit
) {
    val todayEpoch = LocalDate.now().toEpochDay()
    val context = LocalContext.current
    val currentWorkspace = workspaces.find { it.workspaceId == workspaceId }
    val isReminderOn = list.recurrence is RecurrenceRule.Weekly
    var showShareDialog by remember { mutableStateOf(false) }
    var showConvertDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDataCopyDialog by remember { mutableStateOf(false) }
    val todayInstance = instances.find { it.instanceDate == todayEpoch }
    val cloneString = stringResource(R.string.clone_created_successfully)
    val contentCopiedString = stringResource(R.string.content_copied)
    val contentMovedString = stringResource(R.string.content_moved)
    val successString = stringResource(R.string.success)

    val webView = WebView(context)
    webView.settings.javaScriptEnabled = false

    webView.loadDataWithBaseURL(
        null,
        if(isReminderOn) todayInstance?.toHtml(list.title) ?: list.toHtml() else list.toHtml(),
        "text/html",
        "UTF-8",
        null
    )

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

                    TodoDropdownMenu(
                        (isReminderOn && isAllowed(todayEpoch) && todayInstance!=null) || !isReminderOn,
                        { showShareDialog = true },
                        { printPdf(context as Activity, webView, list.title) },
                        {
                            onTodoEvents(TodoEvents.UpsertList(context, false,TodoModel(items = list.items, recurrence = list.recurrence, workspaceId = list.workspaceId, title = "Clone ${list.title}")))
                            Toast.makeText(context, cloneString, Toast.LENGTH_SHORT).show()
                        },
                        { showDataCopyDialog = true },
                        { showConvertDialog = true },
                        { showDeleteDialog = true}
                    )
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

    if (showShareDialog) {
        ShareDialog(true, {
            shareTodo(
                context = context,
                exportType = it,
                list = list,
                readWebView = webView
            )
            showShareDialog = false
        }) { showShareDialog = false }
    }

    if(showDataCopyDialog){
        DataCopyDialog(
            workspaces.filterNot { it.workspaceId == workspaceId },
            { dataCopyType, selectedWorkspaces ->
                if(selectedWorkspaces.isEmpty()) return@DataCopyDialog
                when(dataCopyType){
                    DataCopyType.COPY -> {
                        selectedWorkspaces.forEach { workspace ->
                            if(!workspace.selectedSpaces.contains(2)){
                                onWorkspaceEvents(WorkspaceEvents.UpsertSpace(workspace.copy(selectedSpaces = workspace.selectedSpaces + 2)))
                            }
                            onTodoEvents(TodoEvents.UpsertList(context, false,TodoModel(items = list.items, recurrence = list.recurrence, workspaceId = workspace.workspaceId, title = list.title)))
                        }

                        Toast.makeText(context, contentCopiedString, Toast.LENGTH_SHORT).show()
                    }
                    DataCopyType.MOVE -> {
                        selectedWorkspaces.forEach { workspace ->
                            if(!workspace.selectedSpaces.contains(2)){
                                onWorkspaceEvents(WorkspaceEvents.UpsertSpace(workspace.copy(selectedSpaces = workspace.selectedSpaces + 2)))
                            }
                            onTodoEvents(TodoEvents.UpsertList(context, false,TodoModel(items = list.items, recurrence = list.recurrence, workspaceId = workspace.workspaceId, title = list.title)))
                        }

                        navController.popBackStack()
                        Toast.makeText(context, contentMovedString, Toast.LENGTH_SHORT).show()
                        onTodoEvents(TodoEvents.DeleteList(context, list))
                    }
                }
            }
        ) { showDataCopyDialog = false }
    }

    if(showConvertDialog){
        ConvertTODODialog ({
            when(it){
                ConvertType.NOTE -> {
                    if(!currentWorkspace!!.selectedSpaces.contains(1)){
                        val newSelectedSpaces = currentWorkspace.selectedSpaces + 1
                        onWorkspaceEvents(WorkspaceEvents.UpsertSpace(currentWorkspace.copy(selectedSpaces = newSelectedSpaces)))
                    }

                    onNotesEvents(
                        NotesEvents.UpsertNote(
                            NotesModel(
                                title = list.title,
                                description = list.toMarkdownContent(),
                                workspaceId = list.workspaceId
                            )
                        )
                    )
                    navController.popBackStack()
                    onTodoEvents(TodoEvents.DeleteList(context,list))
                }
                ConvertType.JOURNAL -> {
                    if(!currentWorkspace!!.selectedSpaces.contains(4)){
                        val newSelectedSpaces = currentWorkspace.selectedSpaces + 4
                        onWorkspaceEvents(WorkspaceEvents.UpsertSpace(currentWorkspace.copy(selectedSpaces = newSelectedSpaces)))
                    }

                    onJournalEvents(
                        JournalEvents.UpsertEntry(
                            JournalModel(
                                text = list.toMarkdown(),
                                workspaceId = list.workspaceId
                            )
                        )
                    )

                    navController.popBackStack()
                    onTodoEvents(TodoEvents.DeleteList(context,list))
                }
                else -> {}
            }

            Toast.makeText(context, successString, Toast.LENGTH_SHORT).show()
            showConvertDialog=false
        }) {
            showConvertDialog=false
        }
    }
}