package org.telegram.messenger.feature.messaging.texthtml.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.messaging.texthtml.domain.usecase.ClearTextHtmlStateUseCase
import org.telegram.messenger.feature.messaging.texthtml.domain.usecase.ConvertToHtmlUseCase
import org.telegram.messenger.feature.messaging.texthtml.domain.usecase.EscapeHtmlUseCase
import org.telegram.messenger.feature.messaging.texthtml.domain.usecase.ObserveTextHtmlStateUseCase
import org.telegram.messenger.feature.messaging.texthtml.domain.usecase.ParseFromHtmlUseCase
import org.telegram.messenger.feature.messaging.texthtml.domain.usecase.StripHtmlFormattingUseCase

class TextHtmlViewModel(
    private val convertToHtmlUseCase: ConvertToHtmlUseCase,
    private val parseFromHtmlUseCase: ParseFromHtmlUseCase,
    private val escapeHtmlUseCase: EscapeHtmlUseCase,
    private val stripHtmlFormattingUseCase: StripHtmlFormattingUseCase,
    private val observeTextHtmlStateUseCase: ObserveTextHtmlStateUseCase,
    private val clearTextHtmlStateUseCase: ClearTextHtmlStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TextHtmlUiState())
    val uiState: StateFlow<TextHtmlUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeTextHtmlStateUseCase().collectLatest { state ->
                _uiState.update { current ->
                    current.copy(
                        currentHtml = state.lastHtml ?: current.currentHtml,
                        currentPlainText = state.lastPlainText ?: current.currentPlainText,
                        spansCount = state.spansCount,
                        hasCustomEmoji = state.hasCustomEmoji,
                        hasCodeBlocks = state.hasCodeBlocks,
                        hasQuotes = state.hasQuotes
                    )
                }
            }
        }
    }

    fun onEvent(event: TextHtmlEvent) {
        when (event) {
            is TextHtmlEvent.ConvertToHtml -> {
                try {
                    val html = convertToHtmlUseCase(event.text)
                    _uiState.update {
                        it.copy(
                            currentHtml = html,
                            currentPlainText = event.text.plainText,
                            spans = event.text.spans,
                            spansCount = event.text.spans.size,
                            hasCustomEmoji = event.text.spans.any { span -> span.type == org.telegram.messenger.feature.messaging.texthtml.domain.model.HtmlTextSpanType.CUSTOM_EMOJI },
                            hasCodeBlocks = event.text.spans.any { span -> span.type == org.telegram.messenger.feature.messaging.texthtml.domain.model.HtmlTextSpanType.CODE || span.type == org.telegram.messenger.feature.messaging.texthtml.domain.model.HtmlTextSpanType.MONO },
                            hasQuotes = event.text.spans.any { span -> span.type == org.telegram.messenger.feature.messaging.texthtml.domain.model.HtmlTextSpanType.QUOTE || span.type == org.telegram.messenger.feature.messaging.texthtml.domain.model.HtmlTextSpanType.COLLAPSED_QUOTE }
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = e.message ?: "Failed to convert to HTML") }
                }
            }
            is TextHtmlEvent.ParseHtml -> {
                try {
                    val parsed = parseFromHtmlUseCase(event.html)
                    _uiState.update {
                        it.copy(
                            currentHtml = event.html,
                            currentPlainText = parsed.plainText,
                            spans = parsed.spans,
                            spansCount = parsed.spans.size,
                            hasCustomEmoji = parsed.spans.any { span -> span.type == org.telegram.messenger.feature.messaging.texthtml.domain.model.HtmlTextSpanType.CUSTOM_EMOJI },
                            hasCodeBlocks = parsed.spans.any { span -> span.type == org.telegram.messenger.feature.messaging.texthtml.domain.model.HtmlTextSpanType.CODE || span.type == org.telegram.messenger.feature.messaging.texthtml.domain.model.HtmlTextSpanType.MONO },
                            hasQuotes = parsed.spans.any { span -> span.type == org.telegram.messenger.feature.messaging.texthtml.domain.model.HtmlTextSpanType.QUOTE || span.type == org.telegram.messenger.feature.messaging.texthtml.domain.model.HtmlTextSpanType.COLLAPSED_QUOTE }
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = e.message ?: "Failed to parse HTML") }
                }
            }
            is TextHtmlEvent.EscapeText -> {
                val escaped = escapeHtmlUseCase(event.rawText)
                _uiState.update {
                    it.copy(
                        currentHtml = escaped,
                        currentPlainText = event.rawText,
                        spans = emptyList(),
                        spansCount = 0
                    )
                }
            }
            is TextHtmlEvent.StripFormatting -> {
                val stripped = stripHtmlFormattingUseCase(event.html)
                _uiState.update {
                    it.copy(
                        currentHtml = stripped,
                        currentPlainText = stripped,
                        spans = emptyList(),
                        spansCount = 0
                    )
                }
            }
            is TextHtmlEvent.ClearState -> {
                clearTextHtmlStateUseCase()
                _uiState.update { TextHtmlUiState() }
            }
            is TextHtmlEvent.DismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }
}
