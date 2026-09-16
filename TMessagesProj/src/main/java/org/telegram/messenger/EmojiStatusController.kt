package org.telegram.messenger

import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.feature.messaging.reactions.domain.repository.ReactionsRepository
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account
import org.telegram.tgnet.tl.TL_stars
import org.telegram.ui.Stars.StarsController
import java.util.concurrent.ConcurrentHashMap

/**
 * Controller extracted from MessagesController to modularize Emoji Status management.
 * Written in Kotlin to drive the codebase migration towards modern language standards while
 * preserving 100% Java interop and bridging directly into ReactionsRepository.
 */
class EmojiStatusController(currentAccount: Int) : BaseController(currentAccount) {

    companion object {
        @JvmStatic
        private val instances = arrayOfNulls<EmojiStatusController>(UserConfig.MAX_ACCOUNT_COUNT)

        @JvmStatic
        fun getInstance(accountNum: Int): EmojiStatusController {
            var local = instances[accountNum]
            if (local == null) {
                synchronized(EmojiStatusController::class.java) {
                    local = instances[accountNum]
                    if (local == null) {
                        local = EmojiStatusController(accountNum)
                        instances[accountNum] = local
                    }
                }
            }
            return local!!
        }

        @JvmStatic
        fun getReactionsRepository(account: Int): ReactionsRepository {
            return AccountFeatureContainer.get(account).messaging.reactionsRepository
        }

        @JvmStatic
        fun emojiStatusCollectibleFromGift(gift: TL_stars.TL_starGiftUnique): TLRPC.TL_emojiStatusCollectible {
            val status = TLRPC.TL_emojiStatusCollectible()
            status.collectible_id = gift.id
            val model = StarsController.findAttribute(gift.attributes, TL_stars.starGiftAttributeModel::class.java)
            val backdrop = StarsController.findAttribute(gift.attributes, TL_stars.starGiftAttributeBackdrop::class.java)
            val pattern = StarsController.findAttribute(gift.attributes, TL_stars.starGiftAttributePattern::class.java)
            status.title = gift.title + " #" + gift.num
            if (model != null) {
                status.document_id = model.document.id
            }
            if (pattern != null) {
                status.pattern_document_id = pattern.document.id
            }
            if (backdrop != null) {
                status.center_color = backdrop.center_color
                status.edge_color = backdrop.edge_color
                status.text_color = backdrop.text_color
                status.pattern_color = backdrop.pattern_color
            }
            return status
        }
    }

    val reactionsRepository: ReactionsRepository
        get() = getReactionsRepository(currentAccount)

    private val emojiStatusUntilValues = ConcurrentHashMap<Long, Int>()
    private var recentEmojiStatusUpdateRunnableTime: Long = 0
    private var recentEmojiStatusUpdateRunnableTimeout: Long = 0
    private var recentEmojiStatusUpdateRunnable: Runnable? = null

    fun updateEmojiStatus(newStatus: TLRPC.EmojiStatus) {
        updateEmojiStatus(newStatus, null)
    }

    fun updateEmojiStatus(newStatus: TLRPC.EmojiStatus, gift: TL_stars.StarGift?) {
        updateEmojiStatus(0, newStatus, gift)
    }

    fun updateEmojiStatus(dialogId: Long, newStatus: TLRPC.EmojiStatus, gift: TL_stars.StarGift?) {
        val myself = dialogId == 0L || dialogId == userConfig.clientUserId
        var new_emoji_status = newStatus
        if (new_emoji_status is TLRPC.TL_inputEmojiStatusCollectible && gift is TL_stars.TL_starGiftUnique) {
            new_emoji_status = emojiStatusCollectibleFromGift(gift)
        }

        val r: TLObject
        if (myself) {
            val req = TL_account.updateEmojiStatus()
            req.emoji_status = newStatus
            r = req

            val user = userConfig.currentUser
            if (user != null) {
                user.emoji_status = new_emoji_status
                notificationCenter.postNotificationName(NotificationCenter.userEmojiStatusUpdated, user)
            }
        } else {
            val req = TLRPC.TL_channels_updateEmojiStatus()
            req.channel = messagesController.getInputChannel(-dialogId)
            req.emoji_status = newStatus
            r = req

            val chat = messagesController.getChat(-dialogId)
            if (chat != null) {
                chat.flags = chat.flags or 512
                chat.emoji_status = new_emoji_status
                messagesController.putChat(chat, true)
            }
        }
        updateEmojiStatusUntilUpdate(dialogId, new_emoji_status)
        notificationCenter.postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_EMOJI_STATUS)
        connectionsManager.sendRequest(r, null)
    }

    fun updateEmojiStatusUntilUpdate(dialogId: Long, status: TLRPC.EmojiStatus?) {
        val until = DialogObject.getEmojiStatusUntil(status)
        if (until != 0) {
            emojiStatusUntilValues[dialogId] = until
        } else {
            if (!emojiStatusUntilValues.containsKey(dialogId)) {
                return
            }
            emojiStatusUntilValues.remove(dialogId)
        }
        updateEmojiStatusUntil()
    }

    fun updateEmojiStatusUntil() {
        val now = (System.currentTimeMillis() / 1000L).toInt()
        var timeout: Long? = null
        val it = emojiStatusUntilValues.keys.iterator()
        while (it.hasNext()) {
            val until = emojiStatusUntilValues[it.next()] ?: 0
            if (until > now) {
                val diff = (until - now).toLong()
                timeout = Math.min(timeout ?: Long.MAX_VALUE, diff)
            } else {
                it.remove()
            }
        }

        if (timeout != null) {
            timeout += 2
            if (now + timeout != recentEmojiStatusUpdateRunnableTime + recentEmojiStatusUpdateRunnableTimeout) {
                AndroidUtilities.cancelRunOnUIThread(recentEmojiStatusUpdateRunnable)
                recentEmojiStatusUpdateRunnableTime = now.toLong()
                recentEmojiStatusUpdateRunnableTimeout = timeout
                recentEmojiStatusUpdateRunnable = Runnable {
                    notificationCenter.postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_EMOJI_STATUS)
                    updateEmojiStatusUntil()
                }
                AndroidUtilities.runOnUIThread(recentEmojiStatusUpdateRunnable, timeout * 1000)
            }
        } else if (recentEmojiStatusUpdateRunnable != null) {
            recentEmojiStatusUpdateRunnableTime = -1
            recentEmojiStatusUpdateRunnableTimeout = -1
            AndroidUtilities.cancelRunOnUIThread(recentEmojiStatusUpdateRunnable)
        }
    }
}
