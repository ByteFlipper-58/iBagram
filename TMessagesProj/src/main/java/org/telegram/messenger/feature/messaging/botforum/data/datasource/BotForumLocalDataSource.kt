package org.telegram.messenger.feature.messaging.botforum.data.datasource

import android.app.Activity
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.feature.messaging.botforum.domain.model.BotDraftMessageModel
import org.telegram.messenger.feature.messaging.botforum.domain.model.BotForumState
import org.telegram.messenger.feature.messaging.botforum.domain.model.StreamingSendButtonState
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Local data source managing bot forum drafts, topic streaming states, and reactive updates.
 */
open class BotForumLocalDataSource(
    private val currentAccount: Int
) {

    private val activeDrafts = ConcurrentHashMap<Triple<Long, Int, Long>, BotDraftMessageModel>()
    private val streamingTopics = ConcurrentHashMap.newKeySet<Pair<Long, Long>>()
    private val blockedRandomIds = ConcurrentHashMap.newKeySet<Triple<Long, Int, Long>>()
    private val nextLocalMessageId = AtomicInteger(1000)

    private val _state = MutableStateFlow(BotForumState())
    val state: StateFlow<BotForumState> = _state.asStateFlow()

    private val preferences: SharedPreferences? by lazy {
        try {
            ApplicationLoader.applicationContext?.getSharedPreferences("bot_drafts$currentAccount", Activity.MODE_PRIVATE)
        } catch (_: Throwable) {
            null
        }
    }

    open fun getStreamingSendButtonState(userId: Long, topicId: Int): StreamingSendButtonState {
        var foundDraft: BotDraftMessageModel? = null
        for ((key, draft) in activeDrafts) {
            if (key.first == userId && key.second == topicId && !draft.isRemoved) {
                foundDraft = draft
                break
            }
        }
        if (foundDraft == null) {
            return StreamingSendButtonState.NO_STREAMING
        }
        return if (foundDraft.canStop) {
            StreamingSendButtonState.STOP
        } else {
            StreamingSendButtonState.BLOCKING
        }
    }

    open fun isStreamingTopic(dialogId: Long, topicId: Long): Boolean {
        val pref = preferences
        if (pref != null) {
            try {
                return pref.getBoolean("${dialogId}_$topicId", false)
            } catch (_: Throwable) {}
        }
        return streamingTopics.contains(dialogId to topicId)
    }

    open fun saveIsStreamingTopic(dialogId: Long, topicId: Long, isStreaming: Boolean) {
        val pref = preferences
        if (pref != null) {
            try {
                pref.edit().putBoolean("${dialogId}_$topicId", isStreaming).apply()
            } catch (_: Throwable) {}
        }
        if (isStreaming) {
            streamingTopics.add(dialogId to topicId)
        } else {
            streamingTopics.remove(dialogId to topicId)
        }
        syncState()
    }

    open fun hasBotForumDrafts(userId: Long, topicId: Int): Boolean {
        for ((key, draft) in activeDrafts) {
            if (key.first == userId && key.second == topicId && !draft.isRemoved) {
                return true
            }
        }
        return false
    }

    open fun stopStreaming(userId: Long, topicId: Long) {
        val topicInt = topicId.toInt()
        val toBlock = ArrayList<Triple<Long, Int, Long>>()
        for ((key, draft) in activeDrafts) {
            if (key.first == userId && key.second == topicInt) {
                toBlock.add(key)
                if (draft.keepOnStop) {
                    activeDrafts[key] = draft.copy(isRemoved = true)
                } else {
                    activeDrafts.remove(key)
                }
            }
        }
        blockedRandomIds.addAll(toBlock)
        syncState()
    }

    open fun removeAllMarkedAsRemovedMessages(userId: Long, topicId: Int) {
        val keysToRemove = ArrayList<Triple<Long, Int, Long>>()
        for ((key, draft) in activeDrafts) {
            if (key.first == userId && key.second == topicId && draft.isRemoved) {
                keysToRemove.add(key)
            }
        }
        for (key in keysToRemove) {
            activeDrafts.remove(key)
        }
        if (keysToRemove.isNotEmpty()) {
            syncState()
        }
    }

    open fun checkNewMessageDraftReplacement(userId: Long, topicId: Int, messageText: String?): BotDraftMessageModel? {
        removeAllMarkedAsRemovedMessages(userId, topicId)

        var bestMatch: BotDraftMessageModel? = null
        var bestKey: Triple<Long, Int, Long>? = null

        for ((key, draft) in activeDrafts) {
            if (key.first == userId && key.second == topicId && !draft.isRemoved) {
                if (bestMatch == null) {
                    bestMatch = draft
                    bestKey = key
                }
                if (messageText != null && draft.text.isNotEmpty() && messageText.startsWith(draft.text)) {
                    bestMatch = draft
                    bestKey = key
                    break
                }
            }
        }

        if (bestKey != null && bestMatch != null) {
            activeDrafts.remove(bestKey)
            syncState()
            return bestMatch
        }

        return null
    }

    open fun onBotDraftUpdate(
        userId: Long,
        topicId: Int,
        randomId: Long,
        text: String,
        canStop: Boolean,
        keepOnStop: Boolean,
        isRich: Boolean = false
    ) {
        val key = Triple(userId, topicId, randomId)
        if (blockedRandomIds.contains(key)) {
            return
        }

        val existing = activeDrafts[key]
        val localId = existing?.localMessageId ?: nextLocalMessageId.incrementAndGet()
        val model = BotDraftMessageModel(
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

        activeDrafts[key] = model
        syncState()

        try {
            NotificationCenter.getInstance(currentAccount).postNotificationName(
                NotificationCenter.botForumDraftUpdate,
                org.telegram.messenger.BotForumHelper.BotForumTextDraftUpdateNotification(
                    userId,
                    topicId.toLong(),
                    null,
                    existing == null
                )
            )
        } catch (_: Throwable) {
            // Headless safe
        }
    }

    open fun onBotDraftTimeout(userId: Long, topicId: Int, randomId: Long) {
        val key = Triple(userId, topicId, randomId)
        val removed = activeDrafts.remove(key)
        if (removed != null) {
            syncState()
            try {
                NotificationCenter.getInstance(currentAccount).postNotificationName(
                    NotificationCenter.botForumDraftDelete,
                    org.telegram.messenger.BotForumHelper.BotForumTextDraftDeleteNotification(
                        userId,
                        topicId.toLong(),
                        removed.localMessageId
                    )
                )
            } catch (_: Throwable) {
                // Headless safe
            }
        }
    }

    open fun clearAll() {
        activeDrafts.clear()
        streamingTopics.clear()
        blockedRandomIds.clear()
        syncState()
    }

    private fun syncState() {
        _state.update {
            it.copy(
                activeDrafts = HashMap(activeDrafts),
                streamingTopics = HashSet(streamingTopics),
                blockedRandomIds = HashSet(blockedRandomIds)
            )
        }
    }
}
