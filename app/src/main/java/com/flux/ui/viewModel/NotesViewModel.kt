package com.flux.ui.viewModel

import android.content.Context
import android.graphics.Bitmap
import android.webkit.WebView
import androidx.compose.ui.util.fastJoinToString
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.data.model.LabelModel
import com.flux.data.model.NotesModel
import com.flux.data.repository.NoteRepository
import com.flux.data.repository.SettingsRepository
import com.flux.other.Constants
import com.flux.other.ExportType
import com.flux.other.HeaderNode
import com.flux.other.PARSER
import com.flux.other.convertHtmlToBitmap
import com.flux.other.getFileExtension
import com.flux.other.getFileName
import com.flux.other.getOrCreateDirectory
import com.flux.other.hasFileWithName
import com.flux.ui.events.NotesEvents
import com.flux.ui.screens.notes.add
import com.flux.ui.state.NotesState
import com.flux.ui.state.TextState
import com.yangdai.opennote.presentation.util.extension.highlight.HighlightExtension
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.commonmark.Extension
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.footnotes.FootnotesExtension
import org.commonmark.ext.front.matter.YamlFrontMatterExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension
import org.commonmark.ext.image.attributes.ImageAttributesExtension
import org.commonmark.ext.ins.InsExtension
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.Heading
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import java.io.OutputStreamWriter
import javax.inject.Inject
import kotlin.collections.forEachIndexed

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    val repository: NoteRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val mutex = Mutex()
    private val _state: MutableStateFlow<NotesState> = MutableStateFlow(NotesState())
    val state: StateFlow<NotesState> = _state.asStateFlow()

    // Markdown
    private var extensions: List<Extension> = listOf(
        TablesExtension.create(),
        AutolinkExtension.create(),
        FootnotesExtension.create(),
        HighlightExtension.create(),
        HeadingAnchorExtension.create(),
        InsExtension.create(),
        ImageAttributesExtension.create(),
        StrikethroughExtension.create(),
        TaskListItemsExtension.create(),
        YamlFrontMatterExtension.create()
    )

    private var parser: Parser = Parser.builder().extensions(extensions).build()
    private var renderer: HtmlRenderer = HtmlRenderer.builder().extensions(extensions).build()
    private var lastOutlineContentHash: Int? = null

    fun renderMarkdown(markdown: String): String {
        return renderer.render(parser.parse(markdown))
    }

    fun onEvent(event: NotesEvents) {
        viewModelScope.launch { reduce(event = event) }
    }

    private suspend fun updateState(reducer: (NotesState) -> NotesState) {
        mutex.withLock { _state.value = reducer(_state.value) }
    }

    init {
        viewModelScope.launch {
            state
                .map { it.workspaceId }
                .distinctUntilChanged()
                .filterNotNull()
                .flatMapLatest { workspaceId ->
                    combine(
                        repository.loadAllNotes(workspaceId),
                        repository.loadAllLabels(workspaceId)
                    ) { notes, labels ->
                        notes.sortedByDescending { it.lastEdited } to labels
                    }
                }
                .collect { (notes, labels) ->
                    updateState {
                        it.copy(
                            isNotesLoading = false,
                            isLabelsLoading = false,
                            allNotes = notes,
                            allLabels = labels
                        )
                    }
                }
        }
    }

    private suspend fun reduce(event: NotesEvents) {
        when (event) {
            is NotesEvents.UpsertNote -> updateNotes(event.data)
            is NotesEvents.DeleteNotes -> deleteNotes(event.data)
            is NotesEvents.TogglePinMultiple -> togglePinMultiple(event.data)
            is NotesEvents.DeleteNote -> deleteNote(event.data)
            is NotesEvents.DeleteLabel -> deleteLabel(event.data)
            is NotesEvents.UpsertLabel -> upsertLabel(event.data)
            is NotesEvents.DeleteAllWorkspaceNotes -> deleteWorkspaceNotes(event.workspaceId)
            is NotesEvents.ClearSelection -> { updateState { it.copy(selectedNotes = emptyList()) } }
            is NotesEvents.SelectNotes -> { updateState { it.copy(selectedNotes = it.selectedNotes.plus(event.noteId)) } }
            is NotesEvents.UnSelectNotes -> { updateState { it.copy(selectedNotes = it.selectedNotes.minus(event.noteId)) } }
            is NotesEvents.SelectAllNotes -> { updateState { it.copy(selectedNotes = it.allNotes.map { note -> note.notesId }) } }

            is NotesEvents.ImportImages -> {
                val context = event.context
                val contentResolver = context.contentResolver
                val uriList = event.uriList
                val rootUri = settingsRepository.getStorageRoot()
                val contentState = event.contentState

                viewModelScope.launch(Dispatchers.IO) {
                    val openNoteDir = getOrCreateDirectory(context, rootUri, Constants.File.FLUX)
                    val imagesDir = openNoteDir?.let { dir -> getOrCreateDirectory(context, dir.uri, Constants.File.FLUX_IMAGES) }
                    val savedUriList = mutableListOf<String>()
                    imagesDir?.let { dir ->
                        uriList.forEachIndexed { _, uri ->

                            val timestamp = System.currentTimeMillis()
                            val name = getFileName(context, uri)
                            val fileName = "${name?.substringBeforeLast(".")}_${timestamp}.${name?.substringAfterLast(".")}"

                            try {
                                contentResolver.openInputStream(uri)?.use { input ->
                                    val mimeType = contentResolver.getType(uri) ?: "image/*"
                                    val newFile = dir.createFile(mimeType, fileName)

                                    newFile?.let { file ->
                                        contentResolver.openOutputStream(file.uri)?.use { output -> input.copyTo(output) }
                                        savedUriList.add("![](${fileName})")
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        contentState.edit { add(savedUriList.fastJoinToString(separator = "\n")) }
                    }
                }
            }

            is NotesEvents.ImportVideo -> {
                val context = event.context
                val uri = event.uri
                val rootUri = settingsRepository.getStorageRoot()
                val contentState = event.contentState

                viewModelScope.launch(Dispatchers.IO) {
                    val openNoteDir = getOrCreateDirectory(context, rootUri, Constants.File.FLUX)
                    val videosDir = openNoteDir?.let { dir -> getOrCreateDirectory(context, dir.uri, Constants.File.FLUX_VIDEOS) }

                    videosDir?.let { dir ->
                        val name = getFileName(context, uri)
                        val fileName =
                            "${name?.substringBeforeLast(".")}_${System.currentTimeMillis()}.${
                                name?.substringAfterLast(".")
                            }"
                        val newFile = dir.createFile("video/*", fileName)
                        newFile?.let { file ->
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                context.contentResolver.openOutputStream(file.uri)?.use { output ->
                                    input.copyTo(output)
                                    withContext(Dispatchers.Main) {
                                        contentState.edit { add("<video src=\"$fileName\" controls></video>") }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is NotesEvents.ImportAudio -> {
                val context = event.context
                val sourceUri = event.sourceUri
                val rootUri = settingsRepository.getStorageRoot()
                val contentState = event.contentState

                val openNoteDir =
                    getOrCreateDirectory(context, rootUri, Constants.File.FLUX)
                val audioDir = openNoteDir?.let { dir ->
                    getOrCreateDirectory(context, dir.uri, Constants.File.FLUX_AUDIO)
                }

                audioDir?.let { dir ->
                    val sourceFile = DocumentFile.fromSingleUri(context, sourceUri)
                    val sourceFileName = sourceFile?.name

                    if (sourceFileName != null && hasFileWithName(dir, sourceFileName)) {
                        contentState.edit { add("<audio src=\"$sourceFileName\" controls></audio>") }
                        return
                    }

                    val extension = getFileExtension(context, sourceUri)
                    val fileName = "audio_${System.currentTimeMillis()}.$extension"
                    val mimeType = sourceFile?.type ?: "audio/*"

                    dir.createFile(mimeType, fileName)?.let { newFile ->
                        context.contentResolver.openInputStream(sourceUri)?.use { input ->
                            context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                                input.copyTo(output)
                            }
                        }
                        contentState.edit { add("<audio src=\"$fileName\" controls></audio>") }
                    }
                }
            }

            is NotesEvents.ExportNote -> {
                viewModelScope.launch(Dispatchers.IO) {
                    exportNoteToStorage(
                        context = event.context,
                        noteTitle = event.noteTitle,
                        noteDescription = event.noteDescription,
                        type = event.type,
                        webView = event.webView
                    )
                }
            }

            is NotesEvents.CalculateOutline -> {
                val hash = event.content.hashCode()
                if (hash == lastOutlineContentHash) return

                viewModelScope.launch(Dispatchers.IO) {
                    val outline = buildOutline(event.content)
                    lastOutlineContentHash = hash
                    updateState { it.copy(outline = outline) }
                }
            }

            is NotesEvents.CalculateTextState -> {
                viewModelScope.launch(Dispatchers.IO) {
                    updateState { it.copy(textState = TextState.fromText(event.content)) }
                }
            }

            is NotesEvents.EnterWorkspace -> { updateState { if (it.workspaceId == event.workspaceId) { it } else { it.copy(workspaceId = event.workspaceId, isNotesLoading = true, isLabelsLoading = true) }} }
        }
    }

    fun buildOutline(markdown: CharSequence): HeaderNode {
        val document = PARSER.parse(markdown.toString())
        val root = HeaderNode("", 0, IntRange.EMPTY)
        val headerStack = mutableListOf(root)

        document.accept(object : AbstractVisitor() {
            override fun visit(heading: Heading) {
                val span = heading.sourceSpans.firstOrNull() ?: return
                val range = span.inputIndex until (span.inputIndex + span.length)

                val title = markdown
                    .substring(range)
                    .trimStart('#')
                    .trim()

                val node = HeaderNode(title, heading.level, range)

                while (headerStack.last().level >= heading.level) {
                    headerStack.removeAt(headerStack.lastIndex)
                }

                headerStack.last().children.add(node)
                headerStack.add(node)
            }
        })

        return root
    }

    private suspend fun exportNoteToStorage(
        context: Context,
        noteTitle: String,
        noteDescription: String,
        type: ExportType,
        webView: WebView?
    ) {
        val rootUri = settingsRepository.getStorageRoot()
        val dir = getOrCreateDirectory(context, rootUri, Constants.File.FLUX) ?: return

        when (type) {
            ExportType.TXT,
            ExportType.MARKDOWN,
            ExportType.HTML -> exportAsText(dir, context, noteTitle, noteDescription, type)

            ExportType.IMAGE -> {
                val view = webView ?: return
                exportAsImage(dir, context, view, noteTitle)
            }

            ExportType.PDF -> {}
        }
    }

    private fun exportAsText(
        dir: DocumentFile,
        context: Context,
        noteTitle: String,
        noteDescription: String,
        type: ExportType
    ) {
        val (mime, extension, content) = when (type) {
            ExportType.TXT ->
                Triple("text/plain", ".txt", noteDescription)

            ExportType.MARKDOWN ->
                Triple("text/markdown", ".md", noteDescription)

            ExportType.HTML ->
                Triple("text/html", ".html", renderMarkdown(noteDescription))

            else -> return
        }

        val file = dir.createFile(mime, noteTitle.trim() + extension) ?: return

        context.contentResolver.openOutputStream(file.uri)?.use { stream ->
            OutputStreamWriter(stream).use { it.write(content) }
        }
    }

    private fun exportAsImage(
        dir: DocumentFile,
        context: Context,
        webView: WebView,
        noteTitle: String
    ) {
        val bitmap = convertHtmlToBitmap(webView) ?: return

        val file = dir.createFile("image/jpeg", "${noteTitle.trim()}.jpg") ?: return

        context.contentResolver.openOutputStream(file.uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            bitmap.recycle()
        }
    }

    private fun deleteNotes(data: List<NotesModel>) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteNotes(data.map { it.notesId }) }
    }

    private fun deleteNote(data: NotesModel) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteNote(data) }
    }

    private fun deleteLabel(data: LabelModel) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteLabel(data) }
    }

    private fun upsertLabel(data: LabelModel) {
        viewModelScope.launch(Dispatchers.IO) { repository.upsertLabel(data) }
    }

    private fun deleteWorkspaceNotes(workspaceId: String) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteAllWorkspaceNotes(workspaceId) }
    }

    private fun togglePinMultiple(data: List<NotesModel>) {
        viewModelScope.launch(Dispatchers.IO) {
            val isAllPinned = data.all { it.isPinned }
            if (isAllPinned) {
                val updatedNotes = data.map { it.copy(isPinned = false) }
                repository.upsertNotes(updatedNotes)
            } else {
                val updatedNotes = data.map { it.copy(isPinned = true) }
                repository.upsertNotes(updatedNotes)
            }
        }
    }

    private fun updateNotes(data: NotesModel) {
        val isNewNote = state.value.allNotes.none { it.notesId == data.notesId }
        val isBlankNote = data.title.trim().isBlank() && data.labels.isEmpty() && data.description.trim().isBlank()
        if (isNewNote && isBlankNote) return

        viewModelScope.launch(Dispatchers.IO) { repository.upsertNote(data) }
    }
}
