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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.flux.other.ensureStorageRoot
import com.flux.other.printPdf
import com.flux.other.shareNote
import com.flux.ui.components.DeleteAlert
import com.flux.ui.components.LinkDialog
import com.flux.ui.components.ListDialog
import com.flux.ui.components.MarkdownEditorRow
import com.flux.ui.components.NoteDetailsTopBar
import com.flux.ui.components.SelectLabelDialog
import com.flux.ui.components.ShareNoteDialog
import com.flux.ui.components.TableDialog
import com.flux.ui.components.TaskDialog
import com.flux.ui.components.TaskItem
import com.flux.ui.events.NotesEvents
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
    workspaceId: String,
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
    val pagerState = rememberPagerState(pageCount = { 2 })
    val titleState = remember { TextFieldState(note.title) }
    val contentState = remember { TextFieldState(note.description) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showShareNotesDialog by remember { mutableStateOf(false) }
    var showSaveNotesDialog by remember { mutableStateOf(false) }
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
    val pickedImages = rememberSaveable { mutableStateListOf<String>().apply { addAll(note.images) } }
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

    val onSaveNote = {
        onNotesEvents(
            NotesEvents.UpsertNote(
                note.copy(
                    title = titleState.text.toString(),
                    description = contentState.text.toString(),
                    isPinned = isPinned,
                    lastEdited = System.currentTimeMillis(),
                    labels = noteLabels.map { it.labelId },
                    images = pickedImages.toList()
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
                onBackPressed = {
                    onSaveNote()
                    navController.popBackStack() },
                onReadClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                onEditClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                onTogglePinned = { isPinned = !isPinned },
                onDelete = { showDeleteDialog=true },
                onAddLabel = { showSelectLabels = true },
                onAboutClicked = { },
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
                        ) {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    },
                    onAudioButtonClick = { audioPickerLauncher.launch("audio/*") },
                    onVideoButtonClick = {
                        ensureStorageRoot(
                            scope = scope,
                            settingsViewModel = settingsViewModel,
                            rootPicker = rootPicker
                        ) {
                            videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AnimatedContent(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                targetState = isSearching,
                contentAlignment = Alignment.TopCenter
            ) {
                if (it) FindAndReplaceField(
                    state = searchState,
                    onStateUpdate = { state -> searchState = state })
                else BasicTextField(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
                            showLineNumbers = true,
                            scrollState = scrollState,
                            isLintActive = true,
                            headerRange = selectedHeader,
                            findAndReplaceState = searchState,
                            onFindAndReplaceUpdate = { searchState = it },
                            onImageReceived = {}
                        )
                    }

                    1 -> {
                        ReadView(
                            modifier = Modifier.fillMaxSize(),
                            html = renderedHtml,
                            scrollState = scrollState,
                            rootUri = rootUri,
                            isAppInDarkMode = false,
                            onWebViewReady = { readWebView = it }
                        )
                    }
                }
            }
        }
    }

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
        ShareNoteDialog( true, {
            shareNote(
                context = context,
                exportType = it,
                noteTitle = titleState.text.toString(),
                noteDescription = contentState.text.toString(),
                notesViewModel = notesViewModel,
                readWebView = readWebView
            )
            showShareNotesDialog = false
        }){
            showShareNotesDialog = false
        }
    }

    if(showSaveNotesDialog){
        ShareNoteDialog( false, {
            onNotesEvents(NotesEvents.ExportNote(context, it, titleState.text.toString(), contentState.text.toString(), readWebView))
            showSaveNotesDialog = false
        }){
            showSaveNotesDialog = false
        }
    }
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

//
//fun countWords(text: String): Int {
//    return text.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
//}
//
//fun countCharacters(text: String, includeSpaces: Boolean = true): Int {
//    return if (includeSpaces) text.length else text.count { !it.isWhitespace() }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun NotesInfoBottomSheet(
//    words: Int,
//    characters: Int,
//    lastEdited: String,
//    isVisible: Boolean,
//    sheetState: SheetState,
//    onDismiss: () -> Unit
//) {
//    if (isVisible) {
//        ModalBottomSheet(
//            onDismissRequest = onDismiss,
//            sheetState = sheetState,
//            containerColor = MaterialTheme.colorScheme.surfaceContainer
//        ) {
//            LazyColumn(
//                Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp)
//            ) {
//                item {
//                    SettingOption(
//                        radius = shapeManager(isFirst = true, radius = 32),
//                        icon = Icons.Default.Edit,
//                        title = stringResource(R.string.Last_Edited),
//                        description = lastEdited,
//                        actionType = ActionType.None
//                    )
//                }
//
//                item {
//                    SettingOption(
//                        radius = shapeManager(radius = 32),
//                        icon = Icons.Default.Numbers,
//                        title = stringResource(R.string.Word_Count),
//                        description = words.toString(),
//                        actionType = ActionType.None
//                    )
//                }
//
//                item {
//                    SettingOption(
//                        radius = shapeManager(radius = 32, isLast = true),
//                        icon = Icons.Default.Abc,
//                        title = stringResource(R.string.Character_Count),
//                        description = characters.toString(),
//                        actionType = ActionType.None
//                    )
//                }
//            }
//        }
//    }
//}
