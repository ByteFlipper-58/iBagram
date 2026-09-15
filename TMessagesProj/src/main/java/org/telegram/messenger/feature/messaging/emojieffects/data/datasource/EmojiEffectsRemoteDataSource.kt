package org.telegram.messenger.feature.messaging.emojieffects.data.datasource

import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.emojieffects.domain.model.EmojiInteractionSession

class EmojiEffectsRemoteDataSource(
    private val currentAccount: Int
) {
    private val isLegacyAvailable: Boolean
        get() = ApplicationLoader.applicationContext != null

    suspend fun sendEmojiInteraction(peerId: Long, session: EmojiInteractionSession): Result<Unit> {
        return Result.Success(Unit)
    }
}
