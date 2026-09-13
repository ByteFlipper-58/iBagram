package org.telegram.messenger.feature.messaging.botforum.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.telegram.messenger.BotForumHelper
import org.telegram.messenger.feature.messaging.botforum.data.datasource.BotForumLocalDataSource
import org.telegram.messenger.feature.messaging.botforum.data.datasource.BotForumRemoteDataSource
import org.telegram.messenger.feature.messaging.botforum.domain.model.BotDraftMessageModel
import org.telegram.messenger.feature.messaging.botforum.domain.model.BotForumState
import org.telegram.messenger.feature.messaging.botforum.domain.model.StreamingSendButtonState
import org.telegram.messenger.feature.messaging.botforum.domain.repository.BotForumRepository

/**
 * Modern Clean Architecture implementation of [BotForumRepository]
 * coordinating local draft streaming states and MTProto actions.
 */
open class BotForumRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: BotForumLocalDataSource,
    private val remoteDataSource: BotForumRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : BotForumRepository {

    private val scope = CoroutineScope(SupervisorJob() + mainDispatcher)

    override fun observeState(): StateFlow<BotForumState> {
        return localDataSource.state
    }

    override fun getState(): BotForumState {
        return localDataSource.state.value
    }

    override fun getStreamingSendButtonState(userId: Long, topicId: Int): StreamingSendButtonState {
        return localDataSource.getStreamingSendButtonState(userId, topicId)
    }

    override fun isStreamingTopic(dialogId: Long, topicId: Long): Boolean {
        return localDataSource.isStreamingTopic(dialogId, topicId)
    }

    override fun saveIsStreamingTopic(dialogId: Long, topicId: Long, isStreaming: Boolean) {
        localDataSource.saveIsStreamingTopic(dialogId, topicId, isStreaming)
    }

    override fun hasBotForumDrafts(userId: Long, topicId: Int): Boolean {
        return localDataSource.hasBotForumDrafts(userId, topicId)
    }

    override fun stopStreaming(userId: Long, topicId: Long) {
        val drafts = localDataSource.state.value.activeDrafts.filterKeys {
            it.first == userId && it.second == topicId.toInt()
        }
        val firstRandomId = drafts.keys.firstOrNull()?.third ?: 0L

        localDataSource.stopStreaming(userId, topicId)

        if (firstRandomId != 0L) {
            scope.launch {
                remoteDataSource.sendStopDraft(userId, topicId, firstRandomId)
            }
        }
    }

    override fun removeAllMarkedAsRemovedMessages(userId: Long, topicId: Int) {
        localDataSource.removeAllMarkedAsRemovedMessages(userId, topicId)
    }

    override fun checkNewMessageDraftReplacement(
        userId: Long,
        topicId: Int,
        messageText: String?
    ): BotDraftMessageModel? {
        return localDataSource.checkNewMessageDraftReplacement(userId, topicId, messageText)
    }

    override fun onBotDraftUpdate(
        userId: Long,
        topicId: Int,
        randomId: Long,
        text: String,
        canStop: Boolean,
        keepOnStop: Boolean,
        isRich: Boolean
    ) {
        localDataSource.onBotDraftUpdate(userId, topicId, randomId, text, canStop, keepOnStop, isRich)
    }

    override fun onBotDraftTimeout(userId: Long, topicId: Int, randomId: Long) {
        localDataSource.onBotDraftTimeout(userId, topicId, randomId)
    }

    override fun isBotForum(dialogId: Long): Boolean {
        return runCatching {
            BotForumHelper.isBotForum(currentAccount, dialogId)
        }.getOrDefault(false)
    }

    override fun clearAll() {
        localDataSource.clearAll()
    }
}
