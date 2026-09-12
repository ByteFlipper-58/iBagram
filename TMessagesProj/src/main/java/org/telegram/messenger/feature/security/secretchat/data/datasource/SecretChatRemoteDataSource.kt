package org.telegram.messenger.feature.security.secretchat.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC

/**
 * Remote data source executing MTProto RPC requests for Telegram end-to-end encrypted secret chats.
 */
open class SecretChatRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    /**
     * Requests Diffie-Hellman configuration (prime p and generator g) from the MTProto server.
     */
    open suspend fun getDhConfig(
        version: Int,
        randomLength: Int = 256
    ): Result<TLRPC.messages_DhConfig> {
        val req = TLRPC.TL_messages_getDhConfig().apply {
            this.version = version
            this.random_length = randomLength
        }
        return executeRequest(req)
    }

    /**
     * Requests encryption initiation with a remote user via MTProto.
     */
    open suspend fun requestEncryption(
        userId: TLRPC.InputUser,
        randomId: Int,
        gA: ByteArray
    ): Result<TLRPC.EncryptedChat> {
        val req = TLRPC.TL_messages_requestEncryption().apply {
            this.user_id = userId
            this.random_id = randomId
            this.g_a = gA
        }
        return executeRequest(req)
    }

    /**
     * Accepts an incoming encryption request using the computed public key g_b and fingerprint.
     */
    open suspend fun acceptEncryption(
        peer: TLRPC.TL_inputEncryptedChat,
        gB: ByteArray,
        keyFingerprint: Long
    ): Result<TLRPC.EncryptedChat> {
        val req = TLRPC.TL_messages_acceptEncryption().apply {
            this.peer = peer
            this.g_b = gB
            this.key_fingerprint = keyFingerprint
        }
        return executeRequest(req, ConnectionsManager.RequestFlagInvokeAfter)
    }

    /**
     * Discards (terminates/declines) an encrypted chat on the MTProto server.
     */
    open suspend fun discardEncryption(
        chatId: Int,
        deleteHistory: Boolean
    ): Result<Boolean> {
        val req = TLRPC.TL_messages_discardEncryption().apply {
            this.chat_id = chatId
            this.delete_history = deleteHistory
        }
        val result = executeRequest<TLRPC.Bool>(req)
        return when (result) {
            is Result.Success -> Result.Success(true)
            is Result.Failure -> Result.Failure(result.error)
        }
    }

    /**
     * Sends an encrypted service message (such as TTL or screenshot notifications).
     */
    open suspend fun sendEncryptedService(
        req: TLRPC.TL_messages_sendEncryptedService
    ): Result<TLRPC.messages_SentEncryptedMessage> {
        return executeRequest(req)
    }
}
