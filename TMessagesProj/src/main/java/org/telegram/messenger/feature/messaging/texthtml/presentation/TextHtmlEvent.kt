package org.telegram.messenger.feature.messaging.texthtml.presentation

import org.telegram.messenger.feature.messaging.texthtml.domain.model.RichFormattedText

/**
 * MVI Events for rich text and HTML manipulation.
 */
sealed class TextHtmlEvent {
    data class ConvertToHtml(val text: RichFormattedText) : TextHtmlEvent()
    data class ParseHtml(val html: String) : TextHtmlEvent()
    data class EscapeText(val rawText: String) : TextHtmlEvent()
    data class StripFormatting(val html: String) : TextHtmlEvent()
    object ClearState : TextHtmlEvent()
    object DismissError : TextHtmlEvent()
}
