package org.telegram.messenger.feature.messaging.mentions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionCandidate
import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionReplacement
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.ClearMentionsUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.DismissMentionsUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.FormatMentionReplacementUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.ObserveMentionsStateUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.SetMentionCandidatesUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.UpdateMentionQueryUseCase

/**
 * ViewModel для управления состоянием автодополнения упоминаний, хэштегов и команд ботов.
 */
class MentionsViewModel(
    private val observeMentionsStateUseCase: ObserveMentionsStateUseCase,
    private val updateMentionQueryUseCase: UpdateMentionQueryUseCase,
    private val setMentionCandidatesUseCase: SetMentionCandidatesUseCase,
    private val formatMentionReplacementUseCase: FormatMentionReplacementUseCase,
    private val dismissMentionsUseCase: DismissMentionsUseCase,
    private val clearMentionsUseCase: ClearMentionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MentionsUiState())
    val uiState: StateFlow<MentionsUiState> = _uiState.asStateFlow()

    private val _replacementEvents = MutableSharedFlow<MentionReplacement>(extraBufferCapacity = 1)
    val replacementEvents: SharedFlow<MentionReplacement> = _replacementEvents.asSharedFlow()

    init {
        observeMentionsStateUseCase()
            .onEach { domainState ->
                _uiState.value = MentionsUiState(
                    query = domainState.query,
                    candidates = domainState.candidates,
                    isSearching = domainState.isSearching,
                    isPanelVisible = domainState.isPanelVisible
                )
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: MentionsEvent) {
        when (event) {
            is MentionsEvent.OnInputTextChanged -> {
                updateMentionQueryUseCase(
                    text = event.text,
                    cursorPosition = event.cursorPosition,
                    allowContextBots = event.allowContextBots
                )
            }
            is MentionsEvent.OnCandidatesLoaded -> {
                setMentionCandidatesUseCase(
                    candidates = event.candidates,
                    query = event.query
                )
            }
            is MentionsEvent.OnCandidateSelected -> {
                val currentQuery = _uiState.value.query
                val replacement = formatMentionReplacementUseCase(
                    originalText = event.currentText,
                    query = currentQuery,
                    candidate = event.candidate
                )
                _replacementEvents.tryEmit(replacement)
                clearMentionsUseCase()
            }
            is MentionsEvent.OnDismissRequested -> {
                dismissMentionsUseCase()
            }
            is MentionsEvent.OnClearRequested -> {
                clearMentionsUseCase()
            }
        }
    }

    fun applyCandidate(currentText: String, candidate: MentionCandidate): MentionReplacement {
        val currentQuery = _uiState.value.query
        val replacement = formatMentionReplacementUseCase(
            originalText = currentText,
            query = currentQuery,
            candidate = candidate
        )
        clearMentionsUseCase()
        return replacement
    }
}
