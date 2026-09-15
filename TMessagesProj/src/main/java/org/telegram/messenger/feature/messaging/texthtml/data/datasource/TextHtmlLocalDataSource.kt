package org.telegram.messenger.feature.messaging.texthtml.data.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.messaging.texthtml.data.mapper.TextHtmlMapper
import org.telegram.messenger.feature.messaging.texthtml.domain.model.HtmlTextSpanType
import org.telegram.messenger.feature.messaging.texthtml.domain.model.RichFormattedText
import org.telegram.messenger.feature.messaging.texthtml.domain.model.TextHtmlState

/**
 * Локальный источник данных для конвертации и кэширования форматированного текста и HTML.
 */
class TextHtmlLocalDataSource(
    private val currentAccount: Int = 0
) {

    private val lock = Any()
    private val _stateFlow = MutableStateFlow(TextHtmlState())
    val stateFlow: StateFlow<TextHtmlState> = _stateFlow.asStateFlow()

    fun convertToHtml(text: RichFormattedText): String {
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

    fun parseFromHtml(html: String): RichFormattedText {
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

    fun escapeHtml(rawText: String): String = TextHtmlMapper.escape(rawText)

    fun unescapeHtml(escapedHtml: String): String = TextHtmlMapper.unescape(escapedHtml)

    fun stripFormatting(html: String): String = TextHtmlMapper.stripHtml(html)

    fun clearState() = synchronized(lock) {
        _stateFlow.value = TextHtmlState()
    }
}
