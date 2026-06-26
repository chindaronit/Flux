package com.flux.ui.screens.notes

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import com.flux.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.data.model.LabelModel
import com.flux.data.model.NotesModel
import com.flux.data.model.WorkspaceModel
import com.flux.navigation.Loader
import com.flux.navigation.NavRoutes
import com.flux.ui.common.DeleteAlert
import com.flux.ui.common.EmptyNotes
import com.flux.ui.common.SelectedToolBarRow
import com.flux.ui.common.SpaceSearchBar
import com.flux.ui.common.SpaceTopBar
import com.flux.ui.common.SpacesMenu
import com.flux.ui.events.NotesEvents
import com.flux.ui.events.SettingEvents
import com.flux.ui.screens.workspaces.SpacesToolBar
import com.flux.ui.state.NotesState
import com.flux.ui.state.Settings

data class FilterState(
    val sort: String? = null,
    val view: String? = null,
    val pinned: String? = null,
    val selectedLabelIds: Set<String> = emptySet()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    navController: NavController,
    state: NotesState,
    allLabels: List<LabelModel>,
    settings: Settings,
    workspace: WorkspaceModel,
    onShowSpaceBottomSheet: () -> Unit,
    onSpaceChange: (Int) -> Unit,
    onAddCover: () -> Unit,
    onRemoveCover: () -> Unit,
    onDeleteWorkspace: () -> Unit,
    onToggleLock: () -> Unit,
    allWorkspaces: List<WorkspaceModel> = emptyList(),
    onWorkspaceSelected: (WorkspaceModel) -> Unit = {},
    onNewWorkspace: () -> Unit = {},
    onEvent: (NotesEvents) -> Unit,
    onSettingChange: (SettingEvents) -> Unit
){
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    val workspaceId = workspace.workspaceId
    val isGridView = settings.data.isGridView
    val radius = settings.data.cornerRadius
    val isLoading = state.isLoading
    val notesPreviewMode = settings.data.notesPreviewMode
    val columns = if(isGridView) 2 else 1
    val selectedNotes = remember { mutableStateListOf<NotesModel>() }
    val importSuccess = stringResource(R.string.import_success)
    val importFailed = stringResource(R.string.import_failed)
    var isToolsSheetVisible by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var isDialogVisible by remember { mutableStateOf(false) }
    var showSpacesMenu by remember { mutableStateOf(false) }
    var isActionButtonExpanded by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var filterState by remember { mutableStateOf(FilterState()) }
    val labelMap = remember(allLabels) { allLabels.associateBy { it.labelId } }
    val sortByLabel = stringResource(R.string.sort_by)
    val labelName = stringResource(R.string.labels)
    val viewLabel = stringResource(R.string.view)
    val pinnedLabel = stringResource(R.string.pinned)

    val allNotes = state.allNotes
        .filter { note->
            note.workspaceId==workspaceId
        }

        // 🔍 Search
        .filter { note ->
            val matchesText =
                note.title.contains(query, true) ||
                        note.description.contains(query, true)

            val matchesLabel =
                note.labels.any { labelId ->
                    labelMap[labelId]?.value
                        ?.contains(query, true) == true
                }

            matchesText || matchesLabel
        }

        // 📌 Pinned filter
        .let { notes ->
            when (filterState.pinned) {
                "pinned" -> notes.filter { it.isPinned }
                "unpinned" -> notes.filter { !it.isPinned }
                else -> notes
            }
        }

        // 🏷 Label filter
        .let { notes ->
            if (filterState.selectedLabelIds.isEmpty()) notes
            else notes.filter { note ->
                note.labels.any { it in filterState.selectedLabelIds }
            }
        }

        // 🔄 Sorting
        .let { notes ->
            when (filterState.sort) {
                "latest" -> notes.sortedByDescending { it.lastEdited }
                "oldest" -> notes.sortedBy { it.lastEdited }
                else -> notes
            }
        }

    val pinnedNotes = allNotes.filter { it.isPinned }
    val unPinnedNotes = allNotes.filter { !it.isPinned }
    val showLabels = pinnedNotes.isNotEmpty()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""

                // Get filename (remove extension)
                val fileName = uri.lastPathSegment
                    ?.substringAfterLast("/")
                    ?.substringBeforeLast(".")
                    ?: "Imported Note"

                // Create a new note
                val newNote = NotesModel(
                    title = fileName,
                    description = content,
                    workspaceId = workspaceId,
                    lastEdited = System.currentTimeMillis()
                )

                onEvent(NotesEvents.UpsertNote(newNote))
                Toast.makeText(context, importSuccess, Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, importFailed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            SpaceTopBar(
                scrollBehavior = scrollBehavior,
                workspace = workspace,
                allWorkspaces = allWorkspaces,
                onAddCover = onAddCover,
                onRemoveCover = onRemoveCover,
                onEditWorkspace = { navController.navigate(NavRoutes.NewWorkspace.withArgs(workspaceId)) },
                onDeleteWorkspace = onDeleteWorkspace,
                onToggleLock = onToggleLock,
                onWorkspaceSelected = onWorkspaceSelected,
                onNewWorkspace = onNewWorkspace
            )
        },
        floatingActionButton = {
            Box(contentAlignment = Alignment.BottomEnd) {
                // Expanded actions
                AnimatedVisibility(
                    visible = isActionButtonExpanded,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        val buttonModifier = Modifier.width(140.dp)

                        ExtendedFloatingActionButton(
                            modifier = buttonModifier,
                            onClick = { importLauncher.launch(arrayOf("text/markdown", "text/plain")) },
                            icon = { Icon(Icons.Outlined.Download, contentDescription = null) },
                            text = { Text(stringResource(R.string.Import)) }
                        )

                        ExtendedFloatingActionButton(
                            modifier = buttonModifier,
                            onClick = { navController.navigate(NavRoutes.NoteDetails.withArgs(workspaceId, "")) },
                            icon = { Icon(Icons.Outlined.Create, contentDescription = null) },
                            text = { Text(stringResource(R.string.create)) }
                        )

                        ExtendedFloatingActionButton(
                            modifier = buttonModifier,
                            onClick = { isActionButtonExpanded = false },
                            icon = { Icon(Icons.Outlined.Clear, contentDescription = null) },
                            text = { Text(stringResource(R.string.clear)) }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = !isActionButtonExpanded,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    FloatingActionButton( { isActionButtonExpanded = true }) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            }
        }
    ) { innerPadding ->
        when {
            isLoading -> Loader()
            else -> {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(columns),
                        verticalItemSpacing = 8.dp,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = 12.dp,
                            bottom = 80.dp
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                    ) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            if (selectedNotes.isEmpty()){
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if(showSearchBar){ SpaceSearchBar(query, { query=it }, { showSearchBar=false }) }
                                    else {
                                        SpacesToolBar(
                                            stringResource(R.string.Notes),
                                            Icons.AutoMirrored.Filled.Notes,
                                            false,
                                            onMainClick = { showSpacesMenu = true },
                                            onEditClick = onShowSpaceBottomSheet
                                        )
                                        SpacesMenu(
                                            showSpacesMenu,
                                            workspace,
                                            onSpaceChange
                                        ) { showSpacesMenu = false }

                                        Row{
                                            IconButton({ showSearchBar = true }) {
                                                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton({ isToolsSheetVisible = true }) {
                                                Icon(Icons.Outlined.FilterList, null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                            else{
                                SelectedToolBarRow(
                                    true,
                                    selectedNotes.size,
                                    selectedNotes.size == allNotes.size,
                                    pinnedNotes.size == allNotes.size,
                                    { selectedNotes.clear() },
                                    { isDialogVisible = true },
                                    {
                                        onEvent(NotesEvents.TogglePinMultiple(selectedNotes.toList()))
                                        selectedNotes.clear()
                                    },
                                    {
                                        if (allNotes.size == selectedNotes.size) {
                                            selectedNotes.clear()
                                        } else {
                                            selectedNotes.clear()
                                            selectedNotes.addAll(allNotes)
                                        }
                                    }
                                )
                            }
                        }
                        if (allNotes.isEmpty()) item(span = StaggeredGridItemSpan.FullLine) { EmptyNotes() }
                        if(showLabels) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Text(
                                    text = stringResource(R.string.Pinned),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        items(pinnedNotes, key = { it.notesId }) { note ->
                            NotesPreviewCard(
                                radius = radius,
                                isSelected = selectedNotes.contains(note),
                                note = note,
                                notesPreviewMode = notesPreviewMode,
                                labels = allLabels.filter { note.labels.contains(it.labelId) }.map { it.value },
                                onClick = { navController.navigate(NavRoutes.NoteDetails.withArgs(workspaceId, note.notesId)) },
                                onLongPressed = {
                                    if (selectedNotes.contains(note)) {
                                        selectedNotes.remove(note)
                                    } else {
                                        selectedNotes.add(note)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if(showLabels) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Text(
                                    text = stringResource(R.string.Others),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        items(unPinnedNotes, key = { it.notesId }) { note ->
                            NotesPreviewCard(
                                radius = radius,
                                isSelected = selectedNotes.contains(note),
                                note = note,
                                notesPreviewMode = notesPreviewMode,
                                labels = allLabels.filter { note.labels.contains(it.labelId) }.map { it.value },
                                onClick = { navController.navigate(NavRoutes.NoteDetails.withArgs(workspaceId, note.notesId)) },
                                onLongPressed = {
                                    if (selectedNotes.contains(note)) {
                                        selectedNotes.remove(note)
                                    } else {
                                        selectedNotes.add(note)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
            }
        }
    }

    if (isDialogVisible) {
        DeleteAlert(onConfirmation = {
            onEvent(NotesEvents.DeleteNotes(selectedNotes.toList()))
            selectedNotes.clear()
            isDialogVisible = false
        }, onDismissRequest = { isDialogVisible = false })
    }

    if(isToolsSheetVisible) {
        NotesFilterSheet(
            filterState.copy(view = if(isGridView) "grid" else "list"),
            allLabels,
            rememberModalBottomSheetState(),
            onDismiss = { isToolsSheetVisible = false },
        ) { single, multi ->
            filterState = FilterState(
                sort = single[sortByLabel],
                view = single[viewLabel],
                pinned = single[pinnedLabel],
                selectedLabelIds = multi[labelName] ?: emptySet()
            )

            if(isGridView && single[viewLabel]=="list" || !isGridView && single[viewLabel]=="grid"){
                onSettingChange(SettingEvents.UpdateSettings(settings.data.copy(isGridView=(single[viewLabel]=="grid"))))
            }
        }
    }
}
