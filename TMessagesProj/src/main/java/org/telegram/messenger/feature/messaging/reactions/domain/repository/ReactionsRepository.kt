package org.telegram.messenger.feature.messaging.reactions.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.reactions.domain.model.ReactionItemModel
import org.telegram.messenger.feature.messaging.reactions.domain.model.ReactionsSettingsModel

interface ReactionsRepository {
    fun observeAvailableReactions(): Flow<List<ReactionItemModel>>
    suspend fun getAvailableReactions(): Result<List<ReactionItemModel>>
    suspend fun loadAvailableReactions(force: Boolean = false): Result<List<ReactionItemModel>>

    fun observeRecentReactions(): Flow<List<ReactionItemModel>>
    suspend fun getRecentReactions(): Result<List<ReactionItemModel>>

    suspend fun getReactionsSettings(): Result<ReactionsSettingsModel>
    suspend fun getDoubleTapReaction(): Result<String?>
    suspend fun setDoubleTapReaction(reaction: String): Result<Unit>

    suspend fun sendReaction(
        dialogId: Long,
        messageId: Int,
        reactions: List<ReactionItemModel>,
        isBig: Boolean = false,
        addToRecent: Boolean = true
    ): Result<Unit>

    suspend fun clearReactions(dialogId: Long, messageId: Int): Result<Unit>

    suspend fun sendVote(
        dialogId: Long,
        messageId: Int,
        pollId: Long,
        options: List<ByteArray>
    ): Result<Unit>
}
