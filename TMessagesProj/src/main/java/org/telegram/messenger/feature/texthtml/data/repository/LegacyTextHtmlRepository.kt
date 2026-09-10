package org.telegram.messenger.feature.texthtml.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.texthtml.data.mapper.TextHtmlMapper
import org.telegram.messenger.feature.texthtml.domain.model.HtmlTextSpanType
import org.telegram.messenger.feature.texthtml.domain.model.RichFormattedText
import org.telegram.messenger.feature.texthtml.domain.model.TextHtmlState
import org.telegram.messenger.feature.texthtml.domain.repository.TextHtmlRepository

/**
 * Thread-safe repository implementing [TextHtmlRepository].
 * Adapts legacy HTML conversion logic from [CustomHtml] and [CopyUtilities]
 * into a clean reactive boundary.
 */
class LegacyTextHtmlRepository : TextHtmlRepository {

    private val lock = Any()
    private val _stateFlow = MutableStateFlow(TextHtmlState())

    override fun convertToHtml(text: RichFormattedText): String {
        val html = TextHtmlMapper.toHtml(text)
        synchronized(lock) {
            _stateFlow.value = TextHtmlState(
                lastHtml = html,
                lastPlainText = text.plainText,
                spansCount = text.spans.size,
                hasCustomEmoji = text.spans.any { it.type == HtmlTextSpanType.CUSTOM_EMOJI },
                hasCodeBlocks = text.spans.any { it.type == HtmlTextSpanType.CODE || it.type == HtmlTextSpanType.MONO },
                hasQuotes = text.spans.any { it.type == HtmlTextSpanType.QUOTE || it.type == HtmlTextSpanType.COLLAPSED_QUOTE },
                timestampMs = System.currentTimeMillis()
            )
        }
        return html
    }

    override fun parseFromHtml(html: String): RichFormattedText {
        val parsed = TextHtmlMapper.parseHtml(html)
        synchronized(lock) {
            _stateFlow.value = TextHtmlState(
                lastHtml = html,
                lastPlainText = parsed.plainText,
                spansCount = parsed.spans.size,
                hasCustomEmoji = parsed.spans.any { it.type == HtmlTextSpanType.CUSTOM_EMOJI },
                hasCodeBlocks = parsed.spans.any { it.type == HtmlTextSpanType.CODE || it.type == HtmlTextSpanType.MONO },
                hasQuotes = parsed.spans.any { it.type == HtmlTextSpanType.QUOTE || it.type == HtmlTextSpanType.COLLAPSED_QUOTE },
                timestampMs = System.currentTimeMillis()
            )
        }
        return parsed
    }

    override fun escapeHtml(rawText: String): String {
        return TextHtmlMapper.escape(rawText)
    }

    override fun unescapeHtml(escapedHtml: String): String {
        return TextHtmlMapper.unescape(escapedHtml)
    }

    override fun stripFormatting(html: String): String {
        return TextHtmlMapper.stripHtml(html)
    }

    override fun observeState(): StateFlow<TextHtmlState> {
        return _stateFlow.asStateFlow()
    }

    override fun clearState() {
        synchronized(lock) {
            _stateFlow.value = TextHtmlState()
        }
    }
}
