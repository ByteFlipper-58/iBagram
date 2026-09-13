package org.telegram.messenger.feature.security.sessions.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account

/**
 * Remote data source encapsulating MTProto RPC operations for device authorizations,
 * web authorizations, and QR code login tokens.
 */
open class SessionsRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun getAuthorizations(): Result<TL_account.authorizations> {
        return executeRequest<TL_account.authorizations>(TL_account.getAuthorizations())
    }

    open suspend fun resetAuthorization(hash: Long): Result<TLRPC.TL_boolTrue> {
        val req = TL_account.resetAuthorization()
        req.hash = hash
        return executeRequest<TLRPC.TL_boolTrue>(req)
    }

    open suspend fun resetAllAuthorizations(): Result<TLRPC.TL_boolTrue> {
        return executeRequest<TLRPC.TL_boolTrue>(TLRPC.TL_auth_resetAuthorizations())
    }

    open suspend fun getWebAuthorizations(): Result<TL_account.webAuthorizations> {
        return executeRequest<TL_account.webAuthorizations>(TL_account.getWebAuthorizations())
    }

    open suspend fun resetWebAuthorization(hash: Long): Result<TLRPC.TL_boolTrue> {
        val req = TL_account.resetWebAuthorization()
        req.hash = hash
        return executeRequest<TLRPC.TL_boolTrue>(req)
    }

    open suspend fun resetAllWebAuthorizations(): Result<TLRPC.TL_boolTrue> {
        return executeRequest<TLRPC.TL_boolTrue>(TL_account.resetWebAuthorizations())
    }

    open suspend fun changeAuthorizationSettings(
        hash: Long,
        encryptedRequestsDisabled: Boolean,
        callRequestsDisabled: Boolean
    ): Result<TLRPC.TL_boolTrue> {
        val req = TL_account.changeAuthorizationSettings()
        req.hash = hash
        req.encrypted_requests_disabled = encryptedRequestsDisabled
        req.call_requests_disabled = callRequestsDisabled
        return executeRequest<TLRPC.TL_boolTrue>(req)
    }

    open suspend fun setAuthorizationTTL(ttlDays: Int): Result<TLRPC.TL_boolTrue> {
        val req = TL_account.setAuthorizationTTL()
        req.authorization_ttl_days = ttlDays
        return executeRequest<TLRPC.TL_boolTrue>(req)
    }

    open suspend fun acceptLoginToken(token: ByteArray): Result<TLRPC.TL_authorization> {
        val req = TLRPC.TL_auth_acceptLoginToken()
        req.token = token
        return executeRequest<TLRPC.TL_authorization>(req)
    }
}
