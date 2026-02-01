package com.flux.other

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.PlatformParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

object Constants {
    object File {
        const val FLUX = "Flux"
        const val FLUX_IMAGES = "Images"
        const val FLUX_AUDIO = "Audio"
        const val FLUX_VIDEOS = "Videos"
    }

    object Editor {
        const val UNDO = "undo"
        const val REDO = "redo"

        const val H1 = "h1"
        const val H2 = "h2"
        const val H3 = "h3"
        const val H4 = "h4"
        const val H5 = "h5"
        const val H6 = "h6"

        const val BOLD = "bold"
        const val ITALIC = "italic"
        const val UNDERLINE = "underline"
        const val STRIKETHROUGH = "strikethrough"
        const val MARK = "mark"

        const val INLINE_CODE = "inlineCode"
        const val INLINE_BRACKETS = "inlineBrackets"
        const val INLINE_BRACES = "inlineBraces"
        const val INLINE_MATH = "inlineMath"

        const val TABLE = "table"
        const val TASK = "task"
        const val LIST = "list"
        const val QUOTE = "quote"
        const val NOTE = "note"
        const val TIP = "tip"
        const val IMPORTANT = "important"
        const val WARNING = "warning"
        const val CAUTION = "caution"
        const val TAB = "tab"
        const val UN_TAB = "unTab"
        const val RULE = "rule"
        const val DIAGRAM = "diagram"

        const val TEXT = "text"
    }
}

enum class  ExportType {
    TXT,
    MARKDOWN,
    HTML,
    IMAGE,
    PDF
}

object Properties {
    // Regex pattern to extract the YAML block between "---" markers at the beginning of a note
    private val YAML_BLOCK_PATTERN =
        "\\A---\\s*\\n([\\s\\S]*?)\\n---".toRegex(RegexOption.MULTILINE)

    fun String.splitPropertiesAndContent(): Pair<String, String> {
        val matchResult = YAML_BLOCK_PATTERN.find(this)

        return if (matchResult != null) {
            val propertiesWithDelimiters = this.substring(matchResult.range)
            val content = this.substring(matchResult.range.last + 1).trim()

            Pair(propertiesWithDelimiters, content)
        } else {
            Pair("", this)
        }
    }
}

val REGEX_TASK_LIST_ITEM = "^\\[([xX\\s])]\\s+(.*)".toRegex()

val SYMBOL_STYLE = SpanStyle(
    fontWeight = FontWeight.Light,
    fontStyle = FontStyle.Normal,
    fontSize = 0.sp,
    color = Color.Gray,
    textDecoration = TextDecoration.None,
    fontFamily = FontFamily.Default,
    background = Color.Transparent
)

val BOLD_STYLE = SpanStyle(
    fontWeight = FontWeight.Bold,
    fontSynthesis = FontSynthesis.Weight
)

val ITALIC_STYLE = SpanStyle(
    fontStyle = FontStyle.Italic,
    fontSynthesis = FontSynthesis.Style
)

val BOLD_ITALIC_STYLE = SpanStyle(
    fontWeight = FontWeight.Bold,
    fontStyle = FontStyle.Italic,
    fontSynthesis = FontSynthesis.All
)

val STRIKETHROUGH_STYLE = SpanStyle(
    textDecoration = TextDecoration.LineThrough
)

val UNDERLINE_STYLE = SpanStyle(
    textDecoration = TextDecoration.Underline
)

val STRIKETHROUGH_AND_UNDERLINE_STYLE = SpanStyle(
    textDecoration = TextDecoration.combine(
        listOf(
            TextDecoration.LineThrough,
            TextDecoration.Underline
        )
    )
)

val HIGHLIGHT_STYLE = SpanStyle(
    color = Color.Black,
    background = Color.Yellow.copy(alpha = 1f)
)

val CODE_STYLE = SpanStyle(
    fontFamily = FontFamily.Monospace,
    background = Color.LightGray.copy(alpha = 0.3f)
)

val CODE_BLOCK_STYLE = SpanStyle(fontFamily = FontFamily.Monospace)
val MARKER_STYLE = SpanStyle(color = Color(0xFFCE8D6E), fontFamily = FontFamily.Monospace)
val KEYWORD_STYLE = SpanStyle(color = Color(0xFFC67CBA))

val HEADER_LINE_STYLES = listOf(
    ParagraphStyle(
        lineHeight = 2.em,
        platformStyle = PlatformParagraphStyle.Default,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        )
    ), ParagraphStyle(
        lineHeight = 1.5.em,
        platformStyle = PlatformParagraphStyle.Default,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        )
    ), ParagraphStyle(
        lineHeight = 1.17.em,
        platformStyle = PlatformParagraphStyle.Default,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        )
    ), ParagraphStyle(
        lineHeight = 1.em,
        platformStyle = PlatformParagraphStyle.Default,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        )
    ), ParagraphStyle(
        lineHeight = 0.83.em,
        platformStyle = PlatformParagraphStyle.Default,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        )
    ), ParagraphStyle(
        lineHeight = 0.75.em,
        platformStyle = PlatformParagraphStyle.Default,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        )
    )
)

val HEADER_STYLES = listOf(
    SpanStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Black,
        fontSynthesis = FontSynthesis.Weight
    ), SpanStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.ExtraBold,
        fontSynthesis = FontSynthesis.Weight
    ), SpanStyle(
        fontSize = 18.72.sp,
        fontWeight = FontWeight.ExtraBold,
        fontSynthesis = FontSynthesis.Weight
    ), SpanStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        fontSynthesis = FontSynthesis.Weight
    ), SpanStyle(
        fontSize = 13.28.sp,
        fontWeight = FontWeight.Bold,
        fontSynthesis = FontSynthesis.Weight
    ), SpanStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        fontSynthesis = FontSynthesis.Weight
    )
)

data class StyleRanges(
    val codeRanges: List<IntRange>,
    val boldRanges: List<IntRange>,
    val italicRanges: List<IntRange>,
    val boldItalicRanges: List<IntRange>,
    val strikethroughRanges: List<IntRange>,
    val underlineRanges: List<IntRange>,
    val highlightRanges: List<IntRange>,
    val headerRanges: List<Pair<IntRange, Int>>,
    val markerRanges: List<IntRange>,
    val linkRanges: List<IntRange>,
    val fencedCodeBlockInfoRanges: List<IntRange>,
    val codeBlockContentRanges: List<IntRange>
) {
    companion object {
        val EMPTY = StyleRanges(
            emptyList(), emptyList(), emptyList(),
            emptyList(), emptyList(), emptyList(),
            emptyList(), emptyList(), emptyList(),
            emptyList(), emptyList(), emptyList()
        )
    }
}

fun IntRange.overlaps(other: IntRange): Boolean {
    return this.first <= other.last && other.first <= this.last
}