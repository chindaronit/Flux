package com.flux.other

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebView
import androidx.activity.result.ActivityResultLauncher
import android.graphics.Canvas
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.View
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.flux.ui.viewModel.NotesViewModel
import com.flux.ui.viewModel.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.createBitmap
import com.flux.other.Properties.splitPropertiesAndContent
import com.flux.ui.viewModel.JournalViewModel
import com.yangdai.opennote.presentation.util.extension.highlight.Highlight
import com.yangdai.opennote.presentation.util.extension.highlight.HighlightExtension
import org.commonmark.ext.front.matter.YamlFrontMatterExtension
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.ins.Ins
import org.commonmark.ext.ins.InsExtension
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.CustomNode
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.Heading
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.parser.IncludeSourceSpans
import org.commonmark.parser.Parser
import androidx.core.net.toUri

fun Int.toHexColor(): String {
    return String.format("#%06X", 0xFFFFFF and this)
}

@Composable
fun rememberCustomTabsIntent(): CustomTabsIntent {
    return remember {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
    }
}

fun getOrCreateDirectory(
    context: Context, parentUri: Uri, dirName: String
): DocumentFile? {
    val parent = DocumentFile.fromTreeUri(context, parentUri)
        ?: return null

    return try {
        parent.findFile(dirName)?.let { existingFile ->
            if (existingFile.isDirectory) {
                return existingFile
            }
        }

        parent.createDirectory(dirName)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getFileName(context: Context, uri: Uri): String? {
    val docFile = DocumentFile.fromSingleUri(context, uri)
    return docFile?.name
}

fun ensureStorageRoot(
    scope: CoroutineScope,
    settingsViewModel: SettingsViewModel,
    rootPicker: ActivityResultLauncher<Intent>,
    onReady: () -> Unit
) {
    scope.launch {
        val isReady = settingsViewModel.isStorageReady()

        withContext(Dispatchers.Main) {
            if (isReady) { onReady() }
            else {
                rootPicker.launch(
                    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                        addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        )
                    }
                )
            }
        }
    }
}

fun tryRestoreUriPermission(context: Context, uriString: String): Boolean {
    return try {
        val uri = uriString.toUri()

        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )

        true
    } catch (_: SecurityException) {
        false
    }
}

fun hasFileWithName(dir: DocumentFile, fileName: String): Boolean {
    return dir.listFiles().any { file ->
        file.name == fileName
    }
}

fun getFileExtension(context: Context, uri: Uri): String {
    val sourceFile = DocumentFile.fromSingleUri(context, uri)

    sourceFile?.name?.let { name ->
        val lastDot = name.lastIndexOf('.')
        if (lastDot > 0) {
            return name.substring(lastDot + 1)
        }
    }

    val mimeType = sourceFile?.type ?: context.contentResolver.getType(uri)
    return when (mimeType) {
        "audio/mpeg" -> "mp3"
        "audio/wav", "audio/x-wav" -> "wav"
        "audio/ogg" -> "ogg"
        "audio/aac" -> "aac"
        "audio/x-m4a" -> "m4a"
        else -> "mp3"
    }
}

fun findAllIndices(text: String, word: String): List<Pair<Int, Int>> {
    if (word.isBlank()) return emptyList()

    return buildList {
        var index = text.indexOf(word)
        while (index != -1) {
            add(index to (index + word.length))
            index = text.indexOf(word, index + 1)
        }
    }
}

fun shareJournal(
    context: Context,
    exportType: ExportType,
    title: String,
    content: String,
    viewModel: JournalViewModel,
    readWebView: WebView?
){
    when (exportType) {
        ExportType.TXT,
        ExportType.MARKDOWN,
        ExportType.HTML ->
            shareAsText(context, exportType, title, content, viewModel)

        ExportType.IMAGE -> {
            val view = readWebView ?: return
            shareAsImage(context, view, title)
        }

        ExportType.PDF -> {}
    }
}

private fun shareAsText(
    context: Context,
    type: ExportType,
    title: String,
    content: String,
    viewModel: JournalViewModel
) {
    val (mimeType, content) = when (type) {
        ExportType.TXT ->
            "text/plain" to content

        ExportType.MARKDOWN ->
            "text/markdown" to content

        ExportType.HTML ->
            "text/html" to viewModel.renderMarkdown(content)

        else -> return
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        this.type = mimeType
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, content)
    }

    context.startActivity(
        Intent.createChooser(intent, "Share Journal")
    )
}

fun shareNote(
    context: Context,
    exportType: ExportType,
    noteTitle: String,
    noteDescription: String,
    notesViewModel: NotesViewModel,
    readWebView: WebView?
) {
    when (exportType) {
        ExportType.TXT,
        ExportType.MARKDOWN,
        ExportType.HTML ->
            shareAsText(context, exportType, noteTitle, noteDescription, notesViewModel)

        ExportType.IMAGE -> {
            val view = readWebView ?: return
            shareAsImage(context, view, noteTitle)
        }

        ExportType.PDF -> {}
    }
}

private fun shareAsText(
    context: Context,
    type: ExportType,
    noteTitle: String,
    noteDescription: String,
    notesViewModel: NotesViewModel
) {
    val (mimeType, content) = when (type) {
        ExportType.TXT ->
            "text/plain" to noteDescription

        ExportType.MARKDOWN ->
            "text/markdown" to noteDescription

        ExportType.HTML ->
            "text/html" to notesViewModel.renderMarkdown(noteDescription)

        else -> return
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        this.type = mimeType
        putExtra(Intent.EXTRA_SUBJECT, noteTitle)
        putExtra(Intent.EXTRA_TEXT, content)
    }

    context.startActivity(
        Intent.createChooser(intent, "Share note")
    )
}

private fun sanitizeFileName(name: String): String {
    return name
        .replace(Regex("[\\\\/:*?\"<>|]"), "") // remove illegal chars
        .replace("\\s+".toRegex(), "_")        // collapse spaces
        .take(50)                               // limit length
}

private fun shareAsImage(
    context: Context,
    webView: WebView,
    noteName: String) {
    // temp file
    val safeName = sanitizeFileName(noteName)

    val file = File(
        context.cacheDir,
        "${safeName}_${System.currentTimeMillis()}_preview.jpg"
    )

    val bitmap = convertHtmlToBitmap(webView) ?: return

    // draw exactly what user sees
    val canvas = Canvas(bitmap)
    webView.draw(canvas)

    FileOutputStream(file).use { stream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        bitmap.recycle()
    }

    val imageUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, imageUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
        Intent.createChooser(shareIntent, "Share note as image")
    )
}

fun convertHtmlToBitmap(webView: WebView): Bitmap? {
    return try {
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        webView.layout(
            0,
            0,
            webView.measuredWidth,
            webView.measuredHeight
        )

        if (webView.measuredWidth <= 0 || webView.measuredHeight <= 0) return null

        val bitmap = createBitmap(webView.measuredWidth, webView.measuredHeight)

        val canvas = Canvas(bitmap)
        webView.draw(canvas)

        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun printPdf(
    activity: Activity,
    webView: WebView?,
    noteName: String
) {
    if(webView==null) return
    createWebPrintJob(
        webView = webView,
        activity = activity,
        name = sanitizeFileName(noteName)
    )
}

private fun createWebPrintJob(
    webView: WebView,
    activity: Activity?,
    name: String
) {
    val printManager =
        activity?.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: return

    val jobName = "$name.pdf"

    val printAdapter = webView.createPrintDocumentAdapter(jobName)

    val printAttributes = PrintAttributes.Builder()
        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        .setResolution(
            PrintAttributes.Resolution(
                "pdf",
                Context.PRINT_SERVICE,
                300,
                300
            )
        )
        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
        .build()

    printManager.print(
        jobName,
        printAdapter,
        printAttributes
    )
}

@Stable
data class HeaderNode(
    val title: String,
    val level: Int,
    val range: IntRange,
    val children: MutableList<HeaderNode> = mutableListOf()
)

val PARSER: Parser =
    Parser.builder().extensions(
        listOf(
            StrikethroughExtension.create(),
            InsExtension.create(),
            HighlightExtension.create(),
            TablesExtension.create(),
            YamlFrontMatterExtension.create()
        )
    ).includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES).build()

fun findTagRanges(text: String): StyleRanges {
    if (text.isEmpty()) return StyleRanges.EMPTY
    val document = PARSER.parse(text)
    val codeRanges = mutableListOf<IntRange>()
    val boldRanges = mutableListOf<IntRange>()
    val italicRanges = mutableListOf<IntRange>()
    val boldItalicRanges = mutableListOf<IntRange>()
    val strikethroughRanges = mutableListOf<IntRange>()
    val underlineRanges = mutableListOf<IntRange>()
    val highlightRanges = mutableListOf<IntRange>()
    val headerRanges = mutableListOf<Pair<IntRange, Int>>()
    val markerRanges = mutableListOf<IntRange>()
    val linkRanges = mutableListOf<IntRange>()
    val fencedCodeBlockInfoRanges = mutableListOf<IntRange>()
    val codeBlockContentRanges = mutableListOf<IntRange>()


    document.accept(object : AbstractVisitor() {
        override fun visit(code: Code) {
            val span = code.sourceSpans.first()
            codeRanges.add(span.inputIndex until (span.inputIndex + span.length))
        }

        override fun visit(emphasis: Emphasis) {
            val span = emphasis.sourceSpans.first()
            when {
                emphasis.parent is StrongEmphasis || emphasis.firstChild is StrongEmphasis -> {
                    boldItalicRanges.add(span.inputIndex until (span.inputIndex + span.length))
                }

                else -> {
                    italicRanges.add(span.inputIndex until (span.inputIndex + span.length))
                }
            }
        }

        override fun visit(strong: StrongEmphasis) {
            val span = strong.sourceSpans.first()
            when {
                strong.parent is Emphasis || strong.firstChild is Emphasis -> {
                    boldItalicRanges.add(span.inputIndex until (span.inputIndex + span.length))
                }

                else -> {
                    boldRanges.add(span.inputIndex until (span.inputIndex + span.length))
                }
            }
        }

        override fun visit(listItem: ListItem) {
            val child = listItem.firstChild
            val markerIndent = listItem.markerIndent ?: 0
            if (child is Paragraph) {
                val node = child.firstChild
                if (node is Text) {
                    val span = listItem.sourceSpans.firstOrNull()
                    if (span != null) {
                        val literal = node.literal
                        val markerStart = span.inputIndex
                        markerRanges.add(markerStart until (markerStart + markerIndent + 1))
                        val matchResult = REGEX_TASK_LIST_ITEM.find(literal)
                        if (matchResult != null) {
                            val markerLength = 3 // Length of [x] or [ ]

                            // Add the task list marker range to markerRanges
                            markerRanges.add(markerStart + markerIndent + 2 until (markerStart + markerIndent + 2 + markerLength))
                        }
                    }
                }
            }
            visitChildren(listItem)
        }

        override fun visit(bulletList: BulletList) {
            var item = bulletList.firstChild
            while (item != null) {
                val span = item.sourceSpans.firstOrNull()
                if (span != null) {
                    // The marker is at the beginning of the list item,  -, * or +
                    val marker = bulletList.marker ?: "*"
                    val markerLength = marker.length
                    // Add the bullet marker range
                    markerRanges.add(span.inputIndex until (span.inputIndex + markerLength))
                }
                item = item.next
            }
            visitChildren(bulletList)
        }

        override fun visit(orderedList: OrderedList) {
            var item = orderedList.firstChild

            while (item != null) {
                val span = item.sourceSpans.firstOrNull()
                if (span != null) {
                    // Extract the text from the source span
                    val itemText =
                        text.substring(
                            span.inputIndex,
                            span.inputIndex + span.length
                        )

                    // Find the delimiter in the text
                    val delimiter = orderedList.markerDelimiter ?: "."
                    val delimiterIndex = itemText.indexOf(delimiter)

                    if (delimiterIndex > 0) {
                        // Add range for just the marker part
                        markerRanges.add(span.inputIndex until (span.inputIndex + delimiterIndex + 1))
                    }
                }
                item = item.next
            }
            visitChildren(orderedList)
        }

        override fun visit(link: Link) {
            val span = link.sourceSpans.firstOrNull()
            if (span != null) {
                // The entire link including text and URL needs to be styled
                // Format is [text](url)
                linkRanges.add(span.inputIndex until (span.inputIndex + span.length))
            }
            visitChildren(link)
        }

        override fun visit(image: Image) {
            val span = image.sourceSpans.firstOrNull()
            if (span != null) {
                // The entire image including alt text and URL needs to be styled
                // Format is ![alt text](url)
                linkRanges.add(span.inputIndex until (span.inputIndex + span.length))
            }
            visitChildren(image)
        }

        override fun visit(customNode: CustomNode) {
            when (customNode) {
                is Strikethrough -> {
                    val span = customNode.sourceSpans.first()
                    strikethroughRanges.add(span.inputIndex until (span.inputIndex + span.length))
                }

                is Ins -> {
                    val span = customNode.sourceSpans.first()
                    underlineRanges.add(span.inputIndex until (span.inputIndex + span.length))
                }

                is Highlight -> {
                    val span = customNode.sourceSpans.first()
                    highlightRanges.add(span.inputIndex until (span.inputIndex + span.length))
                }

                is TableRow -> {
                    val span = customNode.sourceSpans.firstOrNull()
                    if (span != null) {
                        // Get the row's text
                        val rowText =
                            text.substring(
                                span.inputIndex,
                                span.inputIndex + span.length
                            )

                        // Find all | characters in the row
                        var charIndex = 0
                        while (charIndex < rowText.length) {
                            val pipeIndex = rowText.indexOf('|', charIndex)
                            if (pipeIndex == -1) break

                            // Add the | separator to marker ranges
                            markerRanges.add((span.inputIndex + pipeIndex) until (span.inputIndex + pipeIndex + 1))
                            charIndex = pipeIndex + 1
                        }
                    }
                }
            }
            visitChildren(customNode)
        }

        override fun visit(heading: Heading) {
            val span = heading.sourceSpans.first()
            if (span.inputIndex + span.length + 1 <= text.length) {
                val range = span.inputIndex until (span.inputIndex + span.length + 1)
                headerRanges.add(range to heading.level)
            }
            visitChildren(heading)
        }

        override fun visit(fencedCodeBlock: FencedCodeBlock) {
            val span = fencedCodeBlock.sourceSpans.firstOrNull() ?: return

            // Get the opening fence marker (```language)
            val openingFenceStartIndex = span.inputIndex
            val openingMarkerLength = fencedCodeBlock.openingFenceLength ?: return
            val infoStringStartIndex = openingFenceStartIndex + openingMarkerLength
            markerRanges.add(openingFenceStartIndex until infoStringStartIndex) // ```
            val infoStringLength = fencedCodeBlock.info?.length ?: 0
            fencedCodeBlockInfoRanges.add(infoStringStartIndex until (infoStringStartIndex + infoStringLength)) // language

            val closingMarkerLength = fencedCodeBlock.closingFenceLength ?: return
            val blockContentLength =
                if (fencedCodeBlock.literal.isEmpty()) 0 else fencedCodeBlock.literal.length + 1
            val fence =
                openingFenceStartIndex + openingMarkerLength + infoStringLength + blockContentLength
            codeBlockContentRanges.add((openingFenceStartIndex + openingMarkerLength + infoStringLength) until fence) // content
            if (fence + closingMarkerLength <= text.length) {
                markerRanges.add(fence until (fence + closingMarkerLength))
            }
        }

        override fun visit(indentedCodeBlock: IndentedCodeBlock) {
            val span = indentedCodeBlock.sourceSpans.firstOrNull()
            if (span != null) {
                val range = span.inputIndex until (span.inputIndex + span.length)
                codeBlockContentRanges.add(range)
            }
        }
    })

    return StyleRanges(
        codeRanges = codeRanges,
        boldRanges = boldRanges,
        italicRanges = italicRanges,
        boldItalicRanges = boldItalicRanges,
        strikethroughRanges = strikethroughRanges,
        underlineRanges = underlineRanges,
        highlightRanges = highlightRanges,
        headerRanges = headerRanges,
        markerRanges = markerRanges,
        linkRanges = linkRanges,
        fencedCodeBlockInfoRanges = fencedCodeBlockInfoRanges,
        codeBlockContentRanges = codeBlockContentRanges
    )
}

fun parseMarkdownContent(text: String): AnnotatedString {
    if (text.isBlank()) return AnnotatedString(text)
    val textWithoutProperties =
        text.splitPropertiesAndContent().second
            .replace("- [ ]", "☐")
            .replace("- [x]", "☑")
    val styleRanges = findTagRanges(textWithoutProperties)

    return buildAnnotatedString {
        fun safeAddStyle(style: SpanStyle, start: Int, end: Int) {
            val safeStart = start.coerceAtLeast(0).coerceAtMost(text.length)
            val safeEnd = end.coerceAtLeast(0).coerceAtMost(text.length)
            addStyle(style, safeStart, safeEnd)
        }

        fun safeAddStyle(style: ParagraphStyle, start: Int, end: Int) {
            val safeStart = start.coerceAtLeast(0).coerceAtMost(text.length)
            val safeEnd = end.coerceAtLeast(0).coerceAtMost(text.length)
            addStyle(style, safeStart, safeEnd)
        }

        styleRanges.apply {
            codeRanges.forEach { range ->
                safeAddStyle(CODE_STYLE, range.first, range.last + 1)
                safeAddStyle(SYMBOL_STYLE, range.first, range.first + 1)
                safeAddStyle(SYMBOL_STYLE, range.last - 1 + 1, range.last + 1)
            }
            boldItalicRanges.forEach { range ->
                safeAddStyle(BOLD_ITALIC_STYLE, range.first, range.last + 1)
                safeAddStyle(SYMBOL_STYLE, range.first, range.first + 3)
                safeAddStyle(SYMBOL_STYLE, range.last - 3 + 1, range.last + 1)
            }
            boldRanges.forEach { range ->
                safeAddStyle(BOLD_STYLE, range.first, range.last + 1)
                safeAddStyle(SYMBOL_STYLE, range.first, range.first + 2)
                safeAddStyle(SYMBOL_STYLE, range.last - 2 + 1, range.last + 1)
            }
            italicRanges.forEach { range ->
                safeAddStyle(ITALIC_STYLE, range.first, range.last + 1)
                safeAddStyle(SYMBOL_STYLE, range.first, range.first + 1)
                safeAddStyle(SYMBOL_STYLE, range.last - 1 + 1, range.last + 1)
            }
            highlightRanges.forEach { range ->
                safeAddStyle(HIGHLIGHT_STYLE, range.first, range.last + 1)
                safeAddStyle(SYMBOL_STYLE, range.first, range.first + 2)
                safeAddStyle(SYMBOL_STYLE, range.last - 2 + 1, range.last + 1)
            }

            val combinedRanges = (strikethroughRanges + underlineRanges).distinct()
            combinedRanges.forEach { range ->
                val hasStrikethrough = strikethroughRanges.any { it.overlaps(range) }
                val hasUnderline = underlineRanges.any { it.overlaps(range) }
                val style = when {
                    hasStrikethrough && hasUnderline -> STRIKETHROUGH_AND_UNDERLINE_STYLE
                    hasStrikethrough -> STRIKETHROUGH_STYLE
                    hasUnderline -> UNDERLINE_STYLE
                    else -> return@forEach
                }
                safeAddStyle(style, range.first, range.last + 1)
            }

            strikethroughRanges.forEach { range ->
                safeAddStyle(SYMBOL_STYLE, range.first, range.first + 2)
                safeAddStyle(SYMBOL_STYLE, range.last - 2 + 1, range.last + 1)
            }

            underlineRanges.forEach { range ->
                safeAddStyle(SYMBOL_STYLE, range.first, range.first + 2)
                safeAddStyle(SYMBOL_STYLE, range.last - 2 + 1, range.last + 1)
            }

            headerRanges.forEach { (range, level) ->
                safeAddStyle(HEADER_STYLES[level - 1], range.first, range.last + 1)
                safeAddStyle(HEADER_LINE_STYLES[level - 1], range.first, range.last + 1)
                safeAddStyle(SYMBOL_STYLE, range.first, range.first + level + 1)
            }

            // Add styling for list markers
            markerRanges.forEach { range ->
                safeAddStyle(MARKER_STYLE, range.first, range.last + 1)
            }
            fencedCodeBlockInfoRanges.forEach { range ->
                safeAddStyle(KEYWORD_STYLE, range.first, range.last + 1)
            }
            codeBlockContentRanges.forEach { range ->
                safeAddStyle(CODE_BLOCK_STYLE, range.first, range.last + 1)
            }
        }
        append(textWithoutProperties)
    }
}