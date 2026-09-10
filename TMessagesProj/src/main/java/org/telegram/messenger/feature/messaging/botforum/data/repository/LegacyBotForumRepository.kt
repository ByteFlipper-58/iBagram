package org.telegram.messenger.feature.messaging.botforum.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.BotForumHelper
import org.telegram.messenger.feature.messaging.botforum.data.mapper.BotForumMapper
import org.telegram.messenger.feature.messaging.botforum.domain.model.BotDraftMessageModel
import org.telegram.messenger.feature.messaging.botforum.domain.model.BotForumState
import org.telegram.messenger.feature.messaging.botforum.domain.model.StreamingSendButtonState
import org.telegram.messenger.feature.messaging.botforum.domain.repository.BotForumRepository
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.ResolveStreamingButtonStateUseCase

/**
 * Thread-safe implementation of BotForumRepository adapting legacy BotForumHelper.
 */
class LegacyBotForumRepository(
    private val currentAccount: Int
) : BotForumRepository {

    private val _state = MutableStateFlow(BotForumState())
    private val resolveButtonState = ResolveStreamingButtonStateUseCase()

    private val legacyHelper: BotForumHelper?
        get() = try {
            BotForumHelper.getInstance(currentAccount)
        } catch (_: Throwable) {
            null
        }

    override fun observeState(): StateFlow<BotForumState> = _state.asStateFlow()

    override fun getState(): BotForumState = _state.value

    override fun getStreamingSendButtonState(userId: Long, topicId: Int): StreamingSendButtonState {
        legacyHelper?.let { helper ->
            try {
                return BotForumMapper.mapStreamingButtonState(helper.getStreamingSendButtonState(userId, topicId))
            } catch (_: Throwable) {}
        }
        val draftsForTopic = _state.value.activeDrafts.filterKeys { it.first == userId && it.second == topicId }.values
        return resolveButtonState(draftsForTopic)
    }

    override fun isStreamingTopic(dialogId: Long, topicId: Long): Boolean {
        legacyHelper?.let { helper ->
            try {
                return helper.isStreamingTopic(dialogId, topicId)
            } catch (_: Throwable) {}
        }
        return _state.value.streamingTopics.contains(dialogId to topicId)
    }

    override fun saveIsStreamingTopic(dialogId: Long, topicId: Long, isStreaming: Boolean) {
        _state.update { curr ->
            val updated = if (isStreaming) {
                curr.streamingTopics + (dialogId to topicId)
            } else {
                curr.streamingTopics - (dialogId to topicId)
            }
            curr.copy(streamingTopics = updated)
        }
        legacyHelper?.let { helper ->
            try {
                helper.saveIsStreamingTopic(dialogId, topicId, isStreaming)
            } catch (_: Throwable) {}
        }
    }

    override fun hasBotForumDrafts(userId: Long, topicId: Int): Boolean {
        legacyHelper?.let { helper ->
            try {
                return helper.hasBotForumDrafts(userId, topicId)
            } catch (_: Throwable) {}
        }
        return _state.value.activeDrafts.any { (key, draft) ->
            key.first == userId && key.second == topicId && !draft.isRemoved
        }
    }

    override fun stopStreaming(userId: Long, topicId: Long) {
        val topicInt = topicId.toInt()
        _state.update { curr ->
            val toRemove = curr.activeDrafts.filterKeys { it.first == userId && it.second == topicInt }
            val newBlocked = curr.blockedRandomIds + toRemove.keys
            val newDrafts = curr.activeDrafts.toMutableMap()
            toRemove.forEach { (key, draft) ->
                if (draft.keepOnStop) {
                    newDrafts[key] = draft.copy(isRemoved = true)
                } else {
                    newDrafts.remove(key)
                }
            }
            curr.copy(
                activeDrafts = newDrafts,
                blockedRandomIds = newBlocked
            )
        }

        legacyHelper?.let { helper ->
            try {
                helper.stopStreaming(userId, topicId)
            } catch (_: Throwable) {}
        }
    }

    override fun removeAllMarkedAsRemovedMessages(userId: Long, topicId: Int) {
        _state.update { curr ->
            val filtered = curr.activeDrafts.filterNot { (key, draft) ->
                key.first == userId && key.second == topicId && draft.isRemoved
            }
            curr.copy(activeDrafts = filtered)
        }
        legacyHelper?.let { helper ->
            try {
                helper.removeAllMarkedAsRemovedMessages(userId, topicId)
            } catch (_: Throwable) {}
        }
    }

    override fun checkNewMessageDraftReplacement(
        userId: Long,
        topicId: Int,
        messageText: String?
    ): BotDraftMessageModel? {
        val topicDrafts = _state.value.activeDrafts.filterKeys { it.first == userId && it.second == topicId }
        if (topicDrafts.isEmpty()) return null

        var bestMatch: Pair<Triple<Long, Int, Long>, BotDraftMessageModel>? = null
        for ((key, draft) in topicDrafts) {
            if (bestMatch == null) {
                bestMatch = key to draft
            }
            if (messageText != null && messageText.startsWith(draft.text)) {
                bestMatch = key to draft
                break
            }
        }

        bestMatch?.let { (key, draft) ->
            _state.update { curr ->
                curr.copy(activeDrafts = curr.activeDrafts - key)
            }
            return draft
        }
        return null
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
        val key = Triple(userId, topicId, randomId)
        if (_state.value.blockedRandomIds.contains(key)) {
            return
        }

        _state.update { curr ->
            val existing = curr.activeDrafts[key]
            val localId = existing?.localMessageId ?: (randomId.toInt())
            val updatedDraft = BotDraftMessageModel(
                userId = userId,
                topicId = topicId,
                randomId = randomId,
                localMessageId = localId,
                text = text,
                canStop = canStop,
                keepOnStop = keepOnStop,
                isRemoved = false,
                isRich = isRich
            )
            // Telegram removes other drafts for the same topic when a new draft arrives
            val newDrafts = curr.activeDrafts.filterKeys { !(it.first == userId && it.second == topicId && it.third != randomId) }.toMutableMap()
            newDrafts[key] = updatedDraft
            curr.copy(activeDrafts = newDrafts)
        }
    }

    override fun onBotDraftTimeout(userId: Long, topicId: Int, randomId: Long) {
        val key = Triple(userId, topicId, randomId)
        _state.update { curr ->
            curr.copy(activeDrafts = curr.activeDrafts - key)
        }
    }

    override fun isBotForum(dialogId: Long): Boolean {
        legacyHelper?.let {
            try {
                return BotForumHelper.isBotForum(currentAccount, dialogId)
            } catch (_: Throwable) {}
        }
        return dialogId > 0
    }

    override fun clearAll() {
        _state.update { BotForumState() }
    }
}
