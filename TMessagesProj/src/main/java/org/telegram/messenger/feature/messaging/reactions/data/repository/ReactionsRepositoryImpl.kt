package org.telegram.messenger.feature.messaging.reactions.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.reactions.data.datasource.ReactionsLocalDataSource
import org.telegram.messenger.feature.messaging.reactions.data.datasource.ReactionsRemoteDataSource
import org.telegram.messenger.feature.messaging.reactions.domain.model.ReactionItemModel
import org.telegram.messenger.feature.messaging.reactions.domain.model.ReactionsSettingsModel
import org.telegram.messenger.feature.messaging.reactions.domain.repository.ReactionsRepository

/**
 * Чистая реализация [ReactionsRepository], координирующая локальный и удаленный источники данных реакций.
 */
class ReactionsRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: ReactionsLocalDataSource,
    private val remoteDataSource: ReactionsRemoteDataSource
) : ReactionsRepository {

    override fun observeAvailableReactions(): Flow<List<ReactionItemModel>> =
        localDataSource.observeAvailableReactions()

    override suspend fun getAvailableReactions(): Result<List<ReactionItemModel>> {
        return Result.success(localDataSource.getAvailableReactions())
    }

    override suspend fun loadAvailableReactions(force: Boolean): Result<List<ReactionItemModel>> {
        val result = remoteDataSource.loadAvailableReactions(force)
        if (result is Result.Success) {
            localDataSource.setAvailableReactions(result.data)
        }
        return result
    }

    override fun observeRecentReactions(): Flow<List<ReactionItemModel>> =
        localDataSource.observeRecentReactions()

    override suspend fun getRecentReactions(): Result<List<ReactionItemModel>> {
        return Result.success(localDataSource.getRecentReactions())
    }

    override suspend fun getReactionsSettings(): Result<ReactionsSettingsModel> {
        return Result.success(localDataSource.getReactionsSettings())
    }

    override suspend fun getDoubleTapReaction(): Result<String?> {
        return Result.success(localDataSource.getDoubleTapReaction())
    }

    override suspend fun setDoubleTapReaction(reaction: String): Result<Unit> {
        localDataSource.setDoubleTapReaction(reaction)
        return Result.success(Unit)
    }

    override suspend fun sendReaction(
        dialogId: Long,
        messageId: Int,
        reactions: List<ReactionItemModel>,
        isBig: Boolean,
        addToRecent: Boolean
    ): Result<Unit> {
        return remoteDataSource.sendReaction(
            dialogId = dialogId,
            messageId = messageId,
            reactions = reactions,
            isBig = isBig,
            addToRecent = addToRecent
        )
    }

    override suspend fun clearReactions(dialogId: Long, messageId: Int): Result<Unit> {
        return remoteDataSource.clearReactions(dialogId, messageId)
    }

    override suspend fun sendVote(
        dialogId: Long,
        messageId: Int,
        pollId: Long,
        options: List<ByteArray>
    ): Result<Unit> {
        return remoteDataSource.sendVote(dialogId, messageId, pollId, options)
    }
}
