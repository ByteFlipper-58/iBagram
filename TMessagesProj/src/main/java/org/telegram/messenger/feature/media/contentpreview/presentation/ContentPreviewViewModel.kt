package org.telegram.messenger.feature.media.contentpreview.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.ClearContentPreviewUseCase
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.DismissContentPreviewUseCase
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.ObserveContentPreviewStateUseCase
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.OpenContentPreviewUseCase
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.TriggerPreviewActionUseCase
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.UpdatePreviewDragUseCase

/**
 * ViewModel для управления жизненным циклом и жестами предпросмотра контента.
 */
class ContentPreviewViewModel(
    private val observeContentPreviewStateUseCase: ObserveContentPreviewStateUseCase,
    private val openContentPreviewUseCase: OpenContentPreviewUseCase,
    private val updatePreviewDragUseCase: UpdatePreviewDragUseCase,
    private val triggerPreviewActionUseCase: TriggerPreviewActionUseCase,
    private val dismissContentPreviewUseCase: DismissContentPreviewUseCase,
    private val clearContentPreviewUseCase: ClearContentPreviewUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContentPreviewUiState())
    val uiState: StateFlow<ContentPreviewUiState> = _uiState.asStateFlow()

    init {
        observeContentPreviewStateUseCase()
            .onEach { domainState ->
                _uiState.value = ContentPreviewUiState(
                    isVisible = domainState.isVisible,
                    isMenuVisible = domainState.isMenuVisible,
                    currentItem = domainState.currentItem,
                    availableActions = domainState.availableActions,
                    dragProgress = domainState.dragProgress
                )
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: ContentPreviewEvent) {
        when (event) {
            is ContentPreviewEvent.OnOpenPreviewRequested -> {
                openContentPreviewUseCase(event.item)
            }
            is ContentPreviewEvent.OnDragUpdated -> {
                updatePreviewDragUseCase(event.startY, event.currentY, event.maxDragDistance)
            }
            is ContentPreviewEvent.OnActionSelected -> {
                val currentItem = _uiState.value.currentItem
                triggerPreviewActionUseCase(event.action, currentItem)
            }
            is ContentPreviewEvent.OnDismissRequested -> {
                dismissContentPreviewUseCase()
            }
            is ContentPreviewEvent.OnClearRequested -> {
                clearContentPreviewUseCase()
            }
        }
    }
}
