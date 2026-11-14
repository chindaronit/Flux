package com.omnipad.ui.screens.workspaces

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.omnipad.data.model.WorkspaceModel
import com.omnipad.data.model.getSpacesList
import com.omnipad.data.model.lockWith
import com.omnipad.data.model.removePasskey
import com.omnipad.other.ensureStorageRoot
import com.omnipad.ui.common.DeleteAlert
import com.omnipad.ui.common.SpaceTopBar
import com.omnipad.ui.events.HabitEvents
import com.omnipad.ui.events.JournalEvents
import com.omnipad.ui.events.NotesEvents
import com.omnipad.ui.events.ProgressBoardEvents
import com.omnipad.ui.events.TaskEvents
import com.omnipad.ui.events.TodoEvents
import com.omnipad.ui.events.WorkspaceEvents
import com.omnipad.ui.screens.analytics.AnalyticScreen
import com.omnipad.ui.screens.events.EventScreen
import com.omnipad.ui.screens.habits.HabitScreen
import com.omnipad.ui.screens.journal.JournalScreen
import com.omnipad.ui.screens.journal.FilterState as JournalFilterState
import com.omnipad.ui.screens.notes.NotesScreen
import com.omnipad.ui.screens.notes.FilterState as NotesFilterState
import com.omnipad.ui.screens.progressBoard.ProgressTrackerScreen
import com.omnipad.ui.screens.search.SearchScreen
import com.omnipad.ui.screens.todo.TodoScreen
import com.omnipad.ui.state.States
import com.omnipad.ui.viewModel.ViewModels
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.omnipad.R
import com.omnipad.navigation.NavRoutes

private const val VIRTUAL_PAGE_COUNT = 100_000

/**
 * Circular horizontal pager over [spaces].
 * Swipe left -> next space, swipe right -> previous space, wrapping at both ends.
 */
@Composable
fun CircularSpacePager(
    spaces: List<Int>,
    selectedSpaceId: Int,
    onPageSettled: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (spaceId: Int) -> Unit
) {
    val size = spaces.size
    if (size == 0) return

    // Rebuild the pager when spaces are added/removed
    key(spaces) {
        val circular = size > 1
        val startIndex = spaces.indexOf(selectedSpaceId).coerceAtLeast(0)
        val initialPage = if (circular) {
            val mid = VIRTUAL_PAGE_COUNT / 2
            mid - (mid % size) + startIndex
        } else 0

        val pagerState = rememberPagerState(initialPage = initialPage) {
            if (circular) VIRTUAL_PAGE_COUNT else 1
        }

        // Pager -> selection
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }
                .collect { onPageSettled(spaces[it.mod(size)]) }
        }

        // Selection -> pager (e.g. a chip/tab inside a screen calls onSpaceChange)
        LaunchedEffect(selectedSpaceId) {
            if (!circular || pagerState.isScrollInProgress) return@LaunchedEffect
            val target = spaces.indexOf(selectedSpaceId)
            val current = pagerState.currentPage
            val currentIdx = current.mod(size)
            if (target < 0 || target == currentIdx) return@LaunchedEffect

            var delta = target - currentIdx
            if (delta > size / 2) delta -= size
            else if (delta < -size / 2) delta += size // shortest circular path
            pagerState.animateScrollToPage(current + delta)
        }

        HorizontalPager(
            state = pagerState,
            modifier = modifier,
            userScrollEnabled = circular,
            key = { it }
        ) { page ->
            content(spaces[page.mod(size)])
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceDetails(
    navController: NavController,
    states: States,
    workspace: WorkspaceModel,
    viewModels: ViewModels,
    initialSpaceId: Int = -1
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val workspaceId = workspace.workspaceId
    var notesSearchQuery by rememberSaveable(workspaceId) { mutableStateOf("") }
    var isNotesSearchVisible by rememberSaveable(workspaceId) { mutableStateOf(false) }
    var notesFilterState by remember(workspaceId) { mutableStateOf(NotesFilterState()) }
    var todoSearchQuery by rememberSaveable(workspaceId) { mutableStateOf("") }
    var isTodoSearchVisible by rememberSaveable(workspaceId) { mutableStateOf(false) }
    val expandedTodoIds = rememberSaveable(workspaceId) {
        mutableStateOf<Set<String>>(emptySet())
    }
    var eventSearchQuery by rememberSaveable(workspaceId) { mutableStateOf("") }
    var journalSearchQuery by rememberSaveable(workspaceId) { mutableStateOf("") }
    var isJournalSearchVisible by rememberSaveable(workspaceId) { mutableStateOf(false) }
    var journalFilterState by remember(workspaceId) { mutableStateOf(JournalFilterState()) }
    var habitSearchQuery by rememberSaveable(workspaceId) { mutableStateOf("") }
    var isHabitSearchVisible by rememberSaveable(workspaceId) { mutableStateOf(false) }
    var progressSearchQuery by rememberSaveable(workspaceId) { mutableStateOf("") }
    var isProgressSearchVisible by rememberSaveable(workspaceId) { mutableStateOf(false) }
    val selectedSpaceId = rememberSaveable {
        mutableIntStateOf(
            when {
                initialSpaceId != -1 -> initialSpaceId
                workspace.selectedSpaces.isEmpty() -> -1
                else -> workspace.selectedSpaces.first()
            }
        )
    }
    var isDeleteDialogVisible by remember { mutableStateOf(false) }
    var isPasskeyDialogVisible by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val spacesList = getSpacesList()
    var showBottomSheet by remember { mutableStateOf(false) }
    var showAnalytics by rememberSaveable(workspaceId) { mutableStateOf(initialSpaceId == 6) }
    var searchInitialSpaceId by rememberSaveable(workspaceId) { mutableIntStateOf(initialSpaceId) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                viewModels.workspaceViewModel.onEvent(WorkspaceEvents.ChangeCover(context, uri, workspace))
            }
        }
    )

    val rootPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        viewModels.settingsViewModel.saveRootUri(uri)
    }

    // Shared callbacks, used by both the empty state and the pager pages
    val onAddCover: () -> Unit = {
        ensureStorageRoot(
            scope = scope,
            settingsViewModel = viewModels.settingsViewModel,
            rootPicker = rootPicker
        ) { imagePickerLauncher.launch("image/*") }
    }
    val onRemoveCover: () -> Unit = {
        viewModels.workspaceViewModel.onEvent(WorkspaceEvents.UpsertSpace(workspace.copy(cover = "")))
    }
    val onDeleteWorkspace: () -> Unit = { isDeleteDialogVisible = true }
    val onToggleLock: () -> Unit = {
        if (workspace.isLocked) {
            viewModels.workspaceViewModel.onEvent(WorkspaceEvents.UpsertSpace(workspace.removePasskey()))
        } else {
            isPasskeyDialogVisible = true
        }
    }
    val onShowSpaceBottomSheet: () -> Unit = { showBottomSheet = true }

    if (!showAnalytics) {
        SearchScreen(
            navController = navController,
            states = states,
            viewModels = viewModels,
            workspaceId = workspaceId,
            initialSpaceId = searchInitialSpaceId,
            workspaceHeader = {
                SpaceTopBar(
                    title = workspace.title,
                    description = workspace.description,
                    cover = workspace.cover,
                    icon = workspace.icon,
                    isLocked = workspace.isLocked,
                    onBackPressed = { navController.popBackStack() },
                    onAddCover = onAddCover,
                    onRemoveCover = onRemoveCover,
                    onToggleLock = onToggleLock,
                    onDeleteWorkspace = onDeleteWorkspace,
                    onEditWorkspace = { navController.navigate(NavRoutes.NewWorkspace.withArgs(workspaceId)) }
                )
            },
            workspaceControls = {
                Row(Modifier.padding(horizontal = 8.dp)) {
                    TextButton(onClick = onShowSpaceBottomSheet) {
                        Text(stringResource(R.string.Add_Space))
                    }
                    if (6 in workspace.selectedSpaces) {
                        TextButton(onClick = {
                            selectedSpaceId.intValue = 6
                            showAnalytics = true
                        }) {
                            Text(stringResource(R.string.Analytics))
                        }
                    }
                }
            }
        )
    } else if (workspace.selectedSpaces.isEmpty()) {
        SpaceMapper(
            navController = navController,
            workspace = workspace,
            spaceId = -1,
            states = states,
            viewModels = viewModels,
            onAddCover = onAddCover,
            onRemoveCover = onRemoveCover,
            onDeleteWorkspace = onDeleteWorkspace,
            onToggleLock = onToggleLock,
            onSpaceChange = {},
            onShowSpaceBottomSheet = onShowSpaceBottomSheet,
            eventSearchQuery = eventSearchQuery,
            onEventSearchQueryChange = { eventSearchQuery = it },
            notesSearchQuery = notesSearchQuery,
            onNotesSearchQueryChange = { notesSearchQuery = it },
            isNotesSearchVisible = isNotesSearchVisible,
            onNotesSearchVisibilityChange = { isNotesSearchVisible = it },
            notesFilterState = notesFilterState,
            onNotesFilterStateChange = { notesFilterState = it },
            todoSearchQuery = todoSearchQuery,
            onTodoSearchQueryChange = { todoSearchQuery = it },
            isTodoSearchVisible = isTodoSearchVisible,
            onTodoSearchVisibilityChange = { isTodoSearchVisible = it },
            expandedTodoIds = expandedTodoIds,
            journalSearchQuery = journalSearchQuery,
            onJournalSearchQueryChange = { journalSearchQuery = it },
            isJournalSearchVisible = isJournalSearchVisible,
            onJournalSearchVisibilityChange = { isJournalSearchVisible = it },
            journalFilterState = journalFilterState,
            onJournalFilterStateChange = { journalFilterState = it },
            habitSearchQuery = habitSearchQuery,
            onHabitSearchQueryChange = { habitSearchQuery = it },
            isHabitSearchVisible = isHabitSearchVisible,
            onHabitSearchVisibilityChange = { isHabitSearchVisible = it },
            progressSearchQuery = progressSearchQuery,
            onProgressSearchQueryChange = { progressSearchQuery = it },
            isProgressSearchVisible = isProgressSearchVisible,
            onProgressSearchVisibilityChange = { isProgressSearchVisible = it },
        )
    } else {
        CircularSpacePager(
            spaces = workspace.selectedSpaces,
            selectedSpaceId = selectedSpaceId.intValue,
            onPageSettled = {
                selectedSpaceId.intValue = it
                if (it != 6) {
                    searchInitialSpaceId = it
                    showAnalytics = false
                }
            },
        ) { id ->
            SpaceMapper(
                navController = navController,
                workspace = workspace,
                spaceId = id,
                states = states,
                viewModels = viewModels,
                onAddCover = onAddCover,
                onRemoveCover = onRemoveCover,
                onDeleteWorkspace = onDeleteWorkspace,
                onToggleLock = onToggleLock,
                onSpaceChange = {
                    selectedSpaceId.intValue = it
                    if (it != 6) {
                        searchInitialSpaceId = it
                        showAnalytics = false
                    }
                },
                onShowSpaceBottomSheet = onShowSpaceBottomSheet,
                eventSearchQuery = eventSearchQuery,
                onEventSearchQueryChange = { eventSearchQuery = it },
                notesSearchQuery = notesSearchQuery,
                onNotesSearchQueryChange = { notesSearchQuery = it },
                isNotesSearchVisible = isNotesSearchVisible,
                onNotesSearchVisibilityChange = { isNotesSearchVisible = it },
                notesFilterState = notesFilterState,
                onNotesFilterStateChange = { notesFilterState = it },
                todoSearchQuery = todoSearchQuery,
                onTodoSearchQueryChange = { todoSearchQuery = it },
                isTodoSearchVisible = isTodoSearchVisible,
                onTodoSearchVisibilityChange = { isTodoSearchVisible = it },
                expandedTodoIds = expandedTodoIds,
                journalSearchQuery = journalSearchQuery,
                onJournalSearchQueryChange = { journalSearchQuery = it },
                isJournalSearchVisible = isJournalSearchVisible,
                onJournalSearchVisibilityChange = { isJournalSearchVisible = it },
                journalFilterState = journalFilterState,
                onJournalFilterStateChange = { journalFilterState = it },
                habitSearchQuery = habitSearchQuery,
                onHabitSearchQueryChange = { habitSearchQuery = it },
                isHabitSearchVisible = isHabitSearchVisible,
                onHabitSearchVisibilityChange = { isHabitSearchVisible = it },
                progressSearchQuery = progressSearchQuery,
                onProgressSearchQueryChange = { progressSearchQuery = it },
                isProgressSearchVisible = isProgressSearchVisible,
                onProgressSearchVisibilityChange = { isProgressSearchVisible = it },
            )
        }
    }

    if (isPasskeyDialogVisible) {
        SetPasskeyDialog(
            onConfirmRequest = {
                viewModels.workspaceViewModel.onEvent(WorkspaceEvents.UpsertSpace(workspace.lockWith(it)))
            },
            onDismissRequest = { isPasskeyDialogVisible = false }
        )
    }

    if (isDeleteDialogVisible) {
        DeleteAlert(
            onConfirmation = {
                isDeleteDialogVisible = false
                navController.popBackStack()
                clearWorkspace(context, workspace, viewModels)
            },
            onDismissRequest = { isDeleteDialogVisible = false }
        )
    }

    AddNewSpacesBottomSheet(
        isVisible = showBottomSheet,
        sheetState = sheetState,
        selectedSpaces = spacesList.filter { workspace.selectedSpaces.contains(it.id) },
        onDismiss = { showBottomSheet = false },
        onRemove = { spaceId ->
            val newSelected = workspace.selectedSpaces.firstOrNull { it != spaceId } ?: -1
            selectedSpaceId.intValue = newSelected
            if (spaceId == 6 && showAnalytics) {
                searchInitialSpaceId = newSelected
                showAnalytics = false
            }

            viewModels.workspaceViewModel.onEvent(
                WorkspaceEvents.UpsertSpace(
                    workspace.copy(selectedSpaces = workspace.selectedSpaces.minus(spaceId))
                )
            )

            removeSpaceData(workspaceId, spaceId, context, viewModels)
        },
        onSelect = {
            if (selectedSpaceId.intValue == -1) selectedSpaceId.intValue = it
            viewModels.workspaceViewModel.onEvent(
                WorkspaceEvents.UpsertSpace(
                    workspace.copy(selectedSpaces = workspace.selectedSpaces.plus(it))
                )
            )
        }
    )
}

fun clearWorkspace(context: Context, workspace: WorkspaceModel, viewModels: ViewModels) {
    viewModels.workspaceViewModel.onEvent(WorkspaceEvents.DeleteSpace(workspace))
    workspace.selectedSpaces.forEach { spaceId ->
        removeSpaceData(workspace.workspaceId, spaceId, context, viewModels)
    }
}

fun removeSpaceData(
    workspaceId: String,
    spaceId: Int,
    context: Context,
    viewModels: ViewModels
) {
    when (spaceId) {
        1 -> viewModels.notesViewModel.onEvent(NotesEvents.DeleteAllWorkspaceNotes(workspaceId))
        2 -> viewModels.todoViewModel.onEvent(TodoEvents.DeleteAllWorkspaceLists(context, workspaceId))
        3 -> viewModels.eventViewModel.onEvent(TaskEvents.DeleteAllWorkspaceEvents(workspaceId, context))
        4 -> viewModels.journalViewModel.onEvent(JournalEvents.DeleteWorkspaceEntries(workspaceId))
        5 -> viewModels.habitViewModel.onEvent(HabitEvents.DeleteAllWorkspaceHabits(workspaceId, context))
        7 -> viewModels.progressBoardViewModel.onEvent(ProgressBoardEvents.DeleteBoardItemsByWorkspace(workspaceId))
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun SpaceMapper(
    navController: NavController,
    workspace: WorkspaceModel,
    spaceId: Int,
    states: States,
    viewModels: ViewModels,
    onAddCover: () -> Unit,
    onRemoveCover: () -> Unit,
    onDeleteWorkspace: () -> Unit,
    onToggleLock: () -> Unit,
    onSpaceChange: (Int) -> Unit,
    onShowSpaceBottomSheet: () -> Unit,
    eventSearchQuery: String,
    onEventSearchQueryChange: (String) -> Unit,
    notesSearchQuery: String,
    onNotesSearchQueryChange: (String) -> Unit,
    isNotesSearchVisible: Boolean,
    onNotesSearchVisibilityChange: (Boolean) -> Unit,
    notesFilterState: NotesFilterState,
    onNotesFilterStateChange: (NotesFilterState) -> Unit,
    todoSearchQuery: String,
    onTodoSearchQueryChange: (String) -> Unit,
    isTodoSearchVisible: Boolean,
    onTodoSearchVisibilityChange: (Boolean) -> Unit,
    expandedTodoIds: androidx.compose.runtime.MutableState<Set<String>>,
    journalSearchQuery: String,
    onJournalSearchQueryChange: (String) -> Unit,
    isJournalSearchVisible: Boolean,
    onJournalSearchVisibilityChange: (Boolean) -> Unit,
    journalFilterState: JournalFilterState,
    onJournalFilterStateChange: (JournalFilterState) -> Unit,
    habitSearchQuery: String,
    onHabitSearchQueryChange: (String) -> Unit,
    isHabitSearchVisible: Boolean,
    onHabitSearchVisibilityChange: (Boolean) -> Unit,
    progressSearchQuery: String,
    onProgressSearchQueryChange: (String) -> Unit,
    isProgressSearchVisible: Boolean,
    onProgressSearchVisibilityChange: (Boolean) -> Unit,
) {
    when (spaceId) {
        1 -> NotesScreen(
            navController,
            states.notesState,
            states.labelState.allLabels.filter { it.workspaceId == workspace.workspaceId },
            states.settings,
            workspace,
            onShowSpaceBottomSheet,
            onSpaceChange,
            onAddCover,
            onRemoveCover,
            onDeleteWorkspace,
            onToggleLock,
            viewModels.notesViewModel::onEvent,
            viewModels.settingsViewModel::onEvent,
            notesSearchQuery,
            onNotesSearchQueryChange,
            isNotesSearchVisible,
            onNotesSearchVisibilityChange,
            notesFilterState,
            onNotesFilterStateChange
        )

        2 -> TodoScreen(
            navController,
            states.todoState,
            states.settings,
            workspace,
            onShowSpaceBottomSheet,
            onSpaceChange,
            onAddCover,
            onRemoveCover,
            onDeleteWorkspace,
            onToggleLock,
            viewModels.todoViewModel::onEvent,
            todoSearchQuery,
            onTodoSearchQueryChange,
            isTodoSearchVisible,
            onTodoSearchVisibilityChange,
            expandedTodoIds
        )

        3 -> EventScreen(
            navController,
            states.eventState,
            states.settings,
            workspace,
            onShowSpaceBottomSheet,
            onSpaceChange,
            onAddCover,
            onRemoveCover,
            onDeleteWorkspace,
            onToggleLock,
            viewModels.eventViewModel::onEvent,
            eventSearchQuery,
            onEventSearchQueryChange
        )

        4 -> JournalScreen(
            navController,
            states.journalState,
            states.settings,
            workspace,
            states.labelState.allLabels.filter { it.workspaceId == workspace.workspaceId },
            onShowSpaceBottomSheet,
            onSpaceChange,
            onAddCover,
            onRemoveCover,
            onDeleteWorkspace,
            onToggleLock,
            viewModels.journalViewModel::onEvent,
            journalSearchQuery,
            onJournalSearchQueryChange,
            isJournalSearchVisible,
            onJournalSearchVisibilityChange,
            journalFilterState,
            onJournalFilterStateChange
        )

        5 -> HabitScreen(
            navController,
            states.habitState,
            states.settings,
            workspace,
            onShowSpaceBottomSheet,
            onSpaceChange,
            onAddCover,
            onRemoveCover,
            onDeleteWorkspace,
            onToggleLock,
            viewModels.habitViewModel::onEvent,
            habitSearchQuery,
            onHabitSearchQueryChange,
            isHabitSearchVisible,
            onHabitSearchVisibilityChange
        )

        6 -> AnalyticScreen(
            navController,
            states,
            workspace,
            workspace.selectedSpaces,
            onShowSpaceBottomSheet,
            onSpaceChange,
            onAddCover,
            onRemoveCover,
            onDeleteWorkspace,
            onToggleLock
        )

        7 -> ProgressTrackerScreen(
            navController,
            states.progressBoardState,
            states.settings,
            workspace,
            onShowSpaceBottomSheet,
            onSpaceChange,
            onAddCover,
            onRemoveCover,
            onDeleteWorkspace,
            onToggleLock,
            progressSearchQuery,
            onProgressSearchQueryChange,
            isProgressSearchVisible,
            onProgressSearchVisibilityChange
        )

        else -> EmptyWorkspace(
            navController,
            workspace,
            onShowSpaceBottomSheet,
            onAddCover,
            onRemoveCover,
            onDeleteWorkspace,
            onToggleLock
        )
    }
}