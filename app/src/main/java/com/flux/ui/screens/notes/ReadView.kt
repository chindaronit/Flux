package com.flux.ui.screens.notes

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebView.enableSlowWholeDocumentDraw
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.flux.other.Constants
import com.flux.other.MediaCache
import com.flux.other.rememberCustomTabsIntent
import com.flux.other.toHexColor
import com.flux.ui.theme.FLUX_WEB_FONTS
import com.flux.ui.theme.FontResourceMap
import com.flux.ui.theme.toFontFaceCss
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

data class MarkdownStyles(
    val hexTextColor: String,
    val hexCodeBackgroundColor: String,
    val hexPreBackgroundColor: String,
    val hexQuoteBackgroundColor: String,
    val hexLinkColor: String,
    val hexBorderColor: String,
    val backgroundColor: Int,
    val fontFamilyCss: String,
    val fontFaceCss: String      // the generated @font-face block, empty string for SYSTEM
) {
    companion object {
        private val systemFallback =
            "-apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif"

        fun fromColorScheme(colorScheme: ColorScheme, fontNumber: Int) = MarkdownStyles(
            hexTextColor = colorScheme.onSurface.toArgb().toHexColor(),
            hexCodeBackgroundColor = colorScheme.surfaceVariant.toArgb().toHexColor(),
            hexPreBackgroundColor = colorScheme.surfaceVariant.toArgb().toHexColor(),
            hexQuoteBackgroundColor = colorScheme.secondaryContainer.toArgb().toHexColor(),
            hexLinkColor = colorScheme.primary.toArgb().toHexColor(),
            hexBorderColor = colorScheme.outline.toArgb().toHexColor(),
            backgroundColor = colorScheme.surfaceContainerLow.toArgb(),
            fontFamilyCss = FLUX_WEB_FONTS.getOrNull(fontNumber)?.let { "'${it.familyName}', $systemFallback" }
                ?: systemFallback,
            fontFaceCss = FLUX_WEB_FONTS.getOrNull(fontNumber)?.toFontFaceCss() ?: ""
        )
    }
}

@SuppressLint("SetJavaScriptEnabled", "FrequentlyChangingValue")
@Composable
fun ReadView(
    modifier: Modifier = Modifier,
    fontNumber: Int,
    html: String,
    rootUri: String?,
    scrollState: ScrollState,
    isAppInDarkMode: Boolean=false,
    onWebViewReady: (WebView)->Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val markdownStyles = remember(colorScheme, fontNumber) { MarkdownStyles.fromColorScheme(colorScheme, fontNumber) }
    var template by rememberSaveable { mutableStateOf("") }
    val customTabsIntent = rememberCustomTabsIntent()
    val coroutineScope = rememberCoroutineScope()
    var webView by remember { mutableStateOf<WebView?>(null) }
    val data by remember(html, markdownStyles, isAppInDarkMode, template) {
        mutableStateOf(processHtml(html, markdownStyles, isAppInDarkMode, template))
    }
    var imagesDir by remember { mutableStateOf<DocumentFile?>(null) }
    var audioDir by remember { mutableStateOf<DocumentFile?>(null) }
    var videosDir by remember { mutableStateOf<DocumentFile?>(null) }
    var documentsDir by remember { mutableStateOf<DocumentFile?>(null) }
    val openAttachment: (String) -> Unit = { fileName ->
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val file = documentsDir?.listFiles()?.firstOrNull { it.name == fileName }
                    ?: throw IllegalStateException("Attachment not found: $fileName")
                withContext(Dispatchers.Main) {
                    val intent = Intent(Intent.ACTION_VIEW)
                        .setDataAndType(file.uri, file.type ?: "application/octet-stream")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    try {
                        context.startActivity(Intent.createChooser(intent, fileName))
                    } catch (exception: ActivityNotFoundException) {
                        Toast.makeText(context, "No app can open $fileName", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, exception.localizedMessage ?: "Could not open $fileName", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            template = try {
                context.assets.open("template.html").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }
    }

    LaunchedEffect(scrollState.value) {
        val totalHeight = scrollState.maxValue
        val currentScrollPercent = when {
            totalHeight <= 0 -> 0f
            scrollState.value >= totalHeight -> 1f
            else -> (scrollState.value.toFloat() / totalHeight).coerceIn(0f, 1f)
        }

        webView?.evaluateJavascript(
            """
        (function() {
            const d = document.documentElement;
            const b = document.body;
            const maxHeight = Math.max(
                d.scrollHeight, d.offsetHeight, d.clientHeight,
                b.scrollHeight, b.offsetHeight
            );
            window.scrollTo({ 
                top: maxHeight * $currentScrollPercent, 
                behavior: 'auto' 
            });
        })();
        """.trimIndent(),
            null
        )
    }

    LaunchedEffect(rootUri) {
        if(rootUri!=null){
            withContext(Dispatchers.IO) {
                MediaCache.clearCaches()
                imagesDir = try {
                    DocumentFile.fromTreeUri(context.applicationContext, rootUri.toUri())
                        ?.findFile(Constants.File.FLUX)
                        ?.findFile(Constants.File.FLUX_IMAGES)
                } catch (_: Exception) {
                    null
                }
                audioDir = try {
                    DocumentFile.fromTreeUri(context.applicationContext, rootUri.toUri())
                        ?.findFile(Constants.File.FLUX)
                        ?.findFile(Constants.File.FLUX_AUDIO)
                } catch (_: Exception) {
                    null
                }
                videosDir = try {
                    DocumentFile.fromTreeUri(context.applicationContext, rootUri.toUri())
                        ?.findFile(Constants.File.FLUX)
                        ?.findFile(Constants.File.FLUX_VIDEOS)
                } catch (_: Exception) {
                    null
                }
                documentsDir = try {
                    DocumentFile.fromTreeUri(context.applicationContext, rootUri.toUri())
                        ?.findFile(Constants.File.FLUX)
                        ?.findFile(Constants.File.FLUX_DOCUMENTS)
                } catch (_: Exception) {
                    null
                }
                withContext(Dispatchers.Main) {
                    webView?.evaluateJavascript(
                        """
        (function() {
            if (handlers && typeof handlers.processMediaItems === 'function') {
                handlers.processMediaItems();
            }
        })();
        """.trimIndent(), null
                    )
                }
            }
        }
    }

    AndroidView(
        modifier = modifier.clipToBounds(),
        factory = {
            WebView(it).also { wv ->
                webView = wv
                onWebViewReady(wv)
            }.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest
                    ): Boolean {
                        val uri = request.url
                        if (uri.scheme == "https" && uri.host == "flux.local") {
                            val segments = uri.pathSegments
                            if (segments.size == 2 && segments[0] in setOf("document", "pdf")) {
                                openAttachment(Uri.decode(segments[1]))
                            }
                            return true
                        }
                        return if ((uri.scheme == "http" || uri.scheme == "https") && uri.host != "appassets.flux") {
                            customTabsIntent.launchUrl(it, uri)
                            true
                        } else {
                            false
                        }
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        val url = request.url
                        if (url.scheme == "https" && url.host == "appassets.flux" &&
                            url.path?.startsWith("/fonts/") == true
                        ) {
                            val fileName = url.lastPathSegment.orEmpty()
                            val resId = FontResourceMap.byFileName[fileName]
                                ?: return super.shouldInterceptRequest(view, request)
                            return try {
                                val stream = context.resources.openRawResource(resId)
                                WebResourceResponse("font/ttf", null, stream)
                            } catch (_: Exception) {
                                super.shouldInterceptRequest(view, request)
                            }
                        }
                        return super.shouldInterceptRequest(view, request)
                    }
                }
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun processMedia(mediaName: String, id: String, mediaType: String) {
                            coroutineScope.launch(Dispatchers.IO) {
                                when (mediaType) {
                                    "image" -> {
                                        // Check cache first for images
                                        MediaCache.getImageUri(mediaName)?.let { uri ->
                                            updateImageInWebView(id, uri)
                                            return@launch
                                        }

                                        val file =
                                            imagesDir?.listFiles()?.find { it.name == mediaName }
                                        val uri = file?.uri?.toString().orEmpty()
                                        if (uri.isNotEmpty()) {
                                            MediaCache.cacheImageUri(mediaName, uri)
                                            updateImageInWebView(id, uri)
                                        }
                                    }

                                    "video" -> {
                                        val file =
                                            videosDir?.listFiles()?.find { it.name == mediaName }
                                        val uri = file?.uri ?: return@launch
                                        val uriString = uri.toString()

                                        // Get thumbnail from cache or generate new one
                                        val thumbnail =
                                            MediaCache.getVideoThumbnail(mediaName) ?: run {
                                                val newThumbnail =
                                                    MediaCache.generateVideoThumbnail(context, uri)
                                                if (newThumbnail.isNotEmpty()) {
                                                    MediaCache.cacheVideoThumbnail(
                                                        mediaName,
                                                        newThumbnail
                                                    )
                                                }
                                                newThumbnail
                                            }

                                        updateVideoInWebView(id, uriString, thumbnail)
                                    }

                                    "audio" -> {
                                        val file =
                                            audioDir?.listFiles()?.find { it.name == mediaName }
                                        val uri = file?.uri?.toString().orEmpty()
                                        if (uri.isNotEmpty()) {
                                            updateAudioInWebView(id, uri)
                                        }
                                    }
                                }
                            }
                        }

                        @JavascriptInterface
                        fun processPdf(fileName: String, id: String) {
                            coroutineScope.launch(Dispatchers.IO) {
                                val file = documentsDir?.listFiles()?.firstOrNull { it.name == fileName }
                                if (file == null) {
                                    Log.w("ReadView", "PDF attachment not found: $fileName")
                                    withContext(Dispatchers.Main) {
                                        webView?.evaluateJavascript(
                                            """document.querySelector('img[data-id="$id"]')?.removeAttribute('data-pdf-requested');""",
                                            null
                                        )
                                    }
                                    return@launch
                                }
                                try {
                                    renderPdfPages(context, file) { pageNumber, pageCount, image ->
                                        withContext(Dispatchers.Main) {
                                            webView?.evaluateJavascript(
                                                """
                                                (function() {
                                                    const placeholder = document.querySelector('img[data-id="$id"]');
                                                    if (!placeholder) return;
                                                    const pageNumber = $pageNumber;
                                                    if (document.querySelector('img[data-pdf-owner="$id"][data-pdf-page="' + pageNumber + '"]')) return;
                                                    let page;
                                                    if (pageNumber === 0) {
                                                        page = placeholder;
                                                        page.dataset.pdfOwner = "$id";
                                                    } else {
                                                        page = document.createElement('img');
                                                        page.dataset.pdfOwner = "$id";
                                                        page.className = 'pdf-page';
                                                        const renderedPages = document.querySelectorAll('img[data-pdf-owner="$id"]');
                                                        const lastPage = renderedPages.length ? renderedPages[renderedPages.length - 1] : placeholder;
                                                        lastPage.insertAdjacentElement('afterend', page);
                                                    }
                                                    page.dataset.pdfPage = String(pageNumber);
                                                    page.dataset.pdfRequested = 'true';
                                                    page.alt = 'PDF page ' + (pageNumber + 1) + ' of $pageCount';
                                                    page.src = 'data:image/jpeg;base64,$image';
                                                })();
                                                """.trimIndent(),
                                                null
                                            )
                                        }
                                    }
                                } catch (exception: CancellationException) {
                                    throw exception
                                } catch (exception: Exception) {
                                    Log.e("ReadView", "Could not render PDF preview: $fileName", exception)
                                }
                            }
                        }

                        @JavascriptInterface
                        fun openDocument(fileName: String) {
                            coroutineScope.launch(Dispatchers.Main) {
                                openAttachment(fileName)
                            }
                        }

                        private suspend fun updateImageInWebView(id: String, uri: String) {
                            withContext(Dispatchers.Main) {
                                webView?.evaluateJavascript(
                                    """
                    (function() {
                        const img = document.querySelector('img[data-id="$id"]');
                        if (img) img.src = '$uri';
                    })();
                    """.trimIndent(), null
                                )
                            }
                        }

                        private suspend fun updateVideoInWebView(
                            id: String,
                            uri: String,
                            thumbnail: String
                        ) {
                            withContext(Dispatchers.Main) {
                                webView?.evaluateJavascript(
                                    """
                    (function() {
                        const video = document.querySelector('video[data-id="$id"]');
                        if (video) {
                            video.src = '$uri';
                            video.poster = '$thumbnail';
                        }
                    })();
                    """.trimIndent(), null
                                )
                            }
                        }

                        private suspend fun updateAudioInWebView(id: String, uri: String) {
                            withContext(Dispatchers.Main) {
                                webView?.evaluateJavascript(
                                    """
                    (function() {
                        const audio = document.querySelector('audio[data-id="$id"]');
                        if (audio) audio.src = '$uri';
                    })();
                    """.trimIndent(), null
                                )
                            }
                        }
                    },
                    "mediaPathHandler"
                )
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.allowFileAccessFromFileURLs = true
                settings.allowUniversalAccessFromFileURLs = true
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                settings.domStorageEnabled = true
                settings.javaScriptEnabled = true
                settings.loadsImagesAutomatically = true
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                settings.useWideViewPort = false
                settings.loadWithOverviewMode = false
                enableSlowWholeDocumentDraw()
            }
        },
        update = {
            it.setBackgroundColor(markdownStyles.backgroundColor)
            it.loadDataWithBaseURL(
                "file:///android_asset/",
                data,
                "text/html",
                "UTF-8",
                null
            )
        },
        onReset = {
            it.clearHistory()
            it.stopLoading()
            it.destroy()
            webView = null
        })
}

private suspend fun renderPdfPages(
    context: android.content.Context,
    file: DocumentFile,
    onPageRendered: suspend (pageNumber: Int, pageCount: Int, image: String) -> Unit
) {
    val descriptor = context.contentResolver.openFileDescriptor(file.uri, "r")
        ?: throw IllegalStateException("Could not read PDF ${file.name}")
    descriptor.use { parcelFileDescriptor ->
        PdfRenderer(parcelFileDescriptor).use { renderer ->
            if (renderer.pageCount == 0) throw IllegalStateException("PDF has no pages")
            val pageCount = renderer.pageCount
            for (pageNumber in 0 until pageCount) {
                renderer.openPage(pageNumber).use { page ->
                    val scale = min(1f, min(1000f / page.width, 1400f / page.height))
                    val bitmap = Bitmap.createBitmap(
                        max(1, (page.width * scale).toInt()),
                        max(1, (page.height * scale).toInt()),
                        Bitmap.Config.ARGB_8888
                    )
                    val encodedPage = try {
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        ByteArrayOutputStream().use { output ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, output)
                            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                        }
                    } finally {
                        bitmap.recycle()
                    }
                    onPageRendered(pageNumber, pageCount, encodedPage)
                }
            }
        }
    }
}

private fun processHtml(
    html: String,
    markdownStyles: MarkdownStyles,
    isAppInDarkMode: Boolean,
    template: String
): String {
    return template
        .replace("{{CONTENT}}", html)
        .replace("{{TEXT_COLOR}}", markdownStyles.hexTextColor)
        .replace("{{BACKGROUND_COLOR}}", markdownStyles.backgroundColor.toHexColor())
        .replace("{{CODE_BACKGROUND}}", markdownStyles.hexCodeBackgroundColor)
        .replace("{{PRE_BACKGROUND}}", markdownStyles.hexPreBackgroundColor)
        .replace("{{QUOTE_BACKGROUND}}", markdownStyles.hexQuoteBackgroundColor)
        .replace("{{LINK_COLOR}}", markdownStyles.hexLinkColor)
        .replace("{{BORDER_COLOR}}", markdownStyles.hexBorderColor)
        .replace("{{FONT_FACE_CSS}}", markdownStyles.fontFaceCss)
        .replace("{{FONT_FAMILY}}", markdownStyles.fontFamilyCss)
        .replace("{{COLOR_SCHEME}}", if (isAppInDarkMode) "dark" else "light")
}