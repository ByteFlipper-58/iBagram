package org.telegram.messenger.feature.pushlistener.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.pushlistener.domain.model.PushListenerState
import org.telegram.messenger.feature.pushlistener.domain.model.PushPayloadModel
import org.telegram.messenger.feature.pushlistener.domain.model.PushProcessResult
import org.telegram.messenger.feature.pushlistener.domain.model.PushType

interface PushListenerRepository {
    fun observeState(): Flow<PushListenerState>
    fun observeIncomingPushes(): Flow<PushPayloadModel>
    suspend fun getState(): PushListenerState
    suspend fun processPush(pushType: PushType, rawData: String, timestamp: Long): PushProcessResult
    suspend fun registerToken(pushType: PushType, token: String)
    suspend fun reportDecryptError(pushType: PushType)
    suspend fun setListening(enabled: Boolean)
    suspend fun clearHistory()
}
