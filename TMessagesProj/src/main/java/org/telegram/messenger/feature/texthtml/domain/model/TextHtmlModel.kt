package org.telegram.messenger.feature.texthtml.domain.model

/**
 * Types of formatting spans supported by Telegram Rich Text & HTML conversion.
 */
enum class HtmlTextSpanType {
    BOLD,
    ITALIC,
    UNDERLINE,
    STRIKE,
    SPOILER,
    MONO,
    CODE,
    QUOTE,
    COLLAPSED_QUOTE,
    CUSTOM_EMOJI,
    URL
}

/**
 * Domain representation of a formatting span within text.
 */
data class HtmlTextSpan(
    val type: HtmlTextSpanType,
    val start: Int,
    val end: Int,
    val url: String? = null,
    val language: String? = null,
    val documentId: Long? = null,
    val isCollapsed: Boolean = false
) {
    val length: Int get() = (end - start).coerceAtLeast(0)
}

/**
 * Domain representation of formatted rich text with spans.
 */
data class RichFormattedText(
    val plainText: String = "",
    val spans: List<HtmlTextSpan> = emptyList()
) {
    val isEmpty: Boolean get() = plainText.isEmpty()
    val length: Int get() = plainText.length
    val hasFormatting: Boolean get() = spans.isNotEmpty()
}

/**
 * Result of HTML conversion operation.
 */
data class TextHtmlConversionResult(
    val html: String,
    val formattedText: RichFormattedText,
    val hasFormatting: Boolean = formattedText.hasFormatting
)

/**
 * Aggregated reactive state of the Text & HTML conversion engine.
 */
data class TextHtmlState(
    val lastHtml: String? = null,
    val lastPlainText: String? = null,
    val spansCount: Int = 0,
    val hasCustomEmoji: Boolean = false,
    val hasCodeBlocks: Boolean = false,
    val hasQuotes: Boolean = false,
    val timestampMs: Long = 0L
)
