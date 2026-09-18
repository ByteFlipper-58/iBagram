package org.telegram.messenger

import org.telegram.messenger.support.LongSparseIntArray
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC

/**
 * Controller responsible for tracking and updating blocked users and channels.
 * Handles MTProto RPC requests (contacts.block, contacts.unblock, contacts.getBlocked),
 * pagination, and synchronization with local state and NotificationCenter.
 * Extracted from MessagesController as part of Phase 4 (Batch 4.9) modularization.
 */
class BlockedPeersController(currentAccount: Int) : BaseController(currentAccount) {

    companion object {
        @JvmStatic
        private val instances = arrayOfNulls<BlockedPeersController>(UserConfig.MAX_ACCOUNT_COUNT)
        private val lockObjects = Array(UserConfig.MAX_ACCOUNT_COUNT) { Any() }

        @JvmStatic
        fun getInstance(accountNum: Int): BlockedPeersController {
            var local = instances[accountNum]
            if (local == null) {
                synchronized(lockObjects[accountNum]) {
                    local = instances[accountNum]
                    if (local == null) {
                        local = BlockedPeersController(accountNum)
                        instances[accountNum] = local
                    }
                }
            }
            return local!!
        }
    }

    @JvmField
    val blockePeers = LongSparseIntArray()

    @JvmField
    var totalBlockedCount: Int = -1

    @JvmField
    var loadingBlockedPeers: Boolean = false

    @JvmField
    var blockedEndReached: Boolean = false

    private fun runOnUIThread(runnable: Runnable) {
        try {
            AndroidUtilities.runOnUIThread(runnable)
        } catch (_: Throwable) {
            runnable.run()
        }
    }

    fun syncToLegacy() {
        val mc = try { messagesController } catch (_: Throwable) { null } ?: return
        mc.totalBlockedCount = totalBlockedCount
        mc.loadingBlockedPeers = loadingBlockedPeers
        mc.blockedEndReached = blockedEndReached
    }

    fun isBlocked(peerId: Long): Boolean {
        return blockePeers.indexOfKey(peerId) >= 0
    }

    fun onPeerBlockedChanged(peerId: Long, blocked: Boolean) {
        val index = blockePeers.indexOfKey(peerId)
        if (blocked) {
            if (index < 0) {
                blockePeers.put(peerId, 1)
                try {
                    notificationCenter?.postNotificationName(NotificationCenter.blockedUsersDidLoad)
                } catch (_: Throwable) {}
            }
        } else {
            if (index >= 0) {
                blockePeers.removeAt(index)
                try {
                    notificationCenter?.postNotificationName(NotificationCenter.blockedUsersDidLoad)
                } catch (_: Throwable) {}
            }
        }
    }

    fun blockPeer(id: Long) {
        var user: TLRPC.User? = null
        var chat: TLRPC.Chat? = null
        if (id > 0) {
            user = try { messagesController?.getUser(id) } catch (_: Throwable) { null }
            if (user == null) {
                return
            }
        } else {
            chat = try { messagesController?.getChat(-id) } catch (_: Throwable) { null }
            if (chat == null) {
                return
            }
        }
        if (blockePeers.indexOfKey(id) >= 0) {
            return
        }
        blockePeers.put(id, 1)
        if (user != null) {
            try {
                if (user.bot) {
                    mediaDataController?.removeInline(id)
                } else {
                    mediaDataController?.removePeer(id)
                }
            } catch (_: Throwable) {}
        }
        if (totalBlockedCount >= 0) {
            totalBlockedCount++
        }
        syncToLegacy()
        try {
            notificationCenter?.postNotificationName(NotificationCenter.blockedUsersDidLoad)
        } catch (_: Throwable) {}

        val req = TLRPC.TL_contacts_block()
        if (user != null) {
            req.id = try { MessagesController.getInputPeer(user) } catch (_: Throwable) { null }
        } else {
            req.id = try { MessagesController.getInputPeer(chat) } catch (_: Throwable) { null }
        }
        try {
            connectionsManager?.sendRequest(req) { _, _ -> }
        } catch (_: Throwable) {}
    }

    @JvmOverloads
    fun unblockPeer(id: Long, callback: Runnable? = null) {
        val req = TLRPC.TL_contacts_unblock()
        var user: TLRPC.User? = null
        var chat: TLRPC.Chat? = null
        if (id > 0) {
            user = try { messagesController?.getUser(id) } catch (_: Throwable) { null }
            if (user == null) {
                return
            }
        } else {
            chat = try { messagesController?.getChat(-id) } catch (_: Throwable) { null }
            if (chat == null) {
                return
            }
        }
        totalBlockedCount--
        blockePeers.delete(id)
        syncToLegacy()
        if (user != null) {
            req.id = try { MessagesController.getInputPeer(user) } catch (_: Throwable) { null }
        } else {
            req.id = try { MessagesController.getInputPeer(chat) } catch (_: Throwable) { null }
        }
        try {
            notificationCenter?.postNotificationName(NotificationCenter.blockedUsersDidLoad)
        } catch (_: Throwable) {}

        try {
            connectionsManager?.sendRequest(req) { _, _ ->
                runOnUIThread {
                    callback?.run()
                }
            }
        } catch (_: Throwable) {}
    }

    fun getBlockedPeers(reset: Boolean) {
        val isActivated = try { userConfig?.isClientActivated == true } catch (_: Throwable) { true }
        if (!isActivated || loadingBlockedPeers) {
            return
        }
        loadingBlockedPeers = true
        syncToLegacy()
        val req = TLRPC.TL_contacts_getBlocked()
        req.offset = if (reset) 0 else blockePeers.size()
        req.limit = if (reset) 20 else 100

        try {
            connectionsManager?.sendRequest(req) { response, _ ->
                runOnUIThread {
                    if (response != null) {
                        val res = response as TLRPC.contacts_Blocked
                        try {
                            messagesController?.putUsers(res.users, false)
                            messagesController?.putChats(res.chats, false)
                            messagesStorage?.putUsersAndChats(res.users, res.chats, true, true)
                        } catch (_: Throwable) {}
                        if (reset) {
                            blockePeers.clear()
                        }
                        totalBlockedCount = Math.max(res.count, res.blocked.size)
                        blockedEndReached = res.blocked.size < req.limit
                        for (a in 0 until res.blocked.size) {
                            val blocked = res.blocked[a]
                            blockePeers.put(MessageObject.getPeerId(blocked.peer_id), 1)
                        }
                        loadingBlockedPeers = false
                        syncToLegacy()
                        try {
                            notificationCenter?.postNotificationName(NotificationCenter.blockedUsersDidLoad)
                        } catch (_: Throwable) {}
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    fun cleanup() {
        blockePeers.clear()
        loadingBlockedPeers = false
        totalBlockedCount = -1
        blockedEndReached = false
        syncToLegacy()
    }
}
