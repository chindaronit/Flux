package com.flux.ui.screens.workspaces

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.flux.data.model.WorkspaceModel
import com.flux.data.model.getSpacesList
import com.flux.navigation.NavRoutes
import com.flux.other.ensureStorageRoot
import com.flux.ui.common.DeleteAlert
import com.flux.ui.events.HabitEvents
import com.flux.ui.events.JournalEvents
import com.flux.ui.events.NotesEvents
import com.flux.ui.events.ProgressBoardEvents
import com.flux.ui.events.SettingEvents
import com.flux.ui.events.TaskEvents
import com.flux.ui.events.TodoEvents
import com.flux.ui.events.WorkspaceEvents
import com.flux.ui.screens.analytics.AnalyticScreen
import com.flux.ui.screens.events.EventScreen
import com.flux.ui.screens.habits.HabitScreen
import com.flux.ui.screens.journal.JournalScreen
import com.flux.ui.screens.notes.NotesScreen
import com.flux.ui.screens.progressBoard.ProgressTrackerScreen
import com.flux.ui.screens.todo.TodoScreen
import com.flux.ui.state.States
import com.flux.ui.viewModel.ViewModels

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceDetails(
    navController: NavController,
    states: States,
    workspace: WorkspaceModel,
    viewModels: ViewModels
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val workspaceId = workspace.workspaceId
    val selectedSpaceId = rememberSaveable { mutableIntStateOf(if (workspace.selectedSpaces.isEmpty()) -1 else workspace.selectedSpaces.first()) }
    var isDeleteDialogVisible by remember { mutableStateOf(false) }
    var isPasskeyDialogVisible by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val spacesList = getSpacesList()
    var showBottomSheet by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> uri?.let { viewModels.workspaceViewModel.onEvent(WorkspaceEvents.ChangeCover(context, uri, workspace)) } }
    )

    val rootPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val uri = result.data?.data ?: return@rememberLauncherForActivityResult
            viewModels.settingsViewModel.saveRootUri(uri)
        }

    // Save this workspace as last opened
    LaunchedEffect(workspaceId) {
        viewModels.settingsViewModel.onEvent(
            SettingEvents.UpdateSettings(
                states.settings.data.copy(defaultWorkspaceId = workspaceId)
            )
        )
    }

    SpaceMapper(
        navController = navController,
        workspace = workspace,
        spaceId = selectedSpaceId.intValue,
        allWorkspaces = states.workspaceState.allWorkspaces,
        states = states,
        viewModels = viewModels,
        onAddCover = {
            ensureStorageRoot(
                scope = scope,
                settingsViewModel = viewModels.settingsViewModel,
                rootPicker = rootPicker
            ) { imagePickerLauncher.launch("image/*") }
        },
        onRemoveCover = { viewModels.workspaceViewModel.onEvent(WorkspaceEvents.UpsertSpace(workspace.copy(cover = ""))) },
        onDeleteWorkspace = { isDeleteDialogVisible = true },
        onToggleLock = {
            if(workspace.passKey!=null) {
                viewModels.workspaceViewModel.onEvent(WorkspaceEvents.UpsertSpace(workspace.copy(passKey = null)))
            }
            else { isPasskeyDialogVisible = true }
        },
        onSpaceChange = { selectedSpaceId.intValue = it },
        onShowSpaceBottomSheet = { showBottomSheet = true },
        onWorkspaceSelected = { ws ->
            if (ws.workspaceId != workspace.workspaceId) {
                navController.navigate(NavRoutes.WorkspaceHome.withArgs(ws.workspaceId)) {
                    popUpTo(NavRoutes.WorkspaceHome.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        },
        onNewWorkspace = {
            navController.navigate(NavRoutes.NewWorkspace.withArgs(""))
        }
    )

    if (isPasskeyDialogVisible) {
        SetPasskeyDialog(
            onConfirmRequest = {
                viewModels.workspaceViewModel.onEvent(WorkspaceEvents.UpsertSpace(workspace.copy(passKey = it)))
            },
            onDismissRequest = { isPasskeyDialogVisible = false }
        )
    }

    if (isDeleteDialogVisible) {
        DeleteAlert(onConfirmation = {
            isDeleteDialogVisible = false
            navController.popBackStack()
            clearWorkspace(context, workspace, viewModels)
        }, onDismissRequest = {
            isDeleteDialogVisible = false
        })
    }

    AddNewSpacesBottomSheet(
        isVisible = showBottomSheet,
        sheetState = sheetState,
        selectedSpaces = spacesList.filter { workspace.selectedSpaces.contains(it.id) },
        onDismiss = { showBottomSheet = false },
        onRemove = { spaceId ->
            val newSelected = workspace.selectedSpaces.firstOrNull { it != spaceId } ?: -1
            selectedSpaceId.intValue = newSelected

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
                    workspace.copy(
                        selectedSpaces = workspace.selectedSpaces.plus(
                            it
                        )
                    )
                )
            )
        }
    )
}

fun clearWorkspace(context: Context, workspace: WorkspaceModel, viewModels: ViewModels){
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
    allWorkspaces: List<WorkspaceModel>,
    states: States,
    viewModels: ViewModels,
    onAddCover: () -> Unit,
    onRemoveCover: () -> Unit,
    onDeleteWorkspace: () -> Unit,
    onToggleLock: () -> Unit,
    onSpaceChange: (Int) -> Unit,
    onShowSpaceBottomSheet: () -> Unit,
    onWorkspaceSelected: (WorkspaceModel) -> Unit = {},
    onNewWorkspace: () -> Unit = {},
) {
    when (spaceId) {
        1 -> NotesScreen(
            navController,
            states.notesState,
            states.labelState.allLabels.filter { it.workspaceId==workspace.workspaceId },
            states.settings,
            workspace,
            allWorkspaces,
            onShowSpaceBottomSheet,
            onSpaceChange,
            onAddCover,
            onRemoveCover,
            onDeleteWorkspace,
            onToggleLock,
            viewModels.notesViewModel::onEvent,
            viewModels.settingsViewModel::onEvent,
            onWorkspaceSelected,
            onNewWorkspace
        )

        2 -> TodoScreen(
            navController,
            states.todoState,
            states.settings,
            workspace,
            allWorkspaces,
            onShowSpaceBottomSheet,
            onSpaceChange,
            onAddCover,
            onRemoveCover,
            onDeleteWorkspace,
            onToggleLock,
            viewModels.todoViewModel::onEvent,
            onWorkspaceSelected,
            onNewWorkspace
        )

        3 -> EventScreen(
            navController,
            states.eventState,
            states.settings,
            workspace,
            allWorkspaces,
            onShowSpaceBottomSheet,
            onSpaceChange,
            onAddCover,
            onRemoveCover,
            onDeleteWorkspace,
            onToggleLock,
            viewModels.settingsViewModel::onEvent,
            viewModels.eventViewModel::onEvent,
            onWorkspaceSelected,
            onNewWorkspace
        )

        4 -> JournalScreen(
            navController,
            states.journalState,
            states.settings,
            workspace,
            allWorkspaces,
            states.labelState.allLabels.filter { it.workspaceId==workspace.workspaceId },
            onShowSpaceBottomSheet,
            onSpaceChange,
            onAddCover,
            onRemoveCover,
            onDeleteWorkspace,
            onToggleLock,
            viewModels.journalViewModel::onEvent,
            onWorkspaceSelected,
            onNewWorkspace
        )

        5 -> HabitScreen(
            navController,
            states.habitState,
            states.settings,
            workspace,
            allWorkspaces,
            onShowSpaceBottomSheet,
            onSpaceChange,
            onAddCover,
            onRemoveCover,
            onDeleteWorkspace,
            onToggleLock,
            viewModels.habitViewModel::onEvent,
            onWorkspaceSelected,
            onNewWorkspace
        )

        6 -> AnalyticScreen(
            navController,
            states,
            workspace,
            allWorkspaces,
            workspace.selectedSpaces,
            onShowSpaceBottomSheet,
            onSpaceChange,
            onAddCover,
            onRemoveCover,
            onDeleteWorkspace,
            onToggleLock,
            onWorkspaceSelected,
            onNewWorkspace
        )

        7 -> ProgressTrackerScreen(
            navController,
            states.progressBoardState,
            states.settings,
            workspace,
            allWorkspaces,
            onShowSpaceBottomSheet,
            onSpaceChange,
            onAddCover,
            onRemoveCover,
            onDeleteWorkspace,
            onToggleLock,
            viewModels.progressBoardViewModel::onEvent,
            onWorkspaceSelected,
            onNewWorkspace
        )

        else -> {
            EmptyWorkspace(
                navController,
                workspace,
                allWorkspaces,
                onShowSpaceBottomSheet,
                onAddCover,
                onRemoveCover,
                onDeleteWorkspace,
                onToggleLock,
                onWorkspaceSelected,
                onNewWorkspace
            )
        }
    }
}
