package org.telegram.messenger.feature.social.profile.data.datasource

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.tgnet.TLRPC

/**
 * Local data source encapsulating profile cache queries and notification center observations.
 */
open class ProfileLocalDataSource(
    private val currentAccount: Int
) {
    // In-memory test cache
    private val testUsers = ConcurrentHashMap<Long, TLRPC.User>()
    private val testUsersFull = ConcurrentHashMap<Long, TLRPC.UserFull>()
    private val testChats = ConcurrentHashMap<Long, TLRPC.Chat>()
    private val testChatsFull = ConcurrentHashMap<Long, TLRPC.ChatFull>()
    private val testBlockedPeers = ConcurrentHashMap.newKeySet<Long>()

    open fun getUser(userId: Long): TLRPC.User? {
        testUsers[userId]?.let { return it }
        return try {
            MessagesController.getInstance(currentAccount).getUser(userId)
        } catch (_: Throwable) {
            null
        }
    }

    open fun getUserFull(userId: Long): TLRPC.UserFull? {
        testUsersFull[userId]?.let { return it }
        return try {
            MessagesController.getInstance(currentAccount).getUserFull(userId)
        } catch (_: Throwable) {
            null
        }
    }

    open fun getChat(chatId: Long): TLRPC.Chat? {
        testChats[chatId]?.let { return it }
        return try {
            MessagesController.getInstance(currentAccount).getChat(chatId)
        } catch (_: Throwable) {
            null
        }
    }

    open fun getChatFull(chatId: Long): TLRPC.ChatFull? {
        testChatsFull[chatId]?.let { return it }
        return try {
            MessagesController.getInstance(currentAccount).getChatFull(chatId)
        } catch (_: Throwable) {
            null
        }
    }

    open fun isBlocked(peerId: Long): Boolean {
        if (testBlockedPeers.contains(peerId)) return true
        return try {
            val mc = MessagesController.getInstance(currentAccount)
            mc.blockePeers != null && mc.blockePeers.indexOfKey(peerId) >= 0
        } catch (_: Throwable) {
            false
        }
    }

    open fun putUser(user: TLRPC.User) {
        testUsers[user.id] = user
    }

    open fun putUserFull(userId: Long, userFull: TLRPC.UserFull) {
        testUsersFull[userId] = userFull
    }

    open fun putChat(chat: TLRPC.Chat) {
        testChats[chat.id] = chat
    }

    open fun putChatFull(chatId: Long, chatFull: TLRPC.ChatFull) {
        testChatsFull[chatId] = chatFull
    }

    open fun setBlocked(peerId: Long, blocked: Boolean) {
        if (blocked) {
            testBlockedPeers.add(peerId)
        } else {
            testBlockedPeers.remove(peerId)
        }
    }

    open fun observeProfileEvents(): Flow<Unit> = callbackFlow {
        val observer = NotificationCenter.NotificationCenterDelegate { notifId, acc, _ ->
            if (acc == currentAccount) {
                when (notifId) {
                    NotificationCenter.userInfoDidLoad,
                    NotificationCenter.chatInfoDidLoad,
                    NotificationCenter.blockedUsersDidLoad,
                    NotificationCenter.updateInterfaces -> {
                        trySend(Unit)
                    }
                }
            }
        }

        try {
            NotificationCenterFlowBridge.runOnMainThread {
                NotificationCenter.getInstance(currentAccount).addObserver(observer, NotificationCenter.userInfoDidLoad)
                NotificationCenter.getInstance(currentAccount).addObserver(observer, NotificationCenter.chatInfoDidLoad)
                NotificationCenter.getInstance(currentAccount).addObserver(observer, NotificationCenter.blockedUsersDidLoad)
                NotificationCenter.getInstance(currentAccount).addObserver(observer, NotificationCenter.updateInterfaces)
                trySend(Unit)
            }
        } catch (_: Throwable) {
            trySend(Unit)
        }

        awaitClose {
            try {
                NotificationCenterFlowBridge.runOnMainThread {
                    NotificationCenter.getInstance(currentAccount).removeObserver(observer, NotificationCenter.userInfoDidLoad)
                    NotificationCenter.getInstance(currentAccount).removeObserver(observer, NotificationCenter.chatInfoDidLoad)
                    NotificationCenter.getInstance(currentAccount).removeObserver(observer, NotificationCenter.blockedUsersDidLoad)
                    NotificationCenter.getInstance(currentAccount).removeObserver(observer, NotificationCenter.updateInterfaces)
                }
            } catch (_: Throwable) {}
        }
    }
}
