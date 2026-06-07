package com.flux.other

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.flux.R

// MarkdownSegment.kt

sealed class MarkdownSegment {
    /** Regular markdown text (bold, italic, inline code, links, etc.) */
    data class Text(val content: String) : MarkdownSegment()

    /** A fenced code block */
    data class FencedCode(val info: String, val content: String) : MarkdownSegment()

    /**
     * All media references found in the note — rendered as chips at the bottom of the card.
     * Never emitted as an inline segment; extracted globally by [extractMedia].
     */
    data class MediaGroup(
        val images: List<String>,
        val videos: List<String>,
        val audio: List<String>
    ) {
        val isEmpty get() = images.isEmpty() && videos.isEmpty() && audio.isEmpty()
    }
}

// ---------------------------------------------------------------------------
// Extension sets
// ---------------------------------------------------------------------------

private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "svg", "bmp", "heic")
private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "mov", "avi", "webm", "3gp", "m4v")
private val AUDIO_EXTENSIONS = setOf("mp3", "aac", "ogg", "wav", "flac", "m4a", "opus", "wma")

// Markdown image:  ![alt](url)
private val MD_IMAGE_REGEX = Regex("""!\[[^\]]*]\(([^)]+)\)""")

// Markdown link:  [text](url)  — classified by URL extension
private val MD_LINK_REGEX = Regex("""\[[^\]]*]\(([^)]+)\)""")

// HTML video tag:  <video src="..."  or  <video src='...'
private val HTML_VIDEO_REGEX = Regex("""<video[^>]+src=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)

// HTML audio tag:  <audio src="..."  or  <audio src='...'
private val HTML_AUDIO_REGEX = Regex("""<audio[^>]+src=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)

// HTML img tag:  <img src="..."  or  <img src='...'
private val HTML_IMG_REGEX = Regex("""<img[^>]+src=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)

// Closing tags to strip alongside the opening tags
private val HTML_VIDEO_CLOSE = Regex("""</video\s*>""", RegexOption.IGNORE_CASE)
private val HTML_AUDIO_CLOSE = Regex("""</audio\s*>""", RegexOption.IGNORE_CASE)

// Bare https?:// URL not inside ()  — classified by extension
private val BARE_URL_REGEX = Regex("""(?<!\()https?://\S+""")

private val FENCE_REGEX = Regex(
    """^(`{3,}|~{3,})([\w\-+#.]*)[ \t]*\r?\n(.*?)\r?\n?\1[ \t]*$""",
    setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)
)

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun String.urlExtension() =
    substringAfterLast('.', "").lowercase().substringBefore('?').substringBefore('#').trim()

private enum class MediaType { IMAGE, VIDEO, AUDIO }

private fun classifyUrl(url: String): MediaType? {
    val ext = url.trim().urlExtension()
    return when (ext) {
        in IMAGE_EXTENSIONS -> MediaType.IMAGE
        in VIDEO_EXTENSIONS -> MediaType.VIDEO
        in AUDIO_EXTENSIONS -> MediaType.AUDIO
        else -> null
    }
}

// ---------------------------------------------------------------------------
// Global media extraction  (call this separately from segmentMarkdown)
// ---------------------------------------------------------------------------

/**
 * Scans the entire raw [text] for every media reference — markdown, HTML tags,
 * and bare URLs — and returns a [MarkdownSegment.MediaGroup] plus a cleaned
 * copy of the text with all media syntax removed.
 *
 * This is intentionally separate from [segmentMarkdown] so the caller
 * (e.g. NotesPreviewCard) can render chips wherever it wants (e.g. card bottom)
 * rather than inline inside the text column.
 */
data class MediaExtractionResult(
    val cleanedText: String,
    val media: MarkdownSegment.MediaGroup
)

fun extractMedia(text: String): MediaExtractionResult {
    val images = mutableListOf<String>()
    val videos = mutableListOf<String>()
    val audio  = mutableListOf<String>()
    val stripRanges = mutableListOf<IntRange>()

    fun addImage(url: String, range: IntRange) { images += url; stripRanges += range }
    fun addVideo(url: String, range: IntRange) { videos += url; stripRanges += range }
    fun addAudio(url: String, range: IntRange) { audio  += url; stripRanges += range }

    // 1. HTML <video src="..."></video>
    HTML_VIDEO_REGEX.findAll(text).forEach { m ->
        addVideo(m.groupValues[1].trim(), m.range)
    }
    // strip </video> closing tags
    HTML_VIDEO_CLOSE.findAll(text).forEach { m -> stripRanges += m.range }

    // 2. HTML <audio src="..."></audio>
    HTML_AUDIO_REGEX.findAll(text).forEach { m ->
        addAudio(m.groupValues[1].trim(), m.range)
    }
    HTML_AUDIO_CLOSE.findAll(text).forEach { m -> stripRanges += m.range }

    // 3. HTML <img src="...">
    HTML_IMG_REGEX.findAll(text).forEach { m ->
        addImage(m.groupValues[1].trim(), m.range)
    }

    // 4. Markdown images  ![alt](url)  — always image
    MD_IMAGE_REGEX.findAll(text).forEach { m ->
        if (stripRanges.any { it.contains(m.range.first) }) return@forEach
        addImage(m.groupValues[1].trim(), m.range)
    }

    // 5. Markdown links  [text](url)  — classify by extension
    MD_LINK_REGEX.findAll(text).forEach { m ->
        if (stripRanges.any { it.contains(m.range.first) }) return@forEach
        val url = m.groupValues[1].trim()
        when (classifyUrl(url)) {
            MediaType.IMAGE -> addImage(url, m.range)
            MediaType.VIDEO -> addVideo(url, m.range)
            MediaType.AUDIO -> addAudio(url, m.range)
            null -> { /* plain link — leave it */ }
        }
    }

    // 6. Bare URLs — classify by extension
    BARE_URL_REGEX.findAll(text).forEach { m ->
        if (stripRanges.any { it.contains(m.range.first) }) return@forEach
        val url = m.value.trim()
        when (classifyUrl(url)) {
            MediaType.IMAGE -> addImage(url, m.range)
            MediaType.VIDEO -> addVideo(url, m.range)
            MediaType.AUDIO -> addAudio(url, m.range)
            null -> { /* plain URL — leave it */ }
        }
    }

    // Build cleaned text by merging + removing strip ranges
    val merged = stripRanges.sortedBy { it.first }.fold(mutableListOf<IntRange>()) { acc, r ->
        if (acc.isEmpty() || acc.last().last < r.first - 1) acc.add(r)
        else acc[acc.lastIndex] = acc.last().first..maxOf(acc.last().last, r.last)
        acc
    }

    var cleaned = text
    var offset = 0
    for (r in merged) {
        val start = (r.first - offset).coerceAtLeast(0)
        val end   = (r.last  - offset + 1).coerceAtMost(cleaned.length)
        if (start < end) {
            cleaned = cleaned.removeRange(start, end)
            offset += end - start
        }
    }

    return MediaExtractionResult(
        cleanedText = cleaned.trim(),
        media = MarkdownSegment.MediaGroup(images, videos, audio)
    )
}

// ---------------------------------------------------------------------------
// Segment splitter  (fenced code blocks only — media handled separately above)
// ---------------------------------------------------------------------------

fun segmentMarkdown(text: String): List<MarkdownSegment> {
    val segments = mutableListOf<MarkdownSegment>()
    var lastEnd = 0

    for (match in FENCE_REGEX.findAll(text)) {
        if (match.range.first > lastEnd) {
            val before = text.substring(lastEnd, match.range.first)
            if (before.isNotBlank()) segments.add(MarkdownSegment.Text(before))
        }
        segments.add(MarkdownSegment.FencedCode(match.groupValues[2].trim(), match.groupValues[3]))
        lastEnd = match.range.last + 1
    }

    if (lastEnd < text.length) {
        val remaining = text.substring(lastEnd)
        if (remaining.isNotBlank()) segments.add(MarkdownSegment.Text(remaining))
    }

    if (segments.isEmpty()) segments.add(MarkdownSegment.Text(text))
    return segments
}

fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        val uri = url.toUri().let { parsed ->
            // Ensure the URL has a scheme so the browser can handle it
            if (parsed.scheme.isNullOrBlank()) "https://$url".toUri() else parsed
        }
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

@Composable
fun MarkdownBlock(
    text: String,
    modifier: Modifier = Modifier,
    linkColor: Color = Color(0xFF4A90D9),
    codeBlockBackground: Color = MaterialTheme.colorScheme.surfaceVariant,
    codeBlockTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onLinkClick: ((String) -> Unit)? = null,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val handleLink: (String) -> Unit = onLinkClick ?: { url -> openUrl(context, url) }

    // Strip media from text before segmenting
    val extracted = remember(text) { extractMedia(text) }
    val segments  = remember(extracted.cleanedText) { segmentMarkdown(extracted.cleanedText) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        segments.forEach { segment ->
            when (segment) {
                is MarkdownSegment.Text -> {
                    val annotated = parseMarkdownContent(segment.content, linkColor)
                    LinkAwareText(
                        annotated = annotated,
                        onLinkClick = handleLink,
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                }

                is MarkdownSegment.FencedCode -> {
                    FencedCodeBlock(
                        info = segment.info,
                        content = segment.content,
                        background = codeBlockBackground,
                        textColor = codeBlockTextColor,
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                }
            }
        }
    }
}

@Composable
fun MediaChipsRow(
    media: MarkdownSegment.MediaGroup,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    if (media.isEmpty) return

    FlowRow (
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (media.images.isNotEmpty()) {
            MediaChip(icon = Icons.Default.Image,     label = stringResource(R.string.image), count = media.images.size)
        }
        if (media.videos.isNotEmpty()) {
            MediaChip(icon = Icons.Default.VideoFile, label = stringResource(R.string.video), count = media.videos.size)
        }
        if (media.audio.isNotEmpty()) {
            MediaChip(icon = Icons.Default.Audiotrack, label = stringResource(R.string.audio), count = media.audio.size)
        }
    }
}

@Composable
private fun MediaChip(icon: ImageVector, label: String, count: Int) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = if (count > 1) "$label × $count" else label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun LinkAwareText(
    annotated: AnnotatedString,
    onLinkClick: (String) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onBackground
        ),
        onTextLayout = { textLayoutResult = it },
        modifier = Modifier.pointerInput(annotated) {
            detectTapGestures(
                onTap = { offset ->
                    val layout = textLayoutResult ?: run { onClick(); return@detectTapGestures }
                    val charOffset = layout.getOffsetForPosition(offset)
                    val link = annotated
                        .getStringAnnotations(URL_ANNOTATION_TAG, charOffset, charOffset)
                        .firstOrNull()
                    if (link != null) onLinkClick(link.item) else onClick()
                },
                onLongPress = { onLongClick() }
            )
        }
    )
}

@Composable
private fun FencedCodeBlock(
    info: String,
    content: String,
    background: Color,
    textColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        shape = RoundedCornerShape(8.dp),
        color = background,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (info.isNotBlank()) {
                Text(
                    text = info,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .align(Alignment.End)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = content,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = textColor,
                        lineHeight = 20.sp
                    )
                )
            }
        }
    }
}