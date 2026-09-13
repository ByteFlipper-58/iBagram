package org.telegram.messenger.feature.messaging.botkeyboard.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.messaging.botkeyboard.data.datasource.BotKeyboardLocalDataSource
import org.telegram.messenger.feature.messaging.botkeyboard.data.datasource.BotKeyboardRemoteDataSource
import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotButtonItem
import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotKeyboardLayout
import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotKeyboardState
import org.telegram.messenger.feature.messaging.botkeyboard.domain.repository.BotKeyboardRepository

/**
 * Production implementation of BotKeyboardRepository coordinating local and remote data sources.
 */
class BotKeyboardRepositoryImpl(
    private val account: Int,
    private val localDataSource: BotKeyboardLocalDataSource,
    private val remoteDataSource: BotKeyboardRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BotKeyboardRepository {

    private val _state = MutableStateFlow(
        BotKeyboardState(activeKeyboards = localDataSource.getAllKeyboards())
    )

    override fun getKeyboardForMessage(messageId: Long): BotKeyboardLayout? {
        return localDataSource.getKeyboardForMessage(messageId)
    }

    override fun setKeyboardForMessage(messageId: Long, layout: BotKeyboardLayout) {
        localDataSource.setKeyboardForMessage(messageId, layout)
        syncState()
    }

    override fun removeKeyboardForMessage(messageId: Long): BotKeyboardLayout? {
        val removed = localDataSource.removeKeyboardForMessage(messageId)
        syncState()
        return removed
    }

    override fun clearAllKeyboards() {
        localDataSource.clearAllKeyboards()
        syncState()
    }

    override fun isForceReply(replyMarkup: Any?): Boolean {
        return localDataSource.isForceReply(replyMarkup)
    }

    override fun isButtonWebView(button: Any?): Boolean {
        return localDataSource.isButtonWebView(button)
    }

    override fun recordButtonPressed(button: BotButtonItem) {
        _state.update { current ->
            current.copy(lastPressedButton = button)
        }
    }

    override fun observeState(): StateFlow<BotKeyboardState> {
        return _state.asStateFlow()
    }

    override fun getCurrentState(): BotKeyboardState {
        return _state.value
    }

    private fun syncState() {
        _state.update { current ->
            current.copy(activeKeyboards = localDataSource.getAllKeyboards())
        }
    }
}
