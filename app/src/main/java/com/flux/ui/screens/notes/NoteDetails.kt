package com.flux.ui.screens.notes

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.LabelModel
import com.flux.data.model.NotesModel
import com.flux.navigation.NavRoutes
import com.flux.other.EditAction
import com.flux.ui.components.ActionType
import com.flux.ui.components.NoteDetailsTopBar
import com.flux.ui.components.NotesInputCard
import com.flux.ui.components.SelectLabelDialog
import com.flux.ui.components.SettingOption
import com.flux.ui.components.convertMillisToDate
import com.flux.ui.components.shapeManager
import com.flux.ui.events.NotesEvents
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalRichTextApi::class)
@Composable
fun NoteDetails(
    navController: NavController,
    workspaceId: String,
    note: NotesModel,
    allLabels: List<LabelModel>,
    onNotesEvents: (NotesEvents) -> Unit
) {
    var isPinned by rememberSaveable(note.notesId) { mutableStateOf(note.isPinned) }
    val actionHistory = remember { mutableStateListOf<EditAction>() }
    val redoHistory = remember { mutableStateListOf<EditAction>() }
    val lastHtml = remember { mutableStateOf(note.description) }
    var title by remember { mutableStateOf(note.title) }
    val richTextState = rememberRichTextState()
    val interactionSource = remember { MutableInteractionSource() }

    // Flag to prevent tracking changes during undo/redo operations
    val isUndoRedoOperation = remember { mutableStateOf(false) }

    // Track the last known title for proper undo/redo
    val lastTitle = remember { mutableStateOf(note.title) }

    // Only run once to set initial HTML and title
    LaunchedEffect(note.notesId) {
        richTextState.setHtml(note.description)
        lastHtml.value = note.description
        title = note.title
        lastTitle.value = note.title
    }

    // Track changes in description and update undo history
    LaunchedEffect(richTextState) {
        snapshotFlow { richTextState.annotatedString }
            .distinctUntilChanged()
            .collectLatest {
                // Skip tracking if this is an undo/redo operation
                if (isUndoRedoOperation.value) return@collectLatest

                val html = richTextState.toHtml()
                if (html != lastHtml.value) {
                    actionHistory.add(EditAction.DescriptionChanged(lastHtml.value, html))
                    lastHtml.value = html
                    redoHistory.clear()
                }
            }
    }

    val noteLabels = remember {
        mutableStateListOf<LabelModel>().apply {
            addAll(allLabels.filter { note.labels.contains(it.labelId) })
        }
    }

    var showSelectLabels by remember { mutableStateOf(false) }
    var showAboutNotes by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    NotesInfoBottomSheet(
        words = countWords("$title ${richTextState.toHtml()}"),
        characters = countCharacters("$title ${richTextState.toHtml()}"),
        lastEdited = convertMillisToDate(note.lastEdited),
        isVisible = showAboutNotes,
        sheetState = sheetState,
        onDismiss = {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) showAboutNotes = false
            }
        }
    )

    if (showSelectLabels) {
        SelectLabelDialog(
            noteLabels,
            allLabels,
            onConfirmation = {
                noteLabels.clear()
                noteLabels.addAll(it)
            },
            onDismissRequest = { showSelectLabels = false },
            onAddLabel = { navController.navigate(NavRoutes.EditLabels.withArgs(workspaceId)) }
        )
    }

    fun undo() {
        if (actionHistory.isEmpty()) return

        isUndoRedoOperation.value = true

        when (val last = actionHistory.removeAt(actionHistory.lastIndex)) {
            is EditAction.TitleChanged -> {
                redoHistory.add(EditAction.TitleChanged(last.old, last.new))
                title = last.old
                lastTitle.value = last.old
            }

            is EditAction.DescriptionChanged -> {
                redoHistory.add(EditAction.DescriptionChanged(last.old, last.new))
                richTextState.setHtml(last.old)
                lastHtml.value = last.old
            }
        }

        // Small delay to ensure state updates are processed
        scope.launch {
            delay(50)
            isUndoRedoOperation.value = false
        }
    }

    fun redo() {
        if (redoHistory.isEmpty()) return

        isUndoRedoOperation.value = true

        when (val next = redoHistory.removeAt(redoHistory.lastIndex)) {
            is EditAction.TitleChanged -> {
                title = next.new
                lastTitle.value = next.new
                actionHistory.add(EditAction.TitleChanged(next.old, next.new))
            }

            is EditAction.DescriptionChanged -> {
                richTextState.setHtml(next.new)
                lastHtml.value = next.new
                actionHistory.add(EditAction.DescriptionChanged(next.old, next.new))
            }
        }

        // Small delay to ensure state updates are processed
        scope.launch {
            delay(50)
            isUndoRedoOperation.value = false
        }
    }

    val onSaveNote = {
        onNotesEvents(
            NotesEvents.UpsertNote(
                note.copy(
                    title = title,
                    description = richTextState.toHtml(),
                    isPinned = isPinned,
                    lastEdited = System.currentTimeMillis(),
                    labels = noteLabels.map { it.labelId }
                )
            )
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            NoteDetailsTopBar(
                canUndo = actionHistory.isNotEmpty(),
                canRedo = redoHistory.isNotEmpty(),
                isPinned = isPinned,
                onBackPressed = { navController.popBackStack() },
                onDone = { onSaveNote() },
                onTogglePinned = { isPinned = !isPinned },
                onDelete = {
                    onNotesEvents(NotesEvents.DeleteNote(note))
                    navController.popBackStack()
                },
                onAddLabel = { showSelectLabels = true },
                onAboutClicked = { showAboutNotes = true },
                onUndo = ::undo,
                onRedo = ::redo
            )
        }
    ) { innerPadding ->
        NotesInputCard(
            innerPadding = innerPadding,
            title = title,
            allLabels = noteLabels,
            richTextState = richTextState,
            interactionSource = interactionSource,
            onTitleChange = { new ->
                if (new != title && !isUndoRedoOperation.value) {
                    actionHistory.add(EditAction.TitleChanged(lastTitle.value, new))
                    title = new
                    lastTitle.value = new
                    redoHistory.clear()
                }
            },
            onLabelClicked = { showSelectLabels = true }
        )
    }
}

fun countWords(text: String): Int {
    return text.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
}

fun countCharacters(text: String, includeSpaces: Boolean = true): Int {
    return if (includeSpaces) text.length else text.count { !it.isWhitespace() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesInfoBottomSheet(
    words: Int,
    characters: Int,
    lastEdited: String,
    isVisible: Boolean,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                item {
                    SettingOption(
                        radius = shapeManager(isFirst = true, radius = 32),
                        icon = Icons.Default.Edit,
                        title = stringResource(R.string.Last_Edited),
                        description = lastEdited,
                        actionType = ActionType.None
                    )
                }

                item {
                    SettingOption(
                        radius = shapeManager(radius = 32),
                        icon = Icons.Default.Numbers,
                        title = stringResource(R.string.Word_Count),
                        description = words.toString(),
                        actionType = ActionType.None
                    )
                }

                item {
                    SettingOption(
                        radius = shapeManager(radius = 32, isLast = true),
                        icon = Icons.Default.Abc,
                        title = stringResource(R.string.Character_Count),
                        description = characters.toString(),
                        actionType = ActionType.None
                    )
                }
            }
        }
    }
}
