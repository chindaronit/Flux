package com.flux.ui.screens.notes

import android.app.Activity
import android.net.Uri
import android.webkit.WebView
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastJoinToString
import androidx.navigation.NavController
import com.flux.data.model.LabelModel
import com.flux.data.model.NotesModel
import com.flux.navigation.NavRoutes
import com.flux.other.Constants
import com.flux.other.HeaderNode
import com.flux.other.ensureStorageRoot
import com.flux.other.printPdf
import com.flux.other.shareNote
import com.flux.ui.components.DeleteAlert
import com.flux.ui.components.LinkDialog
import com.flux.ui.components.ListDialog
import com.flux.ui.components.MarkdownEditorRow
import com.flux.ui.components.NoteDetailsTopBar
import com.flux.ui.components.NotesInfoBottomSheet
import com.flux.ui.components.OutlineBottomSheet
import com.flux.ui.components.SelectLabelDialog
import com.flux.ui.components.ShareDialog
import com.flux.ui.components.TableDialog
import com.flux.ui.components.TaskDialog
import com.flux.ui.components.TaskItem
import com.flux.ui.components.convertMillisToDate
import com.flux.ui.events.NotesEvents
import com.flux.ui.state.TextState
import com.flux.ui.viewModel.NotesViewModel
import com.flux.ui.viewModel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteDetails(
    navController: NavController,
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
    onNotesEvents: (NotesEvents) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val hasContent = remember(note.notesId) {
        note.title.isNotBlank() || note.description.isNotBlank()
    }

    val pagerState = rememberPagerState(
        initialPage = if (startWithReadView && hasContent) 1 else 0,
        pageCount = { 2 }
    )

    val titleState = remember { TextFieldState(note.title) }
    val contentState = remember { TextFieldState(note.description) }
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
    var showSelectLabels by rememberSaveable { mutableStateOf(false) }
    var isPinned by rememberSaveable(note.notesId) { mutableStateOf(note.isPinned) }
    val noteLabels = rememberSaveable { mutableStateListOf<LabelModel>().apply { addAll(allLabels.filter { note.labels.contains(it.labelId) }) } }
    val isReadView by remember { derivedStateOf { pagerState.currentPage == 1 } }
    var readWebView by remember { mutableStateOf<WebView?>(null) }

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

    LaunchedEffect(startWithReadView, hasContent) {
        pagerState.scrollToPage(
            if (startWithReadView && hasContent) 1 else 0
        )
    }

    fun onSaveNote() {

        val newTitle = titleState.text.toString()
        val newDescription = contentState.text.toString()

        if (newTitle == note.title && newDescription == note.description) return

        onNotesEvents(
            NotesEvents.UpsertNote(
                note.copy(
                    title = titleState.text.toString(),
                    description = contentState.text.toString(),
                    isPinned = isPinned,
                    lastEdited = System.currentTimeMillis(),
                    labels = noteLabels.map { it.labelId }
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
                onSaveNote = { showSaveNotesDialog=true },
                onPrintNote = { printPdf(context as Activity, readWebView, titleState.text.toString()) }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
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
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
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
                                        text = "Title",
                                        style = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                    )
                                },
                                contentPadding = PaddingValues(0.dp),
                                container = {}
                            )
                        }
                    )

                    if (noteLabels.isNotEmpty()) {
                        LazyRow(modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(noteLabels) { label ->
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
        SelectLabelDialog(noteLabels, allLabels,
            onConfirmation = {
                noteLabels.clear()
                noteLabels.addAll(it)
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

    NotesInfoBottomSheet(
        isVisible = showAboutNotes,
        words = aboutNotes.wordCountWithPunctuation,
        characters = aboutNotes.charCount,
        lastEdited = convertMillisToDate(note.lastEdited),
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