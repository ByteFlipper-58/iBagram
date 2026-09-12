package org.telegram.messenger.feature.security.secretchat.data.datasource

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.telegram.messenger.MessagesController
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.SecretChatHelper
import org.telegram.messenger.UserConfig
import org.telegram.messenger.core.data.BaseLocalDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLRPC
import java.util.ArrayList

/**
 * Local data source managing encrypted chat storage persistence via MessagesStorage on Dispatchers.IO,
 * in-memory caching in MessagesController, and crypto state coordination via SecretChatHelper.
 */
open class SecretChatLocalDataSource(
    currentAccount: Int,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseLocalDataSource(currentAccount, ioDispatcher) {

    private val safeStorage: MessagesStorage?
        get() = try {
            MessagesStorage.getInstance(currentAccount)
        } catch (_: Throwable) {
            null
        }

    private val safeMessagesController: MessagesController?
        get() = try {
            MessagesController.getInstance(currentAccount)
        } catch (_: Throwable) {
            null
        }

    private val safeSecretChatHelper: SecretChatHelper?
        get() = try {
            SecretChatHelper.getInstance(currentAccount)
        } catch (_: Throwable) {
            null
        }

    // --- In-Memory Cache Reads & Lookups ---

    open fun getEncryptedChat(chatId: Int): TLRPC.EncryptedChat? {
        return safeMessagesController?.getEncryptedChat(chatId)
    }

    open fun putEncryptedChat(chat: TLRPC.EncryptedChat, notify: Boolean = false) {
        safeMessagesController?.putEncryptedChat(chat, notify)
    }

    open fun getUser(userId: Long): TLRPC.User? {
        return safeMessagesController?.getUser(userId)
    }

    open fun getInputUser(user: TLRPC.User): TLRPC.InputUser? {
        return safeMessagesController?.getInputUser(user)
    }

    open fun getAllDialogs(): List<TLRPC.Dialog> {
        val controller = safeMessagesController ?: return emptyList()
        return try {
            synchronized(controller.allDialogs) {
                ArrayList(controller.allDialogs)
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    open fun isFrozen(): Boolean {
        return safeMessagesController?.isFrozen ?: false
    }

    open fun getClientUserId(): Long {
        return try {
            UserConfig.getInstance(currentAccount).clientUserId
        } catch (_: Throwable) {
            0L
        }
    }

    // --- SQLite Persistence Operations ---

    open suspend fun updateEncryptedChatTTL(chat: TLRPC.EncryptedChat): Result<Unit> {
        val storage = safeStorage ?: return Result.success(Unit)
        return runOnDb { storage.updateEncryptedChatTTL(chat) }
    }

    open suspend fun updateEncryptedChat(chat: TLRPC.EncryptedChat): Result<Unit> {
        val storage = safeStorage ?: return Result.success(Unit)
        return runOnDb { storage.updateEncryptedChat(chat) }
    }

    open suspend fun putEncryptedChat(
        chat: TLRPC.EncryptedChat,
        user: TLRPC.User?,
        dialog: TLRPC.Dialog?
    ): Result<Unit> {
        val storage = safeStorage ?: return Result.success(Unit)
        return runOnDb { storage.putEncryptedChat(chat, user, dialog) }
    }

    open fun getSecretPBytes(): ByteArray? {
        return safeStorage?.secretPBytes
    }

    open fun getSecretG(): Int {
        return safeStorage?.secretG ?: 0
    }

    open fun getLastSecretVersion(): Int {
        return safeStorage?.lastSecretVersion ?: 0
    }

    open suspend fun saveSecretParams(version: Int, g: Int, p: ByteArray?): Result<Unit> {
        val storage = safeStorage ?: return Result.success(Unit)
        return runOnDb {
            storage.setSecretPBytes(p)
            storage.setSecretG(g)
            storage.setLastSecretVersion(version)
            storage.saveSecretParams(version, g, p)
        }
    }

    // --- SecretChatHelper Delegations ---

    open fun startSecretChat(context: Context?, user: TLRPC.User) {
        safeSecretChatHelper?.startSecretChat(context, user)
    }

    open fun acceptSecretChat(chat: TLRPC.EncryptedChat) {
        safeSecretChatHelper?.acceptSecretChat(chat)
    }

    open fun declineSecretChat(chatId: Int, revoke: Boolean) {
        safeSecretChatHelper?.declineSecretChat(chatId, revoke)
    }

    open fun sendTTLMessage(chat: TLRPC.EncryptedChat, resendMessage: TLRPC.Message? = null) {
        safeSecretChatHelper?.sendTTLMessage(chat, resendMessage)
    }

    open fun sendScreenshotMessage(
        chat: TLRPC.EncryptedChat,
        randomIds: ArrayList<Long>? = null,
        resendMessage: TLRPC.Message? = null
    ) {
        safeSecretChatHelper?.sendScreenshotMessage(chat, randomIds, resendMessage)
    }
}
