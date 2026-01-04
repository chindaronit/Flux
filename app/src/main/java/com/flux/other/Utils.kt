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
import androidx.compose.runtime.remember
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

private fun shareAsImage(
    context: Context,
    webView: WebView,
    noteName: String) {
    // temp file
    val file = File(
        context.cacheDir,
        "${noteName}_${System.currentTimeMillis()}_preview.jpg"
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

private fun sanitizeFileName(name: String): String {
    return name
        .trim()
        .replace(Regex("""[\\/:*?"<>|]"""), "_")
        .replace(Regex("""\s+"""), " ")
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
