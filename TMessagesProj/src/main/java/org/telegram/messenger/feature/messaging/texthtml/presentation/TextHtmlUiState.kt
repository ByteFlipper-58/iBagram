package org.telegram.messenger.feature.messaging.texthtml.presentation

import org.telegram.messenger.feature.messaging.texthtml.domain.model.HtmlTextSpan

/**
 * UI State for the Text & HTML conversion engine screen or formatter component.
 */
data class TextHtmlUiState(
    val currentHtml: String = "",
    val currentPlainText: String = "",
    val spans: List<HtmlTextSpan> = emptyList(),
    val spansCount: Int = 0,
    val hasCustomEmoji: Boolean = false,
    val hasCodeBlocks: Boolean = false,
    val hasQuotes: Boolean = false,
    val isConverting: Boolean = false,
    val errorMessage: String? = null
)
