package org.telegram.messenger.feature.security.privacy.data.datasource

import kotlinx.coroutines.suspendCancellableCoroutine
import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account
import kotlin.coroutines.resume

/**
 * Remote data source managing MTProto RPC requests for privacy rules and 2FA password configuration.
 */
open class PrivacyRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun setPrivacy(
        key: TLRPC.InputPrivacyKey,
        rules: List<TLRPC.InputPrivacyRule>
    ): Result<Unit> {
        val req = TL_account.setPrivacy().apply {
            this.key = key
            this.rules.addAll(rules)
        }
        return executeRequest<org.telegram.tgnet.TLObject>(req).map { }
    }

    open suspend fun loadTwoStepVerification(): Result<TL_account.Password> = suspendCancellableCoroutine { cont ->
        try {
            val req = TL_account.getPassword()
            val cm = ConnectionsManager.getInstance(currentAccount)
            cm.sendRequest(req, { response, error ->
                if (error != null) {
                    cont.resume(Result.failure(AppError.Generic(error.text ?: "Failed to load 2FA settings")))
                } else if (response is TL_account.Password) {
                    cont.resume(Result.success(response))
                } else {
                    cont.resume(Result.failure(AppError.Generic("Invalid response for 2FA settings")))
                }
            }, ConnectionsManager.RequestFlagFailOnServerErrors or ConnectionsManager.RequestFlagWithoutLogin)
        } catch (e: Throwable) {
            cont.resume(Result.failure(AppError.Generic("Exception loading 2FA", e)))
        }
    }
}
