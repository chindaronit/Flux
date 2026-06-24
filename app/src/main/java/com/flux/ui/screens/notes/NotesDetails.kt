package com.flux.ui.screens.notes

import android.app.Activity
import android.net.Uri
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LabelImportant
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastJoinToString
import androidx.navigation.NavController
import com.flux.R
import com.flux.data.model.JournalModel
import com.flux.data.model.LabelModel
import com.flux.data.model.NotesModel
import com.flux.data.model.TodoItem
import com.flux.data.model.TodoModel
import com.flux.data.model.WorkspaceModel
import com.flux.navigation.NavRoutes
import com.flux.other.Constants
import com.flux.other.HeaderNode
import com.flux.other.ensureStorageRoot
import com.flux.other.printPdf
import com.flux.other.shareNote
import com.flux.ui.common.DeleteAlert
import com.flux.ui.common.NoteDetailsTopBar
import com.flux.ui.common.convertMillisToDate
import com.flux.ui.events.NotesEvents
import com.flux.ui.state.TextState
import com.flux.ui.viewModel.NotesViewModel
import com.flux.ui.viewModel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.flux.other.AudioRecorder
import com.flux.other.ConvertType
import com.flux.other.DataCopyType
import com.flux.ui.common.DataCopyDialog
import com.flux.ui.common.convertMillisToTime
import com.flux.ui.events.JournalEvents
import com.flux.ui.events.TodoEvents
import com.flux.ui.events.WorkspaceEvents

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteDetails(
    navController: NavController,
    workspaces: List<WorkspaceModel>,
    outline: HeaderNode,
    aboutNotes: TextState,
    workspaceId: String,
    isDarkMode: Boolean,
    isLintValid: Boolean,
    isLineNumbersVisible: Boolean,
    startWithReadView: Boolean,
    note: NotesModel,
    rootUri: String?,
    allLabels: List<LabelModel>,
    settingsViewModel: SettingsViewModel,
    notesViewModel: NotesViewModel,
    onNotesEvents: (NotesEvents) -> Unit,
    onJournalEvents: (JournalEvents) -> Unit,
    onTodoEvents: (TodoEvents) -> Unit,
    onWorkspaceEvents: (WorkspaceEvents) -> Unit
) {
    val textFieldStateSaver = Saver<TextFieldState, String>(
        save = { it.text.toString() },
        restore = { TextFieldState(it) }
    )
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val hasContent = remember(note.notesId) { note.title.isNotBlank() || note.description.isNotBlank() }
    val recorder = AudioRecorder(context)
    val pagerState = rememberPagerState(
        initialPage = if (startWithReadView && hasContent) 1 else 0,
        pageCount = { 2 }
    )
    val titleState = rememberSaveable(saver = textFieldStateSaver) { TextFieldState(note.title) }
    val contentState = rememberSaveable(saver = textFieldStateSaver) { TextFieldState(note.description) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showShareNotesDialog by remember { mutableStateOf(false) }
    var showSaveNotesDialog by remember { mutableStateOf(false) }
    var showOutlineSheet by remember { mutableStateOf(false) }
    var selectedHeader by remember { mutableStateOf<IntRange?>(null) }
    var searchState by remember { mutableStateOf(FindAndReplaceState()) }
    var isSearching by remember { mutableStateOf(false) }
    var showAboutNotes by rememberSaveable { mutableStateOf(false) }
    var showLinkDialog by rememberSaveable { mutableStateOf(false) }
    var showTaskDialog by rememberSaveable { mutableStateOf(false) }
    var showTableDialog by rememberSaveable { mutableStateOf(false) }
    var showListDialog by rememberSaveable { mutableStateOf(false) }
    var showAudioRecorder by rememberSaveable { mutableStateOf(false) }
    var showSelectLabels by rememberSaveable { mutableStateOf(false) }
    var showDataCopyDialog by remember { mutableStateOf(false) }
    var showConvertDialog by remember { mutableStateOf(false) }
    var isPinned by rememberSaveable(note.notesId) { mutableStateOf(note.isPinned) }
    val noteLabelIds = rememberSaveable {
        mutableStateListOf<String>().apply {
            addAll(note.labels)
        }
    }
    val currentWorkspace = workspaces.find { it.workspaceId == workspaceId }
    val isReadView by remember { derivedStateOf { pagerState.currentPage == 1 } }
    var readWebView by remember { mutableStateOf<WebView?>(null) }
    val cloneString = stringResource(R.string.clone_created_successfully)
    val contentCopiedString = stringResource(R.string.content_copied)
    val contentMovedString = stringResource(R.string.content_moved)
    val successString = stringResource(R.string.success)

    LaunchedEffect(searchState.searchWord, contentState.text) {
        withContext(Dispatchers.Default) {
            searchState = searchState.copy(
                matchCount = if (searchState.searchWord.isNotBlank())
                    Regex.escape(searchState.searchWord).toRegex()
                        .findAll(contentState.text)
                        .count()
                else 0
            )
        }
    }

    val rootPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val uri = result.data?.data ?: return@rememberLauncherForActivityResult
            settingsViewModel.saveRootUri(uri)
        }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris -> if (uris.isNotEmpty()) { onNotesEvents(NotesEvents.ImportImages(context, uris, contentState)) } }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) { onNotesEvents(NotesEvents.ImportVideo(context, uri, contentState)) } }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            onNotesEvents(NotesEvents.ImportAudio(context, uri, contentState))
        }
    }

    val renderedHtml by remember(contentState.text) {
        derivedStateOf {
            notesViewModel.renderMarkdown(contentState.text.toString())
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isReadView) {
        keyboardController?.hide()
        focusManager.clearFocus()
        isSearching = false
    }

    fun onSaveNote() {
        val newTitle = titleState.text.toString()
        val newDescription = contentState.text.toString()

        if (newTitle == note.title && newDescription == note.description && noteLabelIds.toList()==note.labels) return

        onNotesEvents(
            NotesEvents.UpsertNote(
                note.copy(
                    title = titleState.text.toString(),
                    description = contentState.text.toString(),
                    isPinned = isPinned,
                    lastEdited = System.currentTimeMillis(),
                    labels = noteLabelIds.toList()
                )
            )
        )
    }

    BackHandler {
        focusManager.clearFocus()
        onSaveNote()
        navController.popBackStack()
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            NoteDetailsTopBar(
                isPinned = isPinned,
                isReadView = isReadView,
                isSearching= isSearching,
                onBackPressed = {
                    onSaveNote()
                    navController.popBackStack() },
                onOutlineClicked = {
                    onNotesEvents(NotesEvents.CalculateOutline(contentState.text.toString()))
                    showOutlineSheet=true },
                onReadClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                onEditClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                onSearchClick = { isSearching= !isSearching },
                onTogglePinned = { isPinned = !isPinned },
                onDelete = { showDeleteDialog=true },
                onAddLabel = { showSelectLabels = true },
                onAboutClicked = {
                    onNotesEvents(NotesEvents.CalculateTextState(contentState.text.toString()))
                    showAboutNotes=true },
                onShareNote = { showShareNotesDialog=true },
                onSaveNote = {
                    ensureStorageRoot(
                        scope = scope,
                        settingsViewModel = settingsViewModel,
                        rootPicker = rootPicker
                    ) { showSaveNotesDialog = true } },
                onPrintNote = { printPdf(context as Activity, readWebView, titleState.text.toString()) },
                onCloneNote = {
                    onNotesEvents(NotesEvents.UpsertNote(NotesModel(title = "Clone ${titleState.text}", description = contentState.text.toString(), workspaceId = workspaceId, labels = noteLabelIds)))
                    Toast.makeText(context, cloneString, Toast.LENGTH_SHORT).show()
                },
                onConvertNote = { showConvertDialog = true },
                onCopyNote = { showDataCopyDialog = true }
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = !isReadView,
                enter = slideInVertically { fullHeight -> fullHeight },
                exit = slideOutVertically { fullHeight -> fullHeight }) {
                MarkdownEditorRow(
                    canRedo = contentState.undoState.canRedo,
                    canUndo = contentState.undoState.canUndo,
                    onEdit = { onMarkdownKeyPressed(it, contentState, null) },
                    onTableButtonClick = { showTableDialog = true },
                    onListButtonClick = { showListDialog = true },
                    onTaskButtonClick = { showTaskDialog = true },
                    onLinkButtonClick = { showLinkDialog = true },
                    onRecordAudioClick = {
                        ensureStorageRoot(
                            scope = scope,
                            settingsViewModel = settingsViewModel,
                            rootPicker = rootPicker
                        ) {
                            showAudioRecorder=true
                        }
                    },
                    onImageButtonClick = {
                        ensureStorageRoot(
                            scope = scope,
                            settingsViewModel = settingsViewModel,
                            rootPicker = rootPicker
                        ) { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    },
                    onAudioButtonClick = {
                        ensureStorageRoot(
                            scope = scope,
                            settingsViewModel = settingsViewModel,
                            rootPicker = rootPicker
                        ) { audioPickerLauncher.launch("audio/*") }
                    },
                    onVideoButtonClick = {
                        ensureStorageRoot(
                            scope = scope,
                            settingsViewModel = settingsViewModel,
                            rootPicker = rootPicker
                        ) { videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            AnimatedContent(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                targetState = isSearching,
                contentAlignment = Alignment.TopCenter
            ) {
                Column {
                    if (it) FindAndReplaceField(
                        state = searchState,
                        onStateUpdate = { state -> searchState = state })
                    else BasicTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        state = titleState,
                        readOnly = isReadView,
                        lineLimits = TextFieldLineLimits.SingleLine,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                        decorator = { innerTextField ->
                            TextFieldDefaults.DecorationBox(
                                value = titleState.text.toString(),
                                innerTextField = innerTextField,
                                enabled = true,
                                singleLine = true,
                                visualTransformation = VisualTransformation.None,
                                interactionSource = remember { MutableInteractionSource() },
                                placeholder = {
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = stringResource(R.string.Title),
                                        style = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                    )
                                },
                                contentPadding = PaddingValues(0.dp),
                                container = {}
                            )
                        }
                    )

                    if (noteLabelIds.isNotEmpty()) {
                        LazyRow(modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(allLabels.filter { l-> noteLabelIds.contains(l.labelId) }) { label ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .clickable { showSelectLabels=true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Default.LabelImportant,
                                            contentDescription = "Label",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            label.value,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            /*-------------------------------------------------*/
            val scrollState = rememberScrollState()
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                userScrollEnabled = false
            ) { currentPage: Int ->
                when (currentPage) {
                    0 -> {
                        StandardTextField(
                            modifier = Modifier.fillMaxSize(),
                            state = contentState,
                            readMode = isReadView,
                            showLineNumbers = isLineNumbersVisible,
                            scrollState = scrollState,
                            isLintActive = isLintValid,
                            headerRange = selectedHeader,
                            findAndReplaceState = searchState,
                            onFindAndReplaceUpdate = { searchState = it }
                        )
                    }

                    1 -> {
                        ReadView(
                            modifier = Modifier.fillMaxSize(),
                            html = renderedHtml,
                            scrollState = scrollState,
                            rootUri = rootUri,
                            isAppInDarkMode = isDarkMode,
                            onWebViewReady = { readWebView = it }
                        )
                    }
                }
            }
        }
    }

    OutlineBottomSheet(showOutlineSheet, outline, sheetState, onHeaderClick = { selectedHeader = it } ) { showOutlineSheet=false }

    if (showLinkDialog) {
        LinkDialog(onDismissRequest = { showLinkDialog = false }) { linkName, linkUri ->
            val insertText = "[${linkName}](${linkUri})"
            onMarkdownKeyPressed(Constants.Editor.TEXT, contentState, insertText)
        }
    }

    if (showTableDialog) {
        TableDialog(onDismissRequest = { showTableDialog = false }) { row, column ->
            onMarkdownKeyPressed(Constants.Editor.TABLE, contentState, "$row,$column")
        }
    }

    if (showListDialog) {
        ListDialog(onDismissRequest = { showListDialog = false }) {
            onMarkdownKeyPressed(Constants.Editor.LIST, contentState, it.fastJoinToString(separator = "\n"))
        }
    }

    if (showTaskDialog) {
        TaskDialog(onDismissRequest = { showTaskDialog = false }) {
            onMarkdownKeyPressed(Constants.Editor.TASK, contentState, Json.encodeToString<List<TaskItem>>(it))
        }
    }

    if(showDeleteDialog){
        DeleteAlert(onConfirmation = {
            onNotesEvents(NotesEvents.DeleteNote(note))
            navController.popBackStack()
            showDeleteDialog=false
        }, onDismissRequest = { showDeleteDialog=false })
    }

    if (showSelectLabels) {
        SelectLabelDialog(noteLabelIds, allLabels,
            onConfirmation = {
                noteLabelIds.clear()
                noteLabelIds.addAll(it)
            },
            onDismissRequest = { showSelectLabels = false },
            onAddLabel = { navController.navigate(NavRoutes.EditLabels.withArgs(workspaceId)) }
        )
    }

    if(showShareNotesDialog){
        ShareDialog( true, {
            shareNote(
                context = context,
                exportType = it,
                noteTitle = titleState.text.toString(),
                noteDescription = contentState.text.toString(),
                notesViewModel = notesViewModel,
                readWebView = readWebView
            )
            showShareNotesDialog = false
        }){ showShareNotesDialog = false }
    }

    if(showSaveNotesDialog){
        ShareDialog( false, {
            onNotesEvents(NotesEvents.ExportNote(context, it, titleState.text.toString(), contentState.text.toString(), readWebView))
            showSaveNotesDialog = false
        }){
            showSaveNotesDialog = false
        }
    }

    if(showDataCopyDialog){
        DataCopyDialog(
            workspaces.filterNot { it.workspaceId == workspaceId },
            { dataCopyType, selectedWorkspaces ->
                if(selectedWorkspaces.isEmpty()) return@DataCopyDialog
                when(dataCopyType){
                    DataCopyType.COPY -> {
                        selectedWorkspaces.forEach { workspace ->
                            if(!workspace.selectedSpaces.contains(1)){
                                onWorkspaceEvents(WorkspaceEvents.UpsertSpace(workspace.copy(selectedSpaces = workspace.selectedSpaces + 1)))
                            }
                            onNotesEvents(NotesEvents.UpsertNote(NotesModel(title = titleState.text.toString(), description = contentState.text.toString(), workspaceId = workspace.workspaceId)))
                        }

                        Toast.makeText(context, contentCopiedString, Toast.LENGTH_SHORT).show()
                    }
                    DataCopyType.MOVE -> {
                        selectedWorkspaces.forEach { workspace ->
                            if(!workspace.selectedSpaces.contains(1)){
                                onWorkspaceEvents(WorkspaceEvents.UpsertSpace(workspace.copy(selectedSpaces = workspace.selectedSpaces + 1)))
                            }
                            onNotesEvents(NotesEvents.UpsertNote(NotesModel(title = titleState.text.toString(), description = contentState.text.toString(), workspaceId = workspace.workspaceId)))
                        }

                        navController.popBackStack()
                        Toast.makeText(context, contentMovedString, Toast.LENGTH_SHORT).show()
                        onNotesEvents(NotesEvents.DeleteNote(note))
                    }
                }
            }
        ) { showDataCopyDialog = false }
    }

    if(showAudioRecorder){
        RecordAudioDialog(context, recorder, {onNotesEvents(NotesEvents.ImportAudio(context, it!!, contentState))}) {
            showAudioRecorder=false
        }
    }

    if(showConvertDialog){
        ConvertNotesDialog ({ type ->
            when(type){
                ConvertType.TODO -> {
                    val todo = TodoModel(
                        workspaceId = note.workspaceId,
                        title = titleState.text.toString(),
                        items = contentState.text.toString()
                            .lineSequence()
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .map { line ->
                                TodoItem(
                                    value = line
                                )
                            }
                            .toList()
                    )
                    if(!currentWorkspace!!.selectedSpaces.contains(2)){
                        onWorkspaceEvents(WorkspaceEvents.UpsertSpace(currentWorkspace.copy(selectedSpaces = currentWorkspace.selectedSpaces + 2)))
                    }
                    onTodoEvents(TodoEvents.UpsertList(context, false, todo))

                    navController.popBackStack()
                    onNotesEvents(NotesEvents.DeleteNote(note))
                }
                ConvertType.JOURNAL -> {
                    if(!currentWorkspace!!.selectedSpaces.contains(4)){
                        onWorkspaceEvents(WorkspaceEvents.UpsertSpace(currentWorkspace.copy(selectedSpaces = currentWorkspace.selectedSpaces + 4)))
                    }
                    onJournalEvents(JournalEvents.UpsertEntry(JournalModel(text = contentState.text.toString(), workspaceId = workspaceId, labels = noteLabelIds)))

                    navController.popBackStack()
                    onNotesEvents(NotesEvents.DeleteNote(note))
                }
                else -> {}
            }

            Toast.makeText(context, successString, Toast.LENGTH_SHORT).show()
            showConvertDialog=false
        }) {
            showConvertDialog=false
        }
    }

    NotesInfoBottomSheet(
        isVisible = showAboutNotes,
        words = aboutNotes.wordCountWithPunctuation,
        characters = aboutNotes.charCount,
        lastEdited = "${convertMillisToDate(note.lastEdited)}, ${convertMillisToTime(note.lastEdited)}",
        sheetState = sheetState,
        paragraph = aboutNotes.paragraphCount,
        wordsWithoutPunctuations = aboutNotes.wordCountWithoutPunctuation,
        lines = aboutNotes.lineCount
        ) { showAboutNotes=false }

}

@OptIn(ExperimentalFoundationApi::class)
fun onMarkdownKeyPressed(key: String, contentState: TextFieldState, value: String?) {
    return when (key) {
        Constants.Editor.UNDO -> contentState.undoState.undo()
        Constants.Editor.REDO -> contentState.undoState.redo()
        Constants.Editor.H1 -> contentState.edit { addHeader(1) }
        Constants.Editor.H2 -> contentState.edit { addHeader(2) }
        Constants.Editor.H3 -> contentState.edit { addHeader(3) }
        Constants.Editor.H4 -> contentState.edit { addHeader(4) }
        Constants.Editor.H5 -> contentState.edit { addHeader(5) }
        Constants.Editor.H6 -> contentState.edit { addHeader(6) }
        Constants.Editor.BOLD -> contentState.edit { bold() }
        Constants.Editor.ITALIC -> contentState.edit { italic() }
        Constants.Editor.UNDERLINE -> contentState.edit { underline() }
        Constants.Editor.STRIKETHROUGH -> contentState.edit { strikeThrough() }
        Constants.Editor.MARK -> contentState.edit { highlight() }
        Constants.Editor.INLINE_CODE -> contentState.edit { inlineCode() }
        Constants.Editor.INLINE_BRACKETS -> contentState.edit { inlineBrackets() }
        Constants.Editor.INLINE_BRACES -> contentState.edit { inlineBraces() }
        Constants.Editor.INLINE_MATH -> contentState.edit { inlineMath() }
        Constants.Editor.QUOTE -> contentState.edit { quote() }
        Constants.Editor.NOTE -> contentState.edit { alert(key.uppercase()) }
        Constants.Editor.TIP -> contentState.edit { alert(key.uppercase()) }
        Constants.Editor.IMPORTANT -> contentState.edit { alert(key.uppercase()) }
        Constants.Editor.WARNING -> contentState.edit { alert(key.uppercase()) }
        Constants.Editor.CAUTION -> contentState.edit { alert(key.uppercase()) }
        Constants.Editor.TAB -> contentState.edit { tab() }
        Constants.Editor.UN_TAB -> contentState.edit { unTab() }
        Constants.Editor.RULE -> contentState.edit { addRule() }
        Constants.Editor.DIAGRAM -> contentState.edit { addMermaid() }
        Constants.Editor.TEXT -> contentState.edit { add(value!!) }
        Constants.Editor.TABLE -> contentState.edit { addTable(value!!.substringBefore(",", "1").toInt(), value.substringAfter(",", "1").toInt()) }
        Constants.Editor.TASK -> {
            val taskList = Json.decodeFromString<List<TaskItem>>(value!!)
            taskList.forEach { contentState.edit { addTask(it.task, it.checked) } }
        }
        Constants.Editor.LIST -> contentState.edit { addInNewLine(value!!) }
        else -> {}
    }
}