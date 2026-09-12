package org.telegram.messenger.feature.social.boosts.data.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.ChannelBoostsController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.core.data.BaseLocalDataSource
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_stories
import java.util.ArrayList
import kotlin.coroutines.resume

/**
 * Local data source managing peer resolution, entity caching, and boost eligibility evaluation.
 */
open class BoostsLocalDataSource(
    currentAccount: Int
) : BaseLocalDataSource(currentAccount) {

    private val messagesController: MessagesController?
        get() = try {
            MessagesController.getInstance(currentAccount)
        } catch (e: Throwable) {
            null
        }

    private val channelBoostsController: ChannelBoostsController?
        get() = try {
            MessagesController.getInstance(currentAccount).boostsController
        } catch (e: Throwable) {
            null
        }

    /**
     * Resolves an InputPeer for the specified dialog identifier.
     */
    open fun getInputPeer(dialogId: Long): TLRPC.InputPeer? {
        return messagesController?.getInputPeer(dialogId)
    }

    /**
     * Updates in-memory caches of users and chats in MessagesController.
     */
    open fun putUsersAndChats(users: ArrayList<TLRPC.User>?, chats: ArrayList<TLRPC.Chat>?) {
        users?.let { messagesController?.putUsers(it, false) }
        chats?.let { messagesController?.putChats(it, false) }
    }

    /**
     * Evaluates boost eligibility, checking available slots, replacement slots, and flood wait.
     */
    open suspend fun checkCanApplyBoost(
        dialogId: Long,
        boostsStatus: TL_stories.TL_premium_boostsStatus
    ): ChannelBoostsController.CanApplyBoost? = withContext(Dispatchers.Main) {
        val controller = channelBoostsController ?: return@withContext null
        suspendCancellableCoroutine { cont ->
            controller.userCanBoostChannel(dialogId, boostsStatus) { canApply ->
                if (cont.isActive) {
                    cont.resume(canApply)
                }
            }
        }
    }
}
