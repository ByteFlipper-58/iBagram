package org.telegram.messenger.feature.richcaption.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.richcaption.domain.model.CaptionEntitySpan
import org.telegram.messenger.feature.richcaption.domain.usecase.ClearRichCaptionUseCase
import org.telegram.messenger.feature.richcaption.domain.usecase.GetRichCaptionUseCase
import org.telegram.messenger.feature.richcaption.domain.usecase.ObserveRichCaptionUseCase
import org.telegram.messenger.feature.richcaption.domain.usecase.SetRichCaptionCreditUseCase
import org.telegram.messenger.feature.richcaption.domain.usecase.SetRichCaptionLockedUseCase
import org.telegram.messenger.feature.richcaption.domain.usecase.SetRichCaptionTextUseCase

class RichCaptionViewModel(
    private val observeRichCaptionUseCase: ObserveRichCaptionUseCase,
    private val getRichCaptionUseCase: GetRichCaptionUseCase,
    private val setRichCaptionTextUseCase: SetRichCaptionTextUseCase,
    private val setRichCaptionCreditUseCase: SetRichCaptionCreditUseCase,
    private val setRichCaptionLockedUseCase: SetRichCaptionLockedUseCase,
    private val clearRichCaptionUseCase: ClearRichCaptionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        RichCaptionUiState(caption = getRichCaptionUseCase())
    )
    val uiState: StateFlow<RichCaptionUiState> = _uiState.asStateFlow()

    init {
        observeRichCaptionUseCase()
            .onEach { caption ->
                _uiState.update { it.copy(caption = caption) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: RichCaptionEvent) {
        when (event) {
            is RichCaptionEvent.SetText -> {
                setRichCaptionTextUseCase(event.text, event.spans)
            }
            is RichCaptionEvent.SetCredit -> {
                setRichCaptionCreditUseCase(event.credit)
            }
            is RichCaptionEvent.SetLocked -> {
                setRichCaptionLockedUseCase(event.locked)
            }
            is RichCaptionEvent.SetEditing -> {
                _uiState.update { it.copy(isEditing = event.isEditing) }
            }
            is RichCaptionEvent.SetHasSelection -> {
                _uiState.update { it.copy(hasSelection = event.hasSelection) }
            }
            is RichCaptionEvent.Clear -> {
                clearRichCaptionUseCase()
            }
        }
    }

    fun setText(text: String, spans: List<CaptionEntitySpan> = emptyList()) {
        onEvent(RichCaptionEvent.SetText(text, spans))
    }

    fun setCredit(credit: String?) {
        onEvent(RichCaptionEvent.SetCredit(credit))
    }

    fun setLocked(locked: Boolean) {
        onEvent(RichCaptionEvent.SetLocked(locked))
    }

    fun setEditing(isEditing: Boolean) {
        onEvent(RichCaptionEvent.SetEditing(isEditing))
    }

    fun setHasSelection(hasSelection: Boolean) {
        onEvent(RichCaptionEvent.SetHasSelection(hasSelection))
    }

    fun clear() {
        onEvent(RichCaptionEvent.Clear)
    }
}
