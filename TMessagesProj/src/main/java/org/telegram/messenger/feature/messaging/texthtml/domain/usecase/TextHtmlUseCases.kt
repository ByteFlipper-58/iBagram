package org.telegram.messenger.feature.messaging.texthtml.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.messaging.texthtml.domain.model.HtmlTextSpan
import org.telegram.messenger.feature.messaging.texthtml.domain.model.RichFormattedText
import org.telegram.messenger.feature.messaging.texthtml.domain.model.TextHtmlState
import org.telegram.messenger.feature.messaging.texthtml.domain.repository.TextHtmlRepository

class ConvertToHtmlUseCase(private val repository: TextHtmlRepository) {
    operator fun invoke(text: RichFormattedText): String {
        return repository.convertToHtml(text)
    }
}

class ParseFromHtmlUseCase(private val repository: TextHtmlRepository) {
    operator fun invoke(html: String): RichFormattedText {
        return repository.parseFromHtml(html)
    }
}

class EscapeHtmlUseCase(private val repository: TextHtmlRepository) {
    operator fun invoke(rawText: String): String {
        return repository.escapeHtml(rawText)
    }
}

class UnescapeHtmlUseCase(private val repository: TextHtmlRepository) {
    operator fun invoke(escapedHtml: String): String {
        return repository.unescapeHtml(escapedHtml)
    }
}

class StripHtmlFormattingUseCase(private val repository: TextHtmlRepository) {
    operator fun invoke(html: String): String {
        return repository.stripFormatting(html)
    }
}

class ExtractHtmlSpansUseCase {
    operator fun invoke(text: RichFormattedText): List<HtmlTextSpan> {
        return text.spans.sortedWith(compareBy({ it.start }, { it.end }))
    }
}

class HasRichFormattingUseCase {
    operator fun invoke(text: RichFormattedText): Boolean {
        return text.spans.isNotEmpty()
    }

    operator fun invoke(html: String): Boolean {
        return html.contains("<") && html.contains(">")
    }
}

class ObserveTextHtmlStateUseCase(private val repository: TextHtmlRepository) {
    operator fun invoke(): StateFlow<TextHtmlState> {
        return repository.observeState()
    }
}

class ClearTextHtmlStateUseCase(private val repository: TextHtmlRepository) {
    operator fun invoke() {
        repository.clearState()
    }
}
