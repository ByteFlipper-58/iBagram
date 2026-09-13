package org.telegram.messenger.feature.security.botguard.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC

/**
 * Remote data source encapsulating MTProto RPC operations for Bot Guard verification and web app queries.
 */
open class BotGuardRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun <T : TLObject> sendBotGuardRequest(request: TLObject): Result<T> {
        return executeRequest(request)
    }
}
