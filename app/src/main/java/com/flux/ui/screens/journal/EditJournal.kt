package com.flux.ui.screens.journal

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastJoinToString
import androidx.navigation.NavController
import com.flux.data.model.JournalModel
import com.flux.other.Constants
import com.flux.other.HeaderNode
import com.flux.other.ensureStorageRoot
import com.flux.other.printPdf
import com.flux.other.shareJournal
import com.flux.ui.components.DeleteAlert
import com.flux.ui.components.JournalDetailsTopBar
import com.flux.ui.components.LinkDialog
import com.flux.ui.components.ListDialog
import com.flux.ui.components.MarkdownEditorRow
import com.flux.ui.components.NotesInfoBottomSheet
import com.flux.ui.components.OutlineBottomSheet
import com.flux.ui.components.ShareDialog
import com.flux.ui.components.TableDialog
import com.flux.ui.components.TaskDialog
import com.flux.ui.components.TaskItem
import com.flux.ui.components.convertMillisToDate
import com.flux.ui.components.convertMillisToTime
import com.flux.ui.events.JournalEvents
import com.flux.ui.screens.notes.FindAndReplaceField
import com.flux.ui.screens.notes.FindAndReplaceState
import com.flux.ui.screens.notes.ReadView
import com.flux.ui.screens.notes.StandardTextField
import com.flux.ui.screens.notes.onMarkdownKeyPressed
import com.flux.ui.state.TextState
import com.flux.ui.viewModel.JournalViewModel
import com.flux.ui.viewModel.SettingsViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditJournal(
    navController: NavController,
    journal: JournalModel,
    outline: HeaderNode,
    aboutJournal: TextState,
    isDarkMode: Boolean,
    isLintValid: Boolean,
    isLineNumbersVisible: Boolean,
    startWithReadView: Boolean,
    rootUri: String?,
    journalViewModel: JournalViewModel,
    settingsViewModel: SettingsViewModel,
    onJournalEvents: (JournalEvents) -> Unit
) {
    val isToday = LocalDate.now() == Instant.ofEpochMilli(journal.dateTime).atZone(ZoneId.systemDefault()).toLocalDate()
    val contentState = remember { TextFieldState(journal.text) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val hasContent = remember(journal.journalId) { journal.text.isNotBlank() }

    val pagerState = rememberPagerState(
        initialPage = if (startWithReadView && hasContent) 1 else 0,
        pageCount = { 2 }
    )
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showOutlineSheet by remember { mutableStateOf(false) }
    var selectedHeader by remember { mutableStateOf<IntRange?>(null) }
    var searchState by remember { mutableStateOf(FindAndReplaceState()) }
    var isSearching by remember { mutableStateOf(false) }
    var showAboutNotes by rememberSaveable { mutableStateOf(false) }
    var showLinkDialog by rememberSaveable { mutableStateOf(false) }
    var showTaskDialog by rememberSaveable { mutableStateOf(false) }
    var showTableDialog by rememberSaveable { mutableStateOf(false) }
    var showListDialog by rememberSaveable { mutableStateOf(false) }
    val isReadView by remember { derivedStateOf { pagerState.currentPage == 1 } }
    var readWebView by remember { mutableStateOf<WebView?>(null) }

    val rootPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val uri = result.data?.data ?: return@rememberLauncherForActivityResult
            settingsViewModel.saveRootUri(uri)
        }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris -> if (uris.isNotEmpty()) { onJournalEvents(JournalEvents.ImportImages(context, uris, contentState)) } }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) { onJournalEvents(JournalEvents.ImportVideo(context, uri, contentState)) } }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onJournalEvents(JournalEvents.ImportAudio(context, uri, contentState)) }
    }

    val renderedHtml by remember(contentState.text) {
        derivedStateOf {
            journalViewModel.renderMarkdown(contentState.text.toString())
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

    val onSaveJournal = {
        onJournalEvents(
            JournalEvents.UpsertEntry(
                journal.copy(
                    text = contentState.text.toString(),
                    dateTime = if (isToday) System.currentTimeMillis() else journal.dateTime
                )
            )
        )
    }

    BackHandler {
        onSaveJournal()
        focusManager.clearFocus()
        navController.popBackStack()
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            JournalDetailsTopBar(
                isReadView = isReadView,
                isSearching= isSearching,
                onBackPressed = {
                    onSaveJournal()
                    navController.popBackStack() },
                onOutlineClicked = {
                    onJournalEvents(JournalEvents.CalculateOutline(contentState.text.toString()))
                    showOutlineSheet=true },
                onReadClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                onEditClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                onSearchClick = { isSearching= !isSearching },
                onDelete = { showDeleteDialog=true },
                onAboutClicked = {
                    onJournalEvents(JournalEvents.CalculateTextState(contentState.text.toString()))
                    showAboutNotes=true },
                onShareNote = { showShareDialog=true },
                onSaveNote = { showSaveDialog=true },
                onPrintNote = { printPdf(context as Activity, readWebView, convertMillisToDate(journal.dateTime)+"_"+ convertMillisToTime(journal.dateTime)) }
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
                        ) { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } },
                    onAudioButtonClick = {
                        ensureStorageRoot(
                            scope = scope,
                            settingsViewModel = settingsViewModel,
                            rootPicker = rootPicker
                        ) { audioPickerLauncher.launch("audio/*") } },
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
        DeleteAlert({
            showDeleteDialog=false
        }, {
            onJournalEvents(JournalEvents.DeleteEntry(journal))
            navController.popBackStack()
            showDeleteDialog=false
        })
    }

    if(showShareDialog){
        ShareDialog( true, {
            shareJournal(context, it, convertMillisToDate(journal.dateTime)+"_"+ convertMillisToTime(journal.dateTime),contentState.text.toString(), journalViewModel, readWebView)
            showShareDialog = false
        }){ showShareDialog = false }
    }

    if(showSaveDialog){
        ShareDialog( false, {
            onJournalEvents(
                JournalEvents.ExportJournal(
                    context,
                    it,
                    convertMillisToDate(journal.dateTime)+"_"+ convertMillisToTime(journal.dateTime),
                    contentState.text.toString(),
                    readWebView
                ))
            showSaveDialog = false
        }){
            showSaveDialog = false
        }
    }

    if(showDeleteDialog){
        DeleteAlert({
            showDeleteDialog=false
        }, {
            onJournalEvents(JournalEvents.DeleteEntry(journal))
            navController.popBackStack()
            showDeleteDialog=false
        })
    }

    NotesInfoBottomSheet(
        isVisible = showAboutNotes,
        words = aboutJournal.wordCountWithPunctuation,
        characters = aboutJournal.charCount,
        lastEdited = convertMillisToDate(journal.dateTime),
        sheetState = sheetState,
        paragraph = aboutJournal.paragraphCount,
        wordsWithoutPunctuations = aboutJournal.wordCountWithoutPunctuation,
        lines = aboutJournal.lineCount
    ) { showAboutNotes=false }
}