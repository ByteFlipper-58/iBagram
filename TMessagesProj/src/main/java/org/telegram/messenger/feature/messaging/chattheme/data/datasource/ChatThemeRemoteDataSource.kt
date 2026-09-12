package org.telegram.messenger.feature.messaging.chattheme.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account

/**
 * Remote data source encapsulating MTProto RPC calls for chat themes.
 */
open class ChatThemeRemoteDataSource(currentAccount: Int) : BaseRemoteDataSource(currentAccount) {

    open suspend fun getChatThemes(hash: Long): Result<TLObject> {
        val request = TL_account.getChatThemes().apply {
            this.hash = hash
        }
        return executeRequest(request)
    }

    open suspend fun setChatTheme(peer: TLRPC.InputPeer, theme: TLRPC.InputChatTheme): Result<TLRPC.Updates> {
        val request = TLRPC.TL_messages_setChatTheme().apply {
            this.peer = peer
            this.theme = theme
        }
        return executeRequest(request)
    }

    open suspend fun getUniqueGiftChatThemes(hash: Long, offset: String?, limit: Int = 50): Result<TLObject> {
        val request = TL_account.Tl_getUniqueGiftChatThemes().apply {
            this.hash = hash
            this.offset = offset
            this.limit = limit
        }
        return executeRequest(request)
    }
}
