package org.telegram.messenger.feature.messaging.texthtml.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.messaging.texthtml.domain.model.RichFormattedText
import org.telegram.messenger.feature.messaging.texthtml.domain.model.TextHtmlState

/**
 * Domain contract for rich text and HTML conversion, parsing, and formatting.
 */
interface TextHtmlRepository {

    /**
     * Converts domain rich formatted text into standard Telegram HTML string.
     */
    fun convertToHtml(text: RichFormattedText): String

    /**
     * Parses an HTML string into domain [RichFormattedText] representation.
     */
    fun parseFromHtml(html: String): RichFormattedText

    /**
     * Escapes raw plain text into HTML safe text (&, <, >, quotes, etc.).
     */
    fun escapeHtml(rawText: String): String

    /**
     * Unescapes HTML entities back to raw text.
     */
    fun unescapeHtml(escapedHtml: String): String

    /**
     * Strips all HTML markup tags from the provided HTML, leaving only text.
     */
    fun stripFormatting(html: String): String

    /**
     * Reactive StateFlow stream of the Text & HTML conversion state.
     */
    fun observeState(): StateFlow<TextHtmlState>

    /**
     * Clears the current conversion cache and state.
     */
    fun clearState()
}
