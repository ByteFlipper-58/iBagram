package org.telegram.messenger.feature.social.joinrequests.data.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.telegram.messenger.MemberRequestsController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.core.data.BaseLocalDataSource
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_update

/**
 * Local data source managing pending join requests caching, importers cache,
 * and updates processing via MessagesController and MemberRequestsController.
 */
open class JoinRequestsLocalDataSource(
    currentAccount: Int
) : BaseLocalDataSource(currentAccount) {

    private val messagesController: MessagesController?
        get() = try {
            MessagesController.getInstance(currentAccount)
        } catch (e: Throwable) {
            null
        }

    private val memberRequestsController: MemberRequestsController?
        get() = try {
            MemberRequestsController.getInstance(currentAccount)
        } catch (e: Throwable) {
            null
        }

    /**
     * Resolves the ChatFull details for a chat to inspect pending requests count.
     */
    open fun getChatFull(chatId: Long): TLRPC.ChatFull? {
        return messagesController?.getChatFull(chatId)
    }

    /**
     * Resolves an InputPeer for the specified dialog identifier.
     */
    open fun getInputPeer(dialogId: Long): TLRPC.InputPeer? {
        return messagesController?.getInputPeer(dialogId)
    }

    /**
     * Retrieves cached user by ID.
     */
    open fun getUser(userId: Long): TLRPC.User? {
        return messagesController?.getUser(userId)
    }

    /**
     * Resolves an InputUser for a given User.
     */
    open fun getInputUser(user: TLRPC.User): TLRPC.InputUser? {
        return messagesController?.getInputUser(user)
    }

    /**
     * Retrieves the first page of importers cached in-memory.
     */
    open fun getCachedImporters(chatId: Long): TLRPC.TL_messages_chatInviteImporters? {
        return memberRequestsController?.getCachedImporters(chatId)
    }

    /**
     * Caches the first page of importers into MemberRequestsController.
     */
    open fun putCachedImporters(chatId: Long, importers: TLRPC.TL_messages_chatInviteImporters) {
        memberRequestsController?.putCachedImporters(chatId, importers)
    }

    /**
     * Processes server updates and refreshes chat information.
     */
    open suspend fun processUpdates(updates: TLRPC.Updates): Unit = withContext(Dispatchers.Main) {
        val controller = messagesController ?: return@withContext
        controller.processUpdates(updates, false)
        if (updates is TLRPC.TL_updates && updates.chats.isNotEmpty()) {
            controller.loadFullChat(updates.chats[0].id, 0, true)
        }
    }

    /**
     * Handles real-time push update for pending join requests.
     */
    open suspend fun onPendingRequestsUpdated(update: TL_update.TL_updatePendingJoinRequests): Unit = withContext(Dispatchers.Main) {
        memberRequestsController?.onPendingRequestsUpdated(update)
    }
}
