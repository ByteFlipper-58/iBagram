package org.telegram.messenger.feature.security.authtokens.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account

/**
 * Remote data source encapsulating MTProto authentication token invalidation and verification.
 */
open class AuthTokensRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun dropTempAuthKey(key: Long): Result<Boolean> {
        // MTProto drop temp key RPC if requested
        return Result.success(true)
    }

    open suspend fun resetAuthorization(hash: Long): Result<Boolean> {
        val req = TL_account.resetAuthorization()
        req.hash = hash
        val result: Result<TLRPC.Bool> = executeRequest(req)
        return when (result) {
            is Result.Success -> Result.success(true)
            is Result.Failure -> Result.failure(result.error)
        }
    }
}
