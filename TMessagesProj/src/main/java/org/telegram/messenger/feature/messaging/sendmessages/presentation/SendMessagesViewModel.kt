package org.telegram.messenger.feature.messaging.sendmessages.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.CancelSendMessageUseCase
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.ForwardMessagesUseCase
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.ObservePendingSendsUseCase
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.RetrySendMessageUseCase
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.SendMediaAlbumUseCase
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.SendMediaMessageUseCase
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.SendTextMessageUseCase

class SendMessagesViewModel(
    private val sendTextMessageUseCase: SendTextMessageUseCase,
    private val sendMediaMessageUseCase: SendMediaMessageUseCase,
    private val sendMediaAlbumUseCase: SendMediaAlbumUseCase,
    private val forwardMessagesUseCase: ForwardMessagesUseCase,
    private val retrySendMessageUseCase: RetrySendMessageUseCase,
    private val cancelSendMessageUseCase: CancelSendMessageUseCase,
    private val observePendingSendsUseCase: ObservePendingSendsUseCase,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
) {

    private val _uiState = MutableStateFlow(SendMessagesUiState())
    val uiState: StateFlow<SendMessagesUiState> = _uiState.asStateFlow()

    init {
        observePendingSendsUseCase()
            .onEach { list ->
                _uiState.update { current ->
                    SendMessagesUiState.fromList(list, current.errorMessage)
                }
            }
            .launchIn(coroutineScope)
    }

    fun onEvent(event: SendMessagesEvent) {
        when (event) {
            is SendMessagesEvent.SendText -> {
                coroutineScope.launch {
                    try {
                        sendTextMessageUseCase(
                            dialogId = event.dialogId,
                            text = event.text,
                            options = event.options,
                            isPremium = event.isPremium
                        )
                    } catch (e: Exception) {
                        _uiState.update { it.copy(errorMessage = e.message) }
                    }
                }
            }
            is SendMessagesEvent.SendMedia -> {
                coroutineScope.launch {
                    try {
                        sendMediaMessageUseCase(
                            dialogId = event.dialogId,
                            item = event.item,
                            options = event.options,
                            isPremium = event.isPremium
                        )
                    } catch (e: Exception) {
                        _uiState.update { it.copy(errorMessage = e.message) }
                    }
                }
            }
            is SendMessagesEvent.SendAlbum -> {
                coroutineScope.launch {
                    try {
                        sendMediaAlbumUseCase(
                            dialogId = event.dialogId,
                            items = event.items,
                            options = event.options
                        )
                    } catch (e: Exception) {
                        _uiState.update { it.copy(errorMessage = e.message) }
                    }
                }
            }
            is SendMessagesEvent.Forward -> {
                coroutineScope.launch {
                    try {
                        forwardMessagesUseCase(event.request)
                    } catch (e: Exception) {
                        _uiState.update { it.copy(errorMessage = e.message) }
                    }
                }
            }
            is SendMessagesEvent.Retry -> {
                coroutineScope.launch {
                    retrySendMessageUseCase(event.localId)
                }
            }
            is SendMessagesEvent.Cancel -> {
                coroutineScope.launch {
                    cancelSendMessageUseCase(event.localId)
                }
            }
            is SendMessagesEvent.CancelAll -> {
                // Cancel all active sends
            }
        }
    }

    fun destroy() {
        coroutineScope.cancel()
    }
}
