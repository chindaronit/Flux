package com.flux.ui.screens.notes

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebView.enableSlowWholeDocumentDraw
import android.webkit.WebViewClient
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.flux.other.Constants
import com.flux.other.MediaCache
import com.flux.other.rememberCustomTabsIntent
import com.flux.other.toHexColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MarkdownStyles(
    val hexTextColor: String,
    val hexCodeBackgroundColor: String,
    val hexPreBackgroundColor: String,
    val hexQuoteBackgroundColor: String,
    val hexLinkColor: String,
    val hexBorderColor: String,
    val backgroundColor: Int
) {
    companion object {
        fun fromColorScheme(colorScheme: ColorScheme) = MarkdownStyles(
            hexTextColor = colorScheme.onSurface.toArgb().toHexColor(),
            hexCodeBackgroundColor = colorScheme.surfaceVariant.toArgb().toHexColor(),
            hexPreBackgroundColor = colorScheme.surfaceColorAtElevation(1.dp).toArgb().toHexColor(),
            hexQuoteBackgroundColor = colorScheme.secondaryContainer.toArgb().toHexColor(),
            hexLinkColor = colorScheme.primary.toArgb().toHexColor(),
            hexBorderColor = colorScheme.outline.toArgb().toHexColor(),
            backgroundColor = colorScheme.surfaceContainerLow.toArgb()
        )
    }
}

@SuppressLint("SetJavaScriptEnabled", "FrequentlyChangingValue")
@Composable
fun ReadView(
    modifier: Modifier = Modifier,
    html: String,
    rootUri: String?,
    scrollState: ScrollState,
    isAppInDarkMode: Boolean=false,
    onWebViewReady: (WebView)->Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val markdownStyles = remember(colorScheme) { MarkdownStyles.fromColorScheme(colorScheme) }
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
                        return if (uri.scheme == "http" || uri.scheme == "https") {
                            customTabsIntent.launchUrl(it, uri)
                            true
                        } else {
                            false
                        }
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
        .replace("{{COLOR_SCHEME}}", if (isAppInDarkMode) "dark" else "light")
}