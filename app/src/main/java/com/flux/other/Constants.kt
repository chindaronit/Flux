package com.flux.other

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