package org.telegram.messenger.feature.messaging.chatinput.data.datasource

import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.core.result.Result

class ChatInputRemoteDataSource(
    private val currentAccount: Int
) {
    private val isLegacyAvailable: Boolean
        get() = ApplicationLoader.applicationContext != null

    suspend fun sendTyping(peerId: Long, action: Int): Result<Unit> {
        return Result.Success(Unit)
    }

    suspend fun saveDraft(peerId: Long, draftText: String): Result<Unit> {
        return Result.Success(Unit)
    }
}
