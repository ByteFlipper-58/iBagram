package org.telegram.messenger.feature.security.passkeys.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account

/**
 * Remote data source executing MTProto RPC requests for Passkey authentication and registration.
 */
open class PasskeysRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    /**
     * Fetches registered passkeys for the current account.
     */
    open suspend fun getPasskeys(): Result<TL_account.Passkeys> {
        val req = TL_account.getPasskeys()
        return executeRequest(req)
    }

    /**
     * Deletes a registered passkey by ID.
     */
    open suspend fun deletePasskey(id: String): Result<TLRPC.Bool> {
        val req = TL_account.deletePasskey().apply {
            this.id = id
        }
        return executeRequest(req)
    }

    /**
     * Initiates passkey registration challenge on the MTProto server.
     */
    open suspend fun initPasskeyRegistration(): Result<TL_account.passkeyRegistrationOptions> {
        val req = TL_account.initPasskeyRegistration()
        return executeRequest(req)
    }

    /**
     * Completes passkey registration by submitting the signed credential response.
     */
    open suspend fun registerPasskey(credential: TL_account.inputPasskeyCredentialPublicKey): Result<TL_account.Passkey> {
        val req = TL_account.registerPasskey().apply {
            this.credential = credential
        }
        return executeRequest(req)
    }
}
