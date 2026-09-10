package org.telegram.messenger.feature.messaging.draftmeasure.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.CalculateDraftMeasureOverrideUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.GetDraftMeasureConfigUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.ObserveDraftMeasureConfigUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.OnDraftMessageIdChangedUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.ResetDraftMeasureTargetUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.SetDraftMeasureTargetUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.SetPreviousMessageHeightUseCase

class DraftMeasureViewModel(
    private val calculateOverrideUseCase: CalculateDraftMeasureOverrideUseCase,
    private val setTargetUseCase: SetDraftMeasureTargetUseCase,
    private val onMessageIdChangedUseCase: OnDraftMessageIdChangedUseCase,
    private val setPreviousHeightUseCase: SetPreviousMessageHeightUseCase,
    private val resetTargetUseCase: ResetDraftMeasureTargetUseCase,
    private val observeConfigUseCase: ObserveDraftMeasureConfigUseCase,
    private val getConfigUseCase: GetDraftMeasureConfigUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        run {
            val config = getConfigUseCase()
            DraftMeasureUiState(
                target = config.target,
                previousMessageHeight = config.previousMessageHeight,
                hasAdditionalHeight = config.hasAdditionalHeight
            )
        }
    )
    val uiState: StateFlow<DraftMeasureUiState> = _uiState.asStateFlow()

    init {
        observeConfigUseCase()
            .onEach { config ->
                _uiState.value = _uiState.value.copy(
                    target = config.target,
                    previousMessageHeight = config.previousMessageHeight,
                    hasAdditionalHeight = config.hasAdditionalHeight
                )
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: DraftMeasureEvent) {
        when (event) {
            is DraftMeasureEvent.SetTarget -> {
                setTargetUseCase(event.messageId, event.groupId)
            }
            is DraftMeasureEvent.MessageIdChanged -> {
                onMessageIdChangedUseCase(event.oldMessageId, event.newMessageId, event.groupId)
            }
            is DraftMeasureEvent.SetPreviousHeight -> {
                setPreviousHeightUseCase(event.height)
            }
            is DraftMeasureEvent.ResetTarget -> {
                resetTargetUseCase()
            }
            is DraftMeasureEvent.CalculateOverride -> {
                val result = calculateOverrideUseCase(
                    messageId = event.messageId,
                    groupId = event.groupId,
                    measuredHeight = event.measuredHeight,
                    viewport = event.viewport
                )
                _uiState.value = _uiState.value.copy(lastResult = result)
            }
        }
    }
}
