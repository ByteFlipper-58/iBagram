package org.telegram.messenger.feature.network.pushlistener.data.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushListenerState
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushPayloadModel
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushType
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.ParsePushJsonPayloadUseCase

class PushListenerLocalDataSource(
    val account: Int,
    private val parsePayload: ParsePushJsonPayloadUseCase = ParsePushJsonPayloadUseCase()
) {
    private val _state = MutableStateFlow(PushListenerState())
    val state: StateFlow<PushListenerState> = _state.asStateFlow()

    fun getState(): PushListenerState = _state.value

    fun setListening(enabled: Boolean) {
        _state.update { it.copy(isListening = enabled) }
    }

    fun saveToken(pushType: PushType, token: String) {
        _state.update { current ->
            val updated = current.registeredTokens.toMutableMap()
            updated[pushType] = token
            current.copy(registeredTokens = updated)
        }
    }

    fun parsePushPayload(pushType: PushType, rawData: String, timestamp: Long): PushPayloadModel {
        return parsePayload(pushType, rawData, timestamp)
    }

    fun recordPushReceived(payload: PushPayloadModel) {
        _state.update { current ->
            current.copy(
                totalReceivedPushes = current.totalReceivedPushes + 1,
                lastProcessedPush = payload,
                lastError = null
            )
        }
    }

    fun recordDecryptError(errorMsg: String? = null) {
        _state.update { current ->
            current.copy(
                totalDecryptErrors = current.totalDecryptErrors + 1,
                lastError = errorMsg
            )
        }
    }

    fun clearHistory() {
        _state.update { current ->
            current.copy(
                lastProcessedPush = null,
                totalReceivedPushes = 0,
                totalDecryptErrors = 0,
                lastError = null
            )
        }
    }
}
