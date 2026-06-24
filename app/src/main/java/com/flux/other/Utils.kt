package com.flux.other

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebView
import androidx.activity.result.ActivityResultLauncher
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
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
import com.flux.data.model.EventModel
import com.flux.data.model.TodoModel
import com.flux.data.model.occursOn
import com.flux.data.model.toHtml
import com.flux.data.model.toMarkdown
import com.flux.data.model.toText
import org.commonmark.ext.gfm.tables.TablesExtension
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

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

fun shareTodo(
    context: Context,
    exportType: ExportType,
    list: TodoModel,
    readWebView: WebView?
) {
    when (exportType) {

        ExportType.TXT -> {
            shareTodoText(
                context,
                list.toText(),
                "text/plain"
            )
        }

        ExportType.MARKDOWN -> {
            shareTodoText(
                context,
                list.toMarkdown(),
                "text/markdown"
            )
        }

        ExportType.HTML -> {
            shareTodoText(
                context,
                list.toHtml(),
                "text/html"
            )
        }

        ExportType.IMAGE -> {
            val activity = context.findActivity()
            exportHtmlAsImage(activity, list.toHtml()) { uri ->
                shareImageUri(context, uri)
            }
        }

        ExportType.PDF -> {
            readWebView?.let {
                createWebPrintJob(
                    it,
                    context as? Activity,
                    list.title
                )
            }
        }
    }
}

fun exportHtmlAsImage(
    activity: Activity,
    html: String,
    widthDp: Int = 480,
    onResult: (Uri) -> Unit
) {
    val tag = "ExportHtmlAsImage"

    val density = activity.resources.displayMetrics.density
    val widthPx = (widthDp * density).toInt()

    val rootView = activity.window.decorView as ViewGroup
    val mainHandler = Handler(Looper.getMainLooper())

    val webView = WebView(activity).apply {
        settings.javaScriptEnabled = true
        layoutParams = FrameLayout.LayoutParams(widthPx, FrameLayout.LayoutParams.WRAP_CONTENT)
        translationX = 10000f
    }
    rootView.addView(webView)

    webView.webViewClient = object : WebViewClient() {

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        }

        override fun onPageFinished(view: WebView, url: String?) {

            mainHandler.postDelayed({

                view.evaluateJavascript(
                    """
                    (function() {
                        var children = document.body.children;
                        if (children.length === 0) return '0';
                        var last = children[children.length - 1];
                        var rect = last.getBoundingClientRect();
                        var bottomWithPadding = rect.bottom + 32; 
                        return (bottomWithPadding * $density).toString();
                    })()
                    """.trimIndent()
                ) { result ->

                    val contentHeightPx = result
                        ?.trim()
                        ?.removeSurrounding("\"")
                        ?.toFloatOrNull()
                        ?.toInt()
                        ?.coerceAtLeast(100)
                        ?: 0

                    if (contentHeightPx <= 0) {
                        rootView.removeView(view)
                        return@evaluateJavascript
                    }

                    mainHandler.post {
                        // Force WebView to exactly contentHeightPx
                        view.layoutParams = FrameLayout.LayoutParams(widthPx, contentHeightPx)
                        view.measure(
                            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(contentHeightPx, View.MeasureSpec.EXACTLY)
                        )
                        view.layout(0, 0, widthPx, contentHeightPx)

                        view.post {
                            try {
                                val bitmap = createBitmap(widthPx, contentHeightPx)
                                val canvas = Canvas(bitmap)
                                canvas.clipRect(0, 0, widthPx, contentHeightPx)
                                view.draw(canvas)

                                rootView.removeView(view)

                                Thread {
                                    try {
                                        val uri = saveBitmapAndGetUri(activity, bitmap)
                                        mainHandler.post { onResult(uri) }
                                    } catch (e: Exception) {
                                        Log.e(tag, "❌ Failed to save bitmap: ${e.message}", e)
                                    }
                                }.start()
                            } catch (e: Exception) {
                                Log.e(tag, "❌ Exception during bitmap drawing: ${e.message}", e)
                                rootView.removeView(view)
                            }
                        }

                        Log.d(tag, "contentHeightPx = $contentHeightPx, density = $density")
                    }
                }
            }, 300)
        }

        override fun onReceivedError(
            view: WebView,
            request: android.webkit.WebResourceRequest?,
            error: android.webkit.WebResourceError?
        ) {
            Log.e(tag, "❌ onReceivedError | url=${request?.url} | error=${error?.description}")
        }
    }

    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}

fun saveBitmapAndGetUri(context: Context, bitmap: Bitmap): Uri {
    val cacheDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
    val file = File(cacheDir, "html_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

fun shareImageUri(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share image via"))
}

fun Context.findActivity(): Activity {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    throw IllegalStateException("Activity not found")
}

private fun shareTodoText(
    context: Context,
    text: String,
    mimeType: String
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_TEXT, text)
    }

    context.startActivity(
        Intent.createChooser(intent, null)
    )
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
    val normalizedText = if (text.endsWith('\n')) text else "$text\n"
    val document = PARSER.parse(normalizedText)
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
    val fenceMarkerRanges = mutableListOf<IntRange>() // NEW: tracks ``` open/close lines separately


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
            val spans = fencedCodeBlock.sourceSpans
            if (spans.isEmpty()) return

            spans.forEachIndexed { index, span ->
                when (index) {
                    0 -> {
                        // opening ``` or ```kotlin — goes into fenceMarkerRanges, not markerRanges
                        fenceMarkerRanges.add(
                            span.inputIndex until
                                    (span.inputIndex + span.length)
                        )

                        val openingText = text.substring(
                            span.inputIndex,
                            (span.inputIndex + span.length).coerceAtMost(text.length)
                        )

                        val firstSpace = openingText.indexOf(' ')
                        if (firstSpace != -1 && firstSpace + 1 < openingText.length) {
                            fencedCodeBlockInfoRanges.add(
                                (span.inputIndex + firstSpace + 1) until
                                        (span.inputIndex + openingText.length)
                            )
                        }
                    }

                    spans.lastIndex -> {
                        // closing ``` — goes into fenceMarkerRanges, not markerRanges
                        fenceMarkerRanges.add(
                            span.inputIndex until
                                    (span.inputIndex + span.length)
                        )
                    }

                    else -> {
                        codeBlockContentRanges.add(
                            span.inputIndex until
                                    (span.inputIndex + span.length)
                        )
                    }
                }
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
        codeBlockContentRanges = codeBlockContentRanges,
        fenceMarkerRanges = fenceMarkerRanges
    )
}

private data class LinkInfo(
    val origStart: Int,   // start in the pre-transform text
    val origEnd: Int,     // end   in the pre-transform text
    val display: String,  // visible label  ([display](url))
    val url: String       // destination URL
)

private fun collectLinks(text: String): List<LinkInfo> {
    val document = PARSER.parse(text)
    val links = mutableListOf<LinkInfo>()

    document.accept(object : AbstractVisitor() {
        override fun visit(link: Link) {
            val span = link.sourceSpans.firstOrNull() ?: return
            val display = buildString {
                var child = link.firstChild
                while (child != null) {
                    if (child is Text) append(child.literal)
                    child = child.next
                }
            }.ifEmpty { link.destination }
            links.add(LinkInfo(span.inputIndex, span.inputIndex + span.length, display, link.destination))
        }

        override fun visit(image: Image) {
            val span = image.sourceSpans.firstOrNull() ?: return
            val altText = buildString {
                var child = image.firstChild
                while (child != null) {
                    if (child is Text) append(child.literal)
                    child = child.next
                }
            }.ifEmpty { image.destination }
            links.add(LinkInfo(span.inputIndex, span.inputIndex + span.length, altText, image.destination))
        }
    })

    return links.sortedBy { it.origStart }
}

private fun transformLinks(rawText: String): Pair<String, List<LinkInfo>> {
    val links = collectLinks(rawText)
    if (links.isEmpty()) return rawText to emptyList()

    val sb = StringBuilder()
    val adjustedLinks = mutableListOf<LinkInfo>()
    var cursor = 0          // current position in rawText
    var shift = 0           // cumulative length change

    for (link in links) {
        // Append everything before this link unchanged
        sb.append(rawText, cursor, link.origStart)

        // Append only the display text
        val newStart = link.origStart + shift
        sb.append(link.display)
        val newEnd = newStart + link.display.length

        adjustedLinks.add(link.copy(origStart = newStart, origEnd = newEnd))

        shift += link.display.length - (link.origEnd - link.origStart)
        cursor = link.origEnd
    }

    // Append any remaining text after the last link
    sb.append(rawText, cursor, rawText.length)

    return sb.toString() to adjustedLinks
}

// URL annotation tag used for click handling
const val URL_ANNOTATION_TAG = "URL"

fun parseMarkdownContent(text: String, linkColor: Color = Color.Blue): AnnotatedString {
    if (text.isBlank()) return AnnotatedString(text)

    val rawText = text.splitPropertiesAndContent().second
        .replace("- [ ]", "☐")
        .replace("- [x]", "☑")
        .replace("- [X]", "☑")
        .replace("-", "•")

    // Transform [display](url) → display, and get adjusted link positions
    val (transformedText, adjustedLinks) = transformLinks(rawText)

    val styleRanges = findTagRanges(transformedText)

    return buildAnnotatedString {
        fun safeAddStyle(style: SpanStyle, start: Int, end: Int) {
            val safeStart = start.coerceAtLeast(0).coerceAtMost(transformedText.length)
            val safeEnd = end.coerceAtLeast(0).coerceAtMost(transformedText.length)
            if (safeStart < safeEnd) addStyle(style, safeStart, safeEnd)
        }

        fun safeAddStyle(style: ParagraphStyle, start: Int, end: Int) {
            val safeStart = start.coerceAtLeast(0).coerceAtMost(transformedText.length)
            val safeEnd = end.coerceAtLeast(0).coerceAtMost(transformedText.length)
            if (safeStart < safeEnd) addStyle(style, safeStart, safeEnd)
        }

        styleRanges.apply {
            // Inline code: `code`
            codeRanges.forEach { range ->
                safeAddStyle(
                    CODE_STYLE,
                    range.first,
                    range.last + 1
                )

                var delimiterLength = 0
                var i = range.first

                while (
                    i <= range.last &&
                    transformedText.getOrNull(i) == '`'
                ) {
                    delimiterLength++
                    i++
                }

                if (delimiterLength > 0) {
                    safeAddStyle(
                        SYMBOL_STYLE,
                        range.first,
                        range.first + delimiterLength
                    )

                    safeAddStyle(
                        SYMBOL_STYLE,
                        range.last - delimiterLength + 1,
                        range.last + 1
                    )
                }
            }

            // Fenced code block content lines
            codeBlockContentRanges.forEach { range ->
                safeAddStyle(
                    CODE_BLOCK_STYLE,
                    range.first,
                    range.last + 1
                )
            }

            // Fenced code block opening/closing ``` lines:
            // Apply CODE_BLOCK_STYLE so the background is contiguous with the content,
            // then apply SYMBOL_STYLE on top to dim/style the fence markers themselves.
            fenceMarkerRanges.forEach { range ->
                safeAddStyle(
                    CODE_BLOCK_STYLE,
                    range.first,
                    range.last + 1
                )
                safeAddStyle(
                    SYMBOL_STYLE,
                    range.first,
                    range.last + 1
                )
            }

            boldItalicRanges.forEach { range ->
                safeAddStyle(BOLD_ITALIC_STYLE, range.first, range.last + 1)
                safeAddStyle(SYMBOL_STYLE, range.first, range.first + 3)
                safeAddStyle(SYMBOL_STYLE, range.last - 2, range.last + 1)
            }

            boldRanges.forEach { range ->
                safeAddStyle(BOLD_STYLE, range.first, range.last + 1)
                safeAddStyle(SYMBOL_STYLE, range.first, range.first + 2)
                safeAddStyle(SYMBOL_STYLE, range.last - 1, range.last + 1)
            }

            italicRanges.forEach { range ->
                safeAddStyle(ITALIC_STYLE, range.first, range.last + 1)
                safeAddStyle(SYMBOL_STYLE, range.first, range.first + 1)
                safeAddStyle(SYMBOL_STYLE, range.last, range.last + 1)
            }

            highlightRanges.forEach { range ->
                safeAddStyle(HIGHLIGHT_STYLE, range.first, range.last + 1)
                safeAddStyle(SYMBOL_STYLE, range.first, range.first + 2)
                safeAddStyle(SYMBOL_STYLE, range.last - 1, range.last + 1)
            }

            val combinedRanges =
                (strikethroughRanges + underlineRanges).distinct()

            combinedRanges.forEach { range ->
                val hasStrikethrough =
                    strikethroughRanges.any { it.overlaps(range) }

                val hasUnderline =
                    underlineRanges.any { it.overlaps(range) }

                val style = when {
                    hasStrikethrough && hasUnderline ->
                        STRIKETHROUGH_AND_UNDERLINE_STYLE

                    hasStrikethrough ->
                        STRIKETHROUGH_STYLE

                    hasUnderline ->
                        UNDERLINE_STYLE

                    else -> return@forEach
                }

                safeAddStyle(
                    style,
                    range.first,
                    range.last + 1
                )
            }

            strikethroughRanges.forEach { range ->
                safeAddStyle(
                    SYMBOL_STYLE,
                    range.first,
                    range.first + 2
                )

                safeAddStyle(
                    SYMBOL_STYLE,
                    range.last - 1,
                    range.last + 1
                )
            }

            underlineRanges.forEach { range ->
                safeAddStyle(
                    SYMBOL_STYLE,
                    range.first,
                    range.first + 2
                )

                safeAddStyle(
                    SYMBOL_STYLE,
                    range.last - 1,
                    range.last + 1
                )
            }

            headerRanges.forEach { (range, level) ->
                safeAddStyle(
                    HEADER_STYLES[level - 1],
                    range.first,
                    range.last + 1
                )

                safeAddStyle(
                    HEADER_LINE_STYLES[level - 1],
                    range.first,
                    range.last + 1
                )

                safeAddStyle(
                    SYMBOL_STYLE,
                    range.first,
                    range.first + level + 1
                )
            }

            markerRanges.forEach { range ->
                safeAddStyle(
                    MARKER_STYLE,
                    range.first,
                    range.last + 1
                )
            }

            fencedCodeBlockInfoRanges.forEach { range ->
                safeAddStyle(
                    KEYWORD_STYLE,
                    range.first,
                    range.last + 1
                )
            }

        }

        // Apply link styles and URL annotations using the adjusted (post-transform) positions
        for (link in adjustedLinks) {
            val start = link.origStart.coerceAtLeast(0).coerceAtMost(transformedText.length)
            val end   = link.origEnd.coerceAtLeast(0).coerceAtMost(transformedText.length)
            if (start < end) {
                addStyle(
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    ),
                    start,
                    end
                )
                // Attach the URL so ClickableText callers can open it
                addStringAnnotation(
                    tag = URL_ANNOTATION_TAG,
                    annotation = link.url,
                    start = start,
                    end = end
                )
            }
        }

        append(transformedText)
    }
}

fun computeMonthlyEventDates(
    events: List<EventModel>,
    yearMonth: YearMonth
): Map<LocalDate, Int> {
    val monthStart = yearMonth.atDay(1)
    val monthEnd = yearMonth.atEndOfMonth()

    if (monthEnd < monthStart) return emptyMap()

    return (0..ChronoUnit.DAYS.between(monthStart, monthEnd))
        .map { monthStart.plusDays(it) }
        .associateWith { date -> events.count { event -> event.occursOn(date) } }
        .filter { (_, count) -> count > 0 }
}