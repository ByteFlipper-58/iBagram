package org.telegram.messenger.feature.messaging.emojieffects.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.telegram.messenger.feature.messaging.emojieffects.domain.model.EmojiInteractionSession
import org.telegram.messenger.feature.messaging.emojieffects.domain.repository.EmojiEffectsRepository
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.ClearEmojiEffectsUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.DismissEmojiEffectUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.ObserveEmojiEffectsStateUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.RecordEmojiTapUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.StartEmojiEffectUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.UpdateEmojiEffectProgressUseCase

/**
 * ViewModel для управления состоянием оверлея эффектов эмодзи и реакций.
 */
class EmojiEffectsViewModel(
    private val observeEmojiEffectsStateUseCase: ObserveEmojiEffectsStateUseCase,
    private val recordEmojiTapUseCase: RecordEmojiTapUseCase,
    private val startEmojiEffectUseCase: StartEmojiEffectUseCase,
    private val updateEmojiEffectProgressUseCase: UpdateEmojiEffectProgressUseCase,
    private val dismissEmojiEffectUseCase: DismissEmojiEffectUseCase,
    private val clearEmojiEffectsUseCase: ClearEmojiEffectsUseCase,
    private val repository: EmojiEffectsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmojiEffectsUiState())
    val uiState: StateFlow<EmojiEffectsUiState> = _uiState.asStateFlow()

    init {
        observeEmojiEffectsStateUseCase()
            .onEach { domainState ->
                _uiState.value = EmojiEffectsUiState(
                    activeEffects = domainState.activeEffects,
                    isIdle = domainState.isIdle,
                    pendingInteractionsCount = domainState.currentSession?.actionsCount ?: 0,
                    lastRecordedEmoji = domainState.currentSession?.emoticon
                )
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: EmojiEffectsEvent) {
        when (event) {
            is EmojiEffectsEvent.OnEmojiTapped -> {
                recordEmojiTapUseCase(
                    messageId = event.messageId,
                    rawEmoticon = event.rawEmoji,
                    animationIndex = event.animationIndex,
                    timestampMs = event.timestampMs
                )
            }
            is EmojiEffectsEvent.OnEffectStarted -> {
                startEmojiEffectUseCase(event.item)
            }
            is EmojiEffectsEvent.OnEffectProgressUpdated -> {
                updateEmojiEffectProgressUseCase(event.id, event.progress)
            }
            is EmojiEffectsEvent.OnEffectDismissed -> {
                dismissEmojiEffectUseCase(event.id)
            }
            is EmojiEffectsEvent.OnCancelAllRequested -> {
                dismissEmojiEffectUseCase.dismissAll()
            }
            is EmojiEffectsEvent.OnFlushTapsRequested -> {
                repository.clearCurrentSession()
            }
            is EmojiEffectsEvent.OnClearRequested -> {
                clearEmojiEffectsUseCase()
            }
        }
    }

    fun drainTapsSession(): EmojiInteractionSession? {
        return repository.drainCurrentSession()
    }
}
