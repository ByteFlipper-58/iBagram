package org.telegram.messenger.feature.chatattach.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.chatattach.domain.repository.ChatAttachRepository
import org.telegram.messenger.feature.chatattach.domain.usecase.CalculateAttachCaptionLimitUseCase
import org.telegram.messenger.feature.chatattach.domain.usecase.ClearAttachSelectionUseCase
import org.telegram.messenger.feature.chatattach.domain.usecase.ObserveChatAttachStateUseCase
import org.telegram.messenger.feature.chatattach.domain.usecase.OpenChatAttachAlertUseCase
import org.telegram.messenger.feature.chatattach.domain.usecase.SelectAttachLayoutUseCase
import org.telegram.messenger.feature.chatattach.domain.usecase.ToggleAttachItemSelectionUseCase
import org.telegram.messenger.feature.chatattach.domain.usecase.UpdateAttachSendOptionsUseCase

class ChatAttachViewModel(
    private val observeStateUseCase: ObserveChatAttachStateUseCase,
    private val openAlertUseCase: OpenChatAttachAlertUseCase,
    private val selectLayoutUseCase: SelectAttachLayoutUseCase,
    private val toggleSelectionUseCase: ToggleAttachItemSelectionUseCase,
    private val updateSendOptionsUseCase: UpdateAttachSendOptionsUseCase,
    private val clearSelectionUseCase: ClearAttachSelectionUseCase,
    private val calculateCaptionLimitUseCase: CalculateAttachCaptionLimitUseCase = CalculateAttachCaptionLimitUseCase(),
    private val repository: ChatAttachRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _uiState = MutableStateFlow(ChatAttachUiState())
    val uiState: StateFlow<ChatAttachUiState> = _uiState.asStateFlow()

    init {
        observeStateUseCase().onEach { domainState ->
            val captionInfo = calculateCaptionLimitUseCase(
                domainState.sendOptions.caption,
                false
            )
            _uiState.update { current ->
                current.copy(
                    currentLayout = domainState.currentLayout,
                    availableLayouts = domainState.availableLayouts,
                    selectedItems = domainState.selectedItems,
                    sendOptions = domainState.sendOptions,
                    permissions = domainState.permissions,
                    captionInfo = captionInfo,
                    isAlertVisible = domainState.isAlertVisible,
                    isSendEnabled = domainState.selectedItems.isNotEmpty() && !captionInfo.isLimitExceeded
                )
            }
        }.launchIn(scope)
    }

    fun onEvent(event: ChatAttachEvent) {
        scope.launch {
            when (event) {
                is ChatAttachEvent.OnOpenAlert -> {
                    openAlertUseCase(event.permissions, event.initialLayout)
                }
                is ChatAttachEvent.OnLayoutSelected -> {
                    selectLayoutUseCase(event.layout)
                }
                is ChatAttachEvent.OnItemSelectionToggled -> {
                    toggleSelectionUseCase(event.item)
                }
                is ChatAttachEvent.OnItemsSelected -> {
                    repository.setSelectedItems(event.items)
                }
                is ChatAttachEvent.OnClearSelectionRequested -> {
                    clearSelectionUseCase()
                }
                is ChatAttachEvent.OnSendOptionsChanged -> {
                    updateSendOptionsUseCase(event.options)
                }
                is ChatAttachEvent.OnCaptionChanged -> {
                    val currentOptions = repository.getState().sendOptions
                    updateSendOptionsUseCase(currentOptions.copy(caption = event.caption))
                }
                is ChatAttachEvent.OnSendAsFileToggled -> {
                    val currentOptions = repository.getState().sendOptions
                    updateSendOptionsUseCase(currentOptions.copy(sendAsFile = event.sendAsFile))
                }
                is ChatAttachEvent.OnSpoilerToggled -> {
                    val currentOptions = repository.getState().sendOptions
                    updateSendOptionsUseCase(currentOptions.copy(hasSpoiler = event.hasSpoiler))
                }
                is ChatAttachEvent.OnCaptionAboveToggled -> {
                    val currentOptions = repository.getState().sendOptions
                    updateSendOptionsUseCase(currentOptions.copy(isCaptionAbove = event.isAbove))
                }
                is ChatAttachEvent.OnStarsPriceChanged -> {
                    val currentOptions = repository.getState().sendOptions
                    updateSendOptionsUseCase(currentOptions.copy(starsPrice = event.price))
                }
                is ChatAttachEvent.OnDismissRequested -> {
                    repository.dismissAlert()
                }
                is ChatAttachEvent.OnClearRequested -> {
                    repository.clear()
                }
            }
        }
    }
}
