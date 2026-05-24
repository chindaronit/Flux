package com.flux.ui.screens.journal

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LabelImportant
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastJoinToString
import androidx.navigation.NavController
import com.flux.data.model.JournalModel
import com.flux.other.AudioRecorder
import com.flux.other.Constants
import com.flux.other.HeaderNode
import com.flux.other.ensureStorageRoot
import com.flux.other.printPdf
import com.flux.other.shareJournal
import com.flux.ui.common.DeleteAlert
import com.flux.ui.common.JournalDetailsTopBar
import com.flux.ui.common.convertMillisToDate
import com.flux.ui.common.convertMillisToTime
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.flux.R
import com.flux.data.model.LabelModel
import com.flux.navigation.NavRoutes
import com.flux.ui.common.DatePickerModal
import com.flux.ui.screens.notes.LinkDialog
import com.flux.ui.screens.notes.ListDialog
import com.flux.ui.screens.notes.MarkdownEditorRow
import com.flux.ui.screens.notes.NotesInfoBottomSheet
import com.flux.ui.screens.notes.OutlineBottomSheet
import com.flux.ui.screens.notes.RecordAudioDialog
import com.flux.ui.screens.notes.SelectLabelDialog
import com.flux.ui.screens.notes.ShareDialog
import com.flux.ui.screens.notes.TableDialog
import com.flux.ui.screens.notes.TaskDialog
import com.flux.ui.screens.notes.TaskItem

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
    allLabels: List<LabelModel>,
    journalViewModel: JournalViewModel,
    settingsViewModel: SettingsViewModel,
    onJournalEvents: (JournalEvents) -> Unit
) {
    val textFieldStateSaver = Saver<TextFieldState, String>(
        save = { it.text.toString() },
        restore = { TextFieldState(it) }
    )
    var isToday by remember { mutableStateOf(LocalDate.now() == Instant.ofEpochMilli(journal.dateTime).atZone(ZoneId.systemDefault()).toLocalDate()) }
    var journalDate by remember { mutableLongStateOf(journal.dateTime) }
    val contentState = rememberSaveable(saver = textFieldStateSaver) { TextFieldState(journal.text) }
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
    var showDateDialog by rememberSaveable { mutableStateOf(false) }
    var showAboutNotes by rememberSaveable { mutableStateOf(false) }
    var showLinkDialog by rememberSaveable { mutableStateOf(false) }
    var showTaskDialog by rememberSaveable { mutableStateOf(false) }
    var showTableDialog by rememberSaveable { mutableStateOf(false) }
    var showListDialog by rememberSaveable { mutableStateOf(false) }
    val isReadView by remember { derivedStateOf { pagerState.currentPage == 1 } }
    var showAudioRecorder by rememberSaveable { mutableStateOf(false) }
    var readWebView by remember { mutableStateOf<WebView?>(null) }
    var showLabelDialog by rememberSaveable { mutableStateOf(false) }
    val currentLabelIds = rememberSaveable {
        mutableStateListOf<String>().apply {
            addAll(journal.labels)
        }
    }
    val recorder = AudioRecorder(context)

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
    val futureDateNotAllowed = stringResource(R.string.future_journal_entries_not_allowed)

    LaunchedEffect(isReadView) {
        keyboardController?.hide()
        focusManager.clearFocus()
        isSearching = false
    }

    LaunchedEffect(journalDate) {
        isToday = LocalDate.now() == Instant.ofEpochMilli(journalDate).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    fun onSaveJournal() {
        val newText = contentState.text.toString()

        val hasChanged = newText != journal.text

        if (!hasChanged && currentLabelIds.toList()==journal.labels) return

        onJournalEvents(
            JournalEvents.UpsertEntry(
                journal.copy(
                    text = newText,
                    dateTime = if (isToday) System.currentTimeMillis() else journalDate,
                    labels = currentLabelIds.toList()
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
                onShareNote = { showShareDialog = true },
                onSaveNote = { showSaveDialog = true },
                onAddLabel = { showLabelDialog = true },
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
                }
            }

            LazyRow(
                modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { showDateDialog=true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                convertMillisToDate(journalDate),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
                items(allLabels.filter { l -> currentLabelIds.contains(l.labelId) }) { label ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { showLabelDialog = true }
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

            /*-------------------------------------------------*/
            val scrollState = rememberScrollState()
            HorizontalPager(
                modifier = Modifier.padding(top=2.dp),
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

    if(showDateDialog){
        DatePickerModal(
            onDateSelected = { selectedDate ->

                if (selectedDate == null) return@DatePickerModal

                val now = System.currentTimeMillis()

                val selectedLocalDate = Instant.ofEpochMilli(selectedDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                val today = LocalDate.now()

                when {
                    // future date not allowed
                    selectedLocalDate.isAfter(today) -> {
                        Toast.makeText(
                            context,
                            futureDateNotAllowed,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    // today -> use current exact time
                    selectedLocalDate.isEqual(today) -> {
                        journalDate = now
                    }

                    // past date -> keep selected date but current time
                    else -> {

                        val currentTime = Instant.ofEpochMilli(now)
                            .atZone(ZoneId.systemDefault())
                            .toLocalTime()

                        val finalMillis = selectedLocalDate
                            .atTime(currentTime)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()

                        journalDate = finalMillis
                    }
                }
            }
        ) {
            showDateDialog = false
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

    if(showAudioRecorder){
        RecordAudioDialog(context, recorder, {onJournalEvents(JournalEvents.ImportAudio(context, it!!, contentState))}) {
            showAudioRecorder=false
        }
    }

    if (showLabelDialog) {
        SelectLabelDialog(currentLabelIds, allLabels,
            onConfirmation = {
                currentLabelIds.clear()
                currentLabelIds.addAll(it)
            },
            onDismissRequest = { showLabelDialog = false },
            onAddLabel = { navController.navigate(NavRoutes.EditLabels.withArgs(journal.workspaceId)) }
        )
    }

    NotesInfoBottomSheet(
        isVisible = showAboutNotes,
        words = aboutJournal.wordCountWithPunctuation,
        characters = aboutJournal.charCount,
        lastEdited = "${convertMillisToDate(journal.dateTime)}, ${convertMillisToTime(journal.dateTime)}",
        sheetState = sheetState,
        paragraph = aboutJournal.paragraphCount,
        wordsWithoutPunctuations = aboutJournal.wordCountWithoutPunctuation,
        lines = aboutJournal.lineCount
    ) { showAboutNotes=false }
}