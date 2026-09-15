package org.telegram.messenger.feature.messaging.emojieffects.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.messaging.emojieffects.data.datasource.EmojiEffectsLocalDataSource
import org.telegram.messenger.feature.messaging.emojieffects.data.datasource.EmojiEffectsRemoteDataSource
import org.telegram.messenger.feature.messaging.emojieffects.domain.model.EmojiEffectItem
import org.telegram.messenger.feature.messaging.emojieffects.domain.model.EmojiEffectsState
import org.telegram.messenger.feature.messaging.emojieffects.domain.model.EmojiInteractionSession
import org.telegram.messenger.feature.messaging.emojieffects.domain.repository.EmojiEffectsRepository

class EmojiEffectsRepositoryImpl(
    private val currentAccount: Int = 0,
    private val localDataSource: EmojiEffectsLocalDataSource,
    private val remoteDataSource: EmojiEffectsRemoteDataSource
) : EmojiEffectsRepository {

    override fun observeState(): Flow<EmojiEffectsState> = localDataSource.stateFlow

    override fun getState(): EmojiEffectsState = localDataSource.getState()

    override fun recordTap(
        messageId: Int,
        emoticon: String,
        animationIndex: Int,
        timestampMs: Long
    ): EmojiInteractionSession = localDataSource.recordTap(messageId, emoticon, animationIndex, timestampMs)

    override fun drainCurrentSession(): EmojiInteractionSession? = localDataSource.drainCurrentSession()

    override fun clearCurrentSession() {
        localDataSource.clearCurrentSession()
    }

    override fun startEffect(item: EmojiEffectItem) {
        localDataSource.startEffect(item)
    }

    override fun updateEffectProgress(id: String, progress: Float) {
        localDataSource.updateEffectProgress(id, progress)
    }

    override fun dismissEffect(id: String) {
        localDataSource.dismissEffect(id)
    }

    override fun removeEffect(id: String) {
        localDataSource.removeEffect(id)
    }

    override fun cancelAllEffects() {
        localDataSource.cancelAllEffects()
    }

    override fun clear() {
        localDataSource.clear()
    }

    override fun updateLastAnimationIndex(documentId: Long, nextIndex: Int) {
        localDataSource.updateLastAnimationIndex(documentId, nextIndex)
    }

    override fun getLastAnimationIndex(documentId: Long): Int =
        localDataSource.getLastAnimationIndex(documentId)
}
