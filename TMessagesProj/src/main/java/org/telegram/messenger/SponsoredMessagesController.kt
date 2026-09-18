package org.telegram.messenger

import android.content.SharedPreferences
import android.os.SystemClock
import androidx.collection.LongSparseArray
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account
import java.util.ArrayList

/**
 * Controller responsible for managing sponsored messages (channel/bot ads),
 * fetching them via MTProto RPC, caching per dialog, and handling ad display rules.
 * Extracted from MessagesController as part of Phase 4 (Batch 4.9) modularization.
 */
class SponsoredMessagesController(currentAccount: Int) : BaseController(currentAccount) {

    companion object {
        @JvmStatic
        private val instances = arrayOfNulls<SponsoredMessagesController>(UserConfig.MAX_ACCOUNT_COUNT)
        private val lockObjects = Array(UserConfig.MAX_ACCOUNT_COUNT) { Any() }

        @JvmStatic
        fun getInstance(accountNum: Int): SponsoredMessagesController {
            var local = instances[accountNum]
            if (local == null) {
                synchronized(lockObjects[accountNum]) {
                    local = instances[accountNum]
                    if (local == null) {
                        local = SponsoredMessagesController(accountNum)
                        instances[accountNum] = local
                    }
                }
            }
            return local!!
        }
    }

    @JvmField
    val sponsoredMessages = LongSparseArray<MessagesController.SponsoredMessagesInfo>()

    @JvmField
    var channelRestrictSponsoredLevelMin: Int = 30

    @JvmField
    var sponsoredLinksInappAllow: Boolean = false

    private val preferences: SharedPreferences?
        get() = try {
            MessagesController.getMainSettings(currentAccount)
        } catch (_: Throwable) {
            null
        }

    init {
        loadSettings()
    }

    fun loadSettings() {
        preferences?.let { prefs ->
            channelRestrictSponsoredLevelMin = prefs.getInt("channelRestrictSponsoredLevelMin", 30)
            sponsoredLinksInappAllow = prefs.getBoolean("sponsoredLinksInappAllow", false)
        }
    }

    private fun runOnUIThread(runnable: Runnable) {
        try {
            AndroidUtilities.runOnUIThread(runnable)
        } catch (_: Throwable) {
            runnable.run()
        }
    }

    private fun currentElapsedRealtime(): Long {
        return try {
            SystemClock.elapsedRealtime()
        } catch (_: Throwable) {
            System.currentTimeMillis()
        }
    }

    fun getSponsoredMessages(dialogId: Long): MessagesController.SponsoredMessagesInfo? {
        var info = sponsoredMessages.get(dialogId)
        val now = currentElapsedRealtime()
        if (info != null && (info.loading || Math.abs(now - info.loadTime) <= 5 * 60 * 1000)) {
            return info
        }
        val isEligible = try {
            if (dialogId < 0) {
                ChatObject.isChannel(messagesController?.getChat(-dialogId))
            } else {
                UserObject.isBot(messagesController?.getUser(dialogId))
            }
        } catch (_: Throwable) {
            false
        }
        if (!isEligible) {
            return null
        }
        info = MessagesController.SponsoredMessagesInfo()
        info.loading = true
        sponsoredMessages.put(dialogId, info)
        val infoFinal = info

        val req = TLRPC.TL_messages_getSponsoredMessages()
        try {
            req.peer = messagesController?.getInputPeer(dialogId)
        } catch (_: Throwable) {}

        try {
            connectionsManager?.sendRequest(req) { response, _ ->
                var result: ArrayList<MessageObject>? = null
                var postsBetween: Int? = null
                if (response is TLRPC.messages_SponsoredMessages) {
                    val res = response
                    if (res.messages.isEmpty()) {
                        result = null
                        postsBetween = null
                    } else {
                        if (res is TLRPC.TL_messages_sponsoredMessages && (res.flags and 0x1) > 0) {
                            postsBetween = res.posts_between
                        } else {
                            postsBetween = null
                        }
                        result = ArrayList()
                        runOnUIThread {
                            try {
                                messagesController?.putUsers(res.users, false)
                                messagesController?.putChats(res.chats, false)
                            } catch (_: Throwable) {}
                        }
                        val usersDict = LongSparseArray<TLRPC.User>()
                        val chatsDict = LongSparseArray<TLRPC.Chat>()
                        if (res.users != null) {
                            for (a in 0 until res.users.size) {
                                val u = res.users[a]
                                usersDict.put(u.id, u)
                            }
                        }
                        if (res.chats != null) {
                            for (a in 0 until res.chats.size) {
                                val c = res.chats[a]
                                chatsDict.put(c.id, c)
                            }
                        }

                        var messageId = -10000000
                        for (a in 0 until res.messages.size) {
                            val sponsoredMessage = res.messages[a]
                            val message = TLRPC.TL_message()
                            if (!sponsoredMessage.entities.isEmpty()) {
                                message.entities = sponsoredMessage.entities
                                message.flags = message.flags or 128
                            }
                            try {
                                message.peer_id = messagesController?.getPeer(dialogId)
                            } catch (_: Throwable) {}
                            message.flags = message.flags or 256
                            message.date = connectionsManager?.currentTime ?: (System.currentTimeMillis() / 1000).toInt()
                            message.id = messageId--
                            message.message = sponsoredMessage.message
                            if (sponsoredMessage.media != null) {
                                message.flags = message.flags or 512
                            }
                            message.media = sponsoredMessage.media

                            try {
                                val messageObject = MessageObject(currentAccount, message, usersDict, chatsDict, true, true)
                                messageObject.sponsoredId = sponsoredMessage.random_id
                                messageObject.sponsoredTitle = sponsoredMessage.title
                                messageObject.sponsoredUrl = sponsoredMessage.url
                                messageObject.sponsoredRecommended = sponsoredMessage.recommended
                                messageObject.sponsoredPhoto = sponsoredMessage.photo
                                messageObject.sponsoredInfo = sponsoredMessage.sponsor_info
                                messageObject.sponsoredAdditionalInfo = sponsoredMessage.additional_info
                                messageObject.sponsoredButtonText = sponsoredMessage.button_text
                                messageObject.sponsoredCanReport = sponsoredMessage.can_report
                                messageObject.sponsoredColor = sponsoredMessage.color
                                messageObject.sponsoredMedia = sponsoredMessage.media
                                messageObject.setType()
                                messageObject.textLayoutBlocks = ArrayList()
                                messageObject.generateThumbs(true)
                                result.add(messageObject)
                            } catch (_: Throwable) {}
                        }
                    }
                } else {
                    result = null
                    postsBetween = null
                }
                runOnUIThread {
                    if (result == null) {
                        sponsoredMessages.remove(dialogId)
                    } else {
                        infoFinal.loadTime = currentElapsedRealtime()
                        infoFinal.loading = false
                        infoFinal.messages = result
                        infoFinal.posts_between = postsBetween
                        try {
                            notificationCenter?.postNotificationName(
                                NotificationCenter.didLoadSponsoredMessages,
                                dialogId,
                                result
                            )
                        } catch (_: Throwable) {}
                    }
                }
            }
        } catch (_: Throwable) {}
        return null
    }

    fun markSponsoredAsRead(dialogId: Long, messageObject: MessageObject?) {
        // sponsoredMessages.remove(dialogId)
    }

    fun disableAds(send: Boolean) {
        val clientUserId = userConfig?.clientUserId ?: 0
        val userFull = messagesController?.getUserFull(clientUserId) ?: return
        userFull.sponsored_enabled = false
        messagesStorage?.updateUserInfo(userFull, false)
        if (send) {
            val req = TL_account.toggleSponsoredMessages()
            req.enabled = false
            try {
                connectionsManager?.sendRequest(req, null)
            } catch (_: Throwable) {}
        }
    }

    fun isSponsoredDisabled(): Boolean {
        val clientUserId = userConfig?.clientUserId ?: 0
        val userFull = messagesController?.getUserFull(clientUserId) ?: return false
        return !userFull.sponsored_enabled
    }

    fun cleanup() {
        sponsoredMessages.clear()
    }
}
