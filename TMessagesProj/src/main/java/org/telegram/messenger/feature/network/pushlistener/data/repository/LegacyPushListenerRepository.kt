package org.telegram.messenger.feature.network.pushlistener.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.telegram.messenger.PushListenerController
import org.telegram.messenger.feature.network.pushlistener.data.mapper.PushListenerMapper
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushDecryptStatus
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushListenerState
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushPayloadModel
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushProcessResult
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushType
import org.telegram.messenger.feature.network.pushlistener.domain.repository.PushListenerRepository
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.ParsePushJsonPayloadUseCase

class LegacyPushListenerRepository(
    private val currentAccount: Int,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val parsePayload: ParsePushJsonPayloadUseCase = ParsePushJsonPayloadUseCase()
) : PushListenerRepository {

    private val _stateFlow = MutableStateFlow(PushListenerState())
    private val _incomingPushesFlow = MutableSharedFlow<PushPayloadModel>(extraBufferCapacity = 64)

    override fun observeState(): Flow<PushListenerState> = _stateFlow.asStateFlow()

    override fun observeIncomingPushes(): Flow<PushPayloadModel> = _incomingPushesFlow.asSharedFlow()

    override suspend fun getState(): PushListenerState = withContext(ioDispatcher) {
        _stateFlow.value
    }

    override suspend fun processPush(
        pushType: PushType,
        rawData: String,
        timestamp: Long
    ): PushProcessResult = withContext(ioDispatcher) {
        if (!_stateFlow.value.isListening) {
            return@withContext PushProcessResult(
                payload = null,
                status = PushDecryptStatus.SUCCESS,
                isHandled = false,
                errorMessage = "Push listener is disabled"
            )
        }

        if (rawData.isBlank()) {
            reportDecryptError(pushType)
            return@withContext PushProcessResult(
                payload = null,
                status = PushDecryptStatus.PAYLOAD_CORRUPTED,
                isHandled = false,
                errorMessage = "Empty push payload"
            )
        }

        val payload = parsePayload(pushType, rawData, timestamp)

        _stateFlow.update { current ->
            current.copy(
                totalReceivedPushes = current.totalReceivedPushes + 1,
                lastProcessedPush = payload,
                lastError = null
            )
        }

        _incomingPushesFlow.tryEmit(payload)

        // Delegate to legacy PushListenerController if available
        runCatching {
            val legacyType = PushListenerMapper.toLegacyPushType(pushType)
            PushListenerController.processRemoteMessage(legacyType, rawData, timestamp)
        }

        PushProcessResult(
            payload = payload,
            status = PushDecryptStatus.SUCCESS,
            isHandled = true
        )
    }

    override suspend fun registerToken(
        pushType: PushType,
        token: String
    ): Unit = withContext(ioDispatcher) {
        _stateFlow.update { current ->
            val updatedTokens = current.registeredTokens.toMutableMap()
            updatedTokens[pushType] = token
            current.copy(registeredTokens = updatedTokens)
        }

        runCatching {
            val legacyType = PushListenerMapper.toLegacyPushType(pushType)
            PushListenerController.sendRegistrationToServer(legacyType, token)
        }
        Unit
    }

    override suspend fun reportDecryptError(
        pushType: PushType
    ): Unit = withContext(ioDispatcher) {
        _stateFlow.update { current ->
            current.copy(
                totalDecryptErrors = current.totalDecryptErrors + 1,
                lastError = "Decrypt error on push from ${pushType.name}"
            )
        }
    }

    override suspend fun setListening(
        enabled: Boolean
    ): Unit = withContext(ioDispatcher) {
        _stateFlow.update { it.copy(isListening = enabled) }
    }

    override suspend fun clearHistory(): Unit = withContext(ioDispatcher) {
        _stateFlow.update { current ->
            current.copy(
                lastProcessedPush = null,
                totalReceivedPushes = 0,
                totalDecryptErrors = 0,
                lastError = null
            )
        }
    }
}
