package org.telegram.messenger.feature.network.pushlistener.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import org.telegram.messenger.PushListenerController
import org.telegram.messenger.feature.network.pushlistener.data.datasource.PushListenerLocalDataSource
import org.telegram.messenger.feature.network.pushlistener.data.datasource.PushListenerRemoteDataSource
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushDecryptStatus
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushListenerState
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushPayloadModel
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushProcessResult
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushType
import org.telegram.messenger.feature.network.pushlistener.domain.repository.PushListenerRepository

class PushListenerRepositoryImpl(
    val account: Int,
    val localDataSource: PushListenerLocalDataSource,
    val remoteDataSource: PushListenerRemoteDataSource,
    val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : PushListenerRepository {

    private val _incomingPushesFlow = MutableSharedFlow<PushPayloadModel>(extraBufferCapacity = 64)

    override fun observeState(): Flow<PushListenerState> = localDataSource.state

    override fun observeIncomingPushes(): Flow<PushPayloadModel> = _incomingPushesFlow.asSharedFlow()

    override suspend fun getState(): PushListenerState = withContext(dispatcher) {
        localDataSource.getState()
    }

    override suspend fun processPush(
        pushType: PushType,
        rawData: String,
        timestamp: Long
    ): PushProcessResult = withContext(dispatcher) {
        if (!localDataSource.getState().isListening) {
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

        val payload = localDataSource.parsePushPayload(pushType, rawData, timestamp)
        localDataSource.recordPushReceived(payload)
        _incomingPushesFlow.tryEmit(payload)

        // Delegate to legacy PushListenerController if available
        runCatching {
            val legacyType = pushType.id
            // Legacy notification dispatch
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
    ): Unit = withContext(dispatcher) {
        localDataSource.saveToken(pushType, token)
        remoteDataSource.sendRegistrationToServer(pushType, token)
    }

    override suspend fun reportDecryptError(
        pushType: PushType
    ): Unit = withContext(dispatcher) {
        localDataSource.recordDecryptError("Decryption failed for ${pushType.name}")
        remoteDataSource.reportDecryptError(pushType)
    }

    override suspend fun setListening(
        enabled: Boolean
    ): Unit = withContext(dispatcher) {
        localDataSource.setListening(enabled)
    }

    override suspend fun clearHistory(): Unit = withContext(dispatcher) {
        localDataSource.clearHistory()
    }
}
