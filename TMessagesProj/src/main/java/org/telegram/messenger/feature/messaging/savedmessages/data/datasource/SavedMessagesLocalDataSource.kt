package org.telegram.messenger.feature.messaging.savedmessages.data.datasource

import org.telegram.messenger.MessagesController
import org.telegram.messenger.SavedMessagesController
import org.telegram.messenger.core.data.BaseLocalDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLRPC

/**
 * Local data source for Saved Messages operations accessing MessagesStorage (SQLite)
 * and in-memory cache on the IO dispatcher.
 */
open class SavedMessagesLocalDataSource(
    currentAccount: Int
) : BaseLocalDataSource(currentAccount) {

    /**
     * Retrieves currently loaded saved dialogs from the in-memory cache of the controller.
     */
    open fun getCachedDialogs(): List<SavedMessagesController.SavedDialog> {
        val controller = try {
            MessagesController.getInstance(currentAccount)?.savedMessagesController
        } catch (e: Throwable) {
            null
        }
        return controller?.allDialogs ?: emptyList()
    }

    /**
     * Persists users and chats returned by saved dialogs response into local database.
     */
    suspend fun putUsersAndChats(
        users: ArrayList<TLRPC.User>,
        chats: ArrayList<TLRPC.Chat>
    ): Result<Unit> = runOnDb { storage ->
        storage.putUsersAndChats(users, chats, true, true)
    }

    /**
     * Persists message objects into local database for Saved Messages.
     */
    suspend fun putMessages(
        messages: ArrayList<TLRPC.Message>
    ): Result<Unit> = runOnDb { storage ->
        storage.putMessages(messages, false, true, false, 0, false, 2, 0)
    }
}
