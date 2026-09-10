package org.telegram.messenger.feature.media.voip.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.voip.domain.model.CallModel

/**
 * Clean domain repository contract for managing VoIP calls.
 */
interface VoIPRepository {
    fun observeCurrentCall(): Flow<CallModel?>
    suspend fun getCurrentCall(): CallModel?
    suspend fun startCall(userId: Long, isVideo: Boolean): Result<Unit>
    suspend fun acceptCall(): Result<Unit>
    suspend fun declineCall(): Result<Unit>
    suspend fun hangUp(): Result<Unit>
    suspend fun toggleMute(): Result<Boolean>
    suspend fun toggleSpeakerphone(): Result<Boolean>
}
