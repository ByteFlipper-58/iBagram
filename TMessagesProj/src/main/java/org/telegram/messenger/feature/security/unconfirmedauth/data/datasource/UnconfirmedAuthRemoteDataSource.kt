package org.telegram.messenger.feature.security.unconfirmedauth.data.datasource

import kotlinx.coroutines.suspendCancellableCoroutine
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.MessagesController
import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account
import kotlin.coroutines.resume

/**
 * Remote data source for MTProto Unconfirmed Authorization actions.
 */
open class UnconfirmedAuthRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun confirmBotAuth(botId: Long): Result<Boolean> =
        suspendCancellableCoroutine { continuation ->
            val req = TL_account.confirmBotConnection().apply {
                bot_id = MessagesController.getInstance(currentAccount).getInputUser(botId)
            }
            val reqId = connectionsManager.sendRequestTyped(req) { res, err ->
                if (continuation.isActive) {
                    if (err != null) {
                        continuation.resume(Result.Failure(AppError.Network(err.text ?: "Confirm bot failed")))
                    } else {
                        continuation.resume(Result.Success(res is TLRPC.TL_boolTrue))
                    }
                }
            }
            continuation.invokeOnCancellation {
                connectionsManager.cancelRequest(reqId, true)
            }
        }

    open suspend fun confirmUserAuth(hash: Long): Result<Boolean> =
        suspendCancellableCoroutine { continuation ->
            val req = TL_account.changeAuthorizationSettings().apply {
                this.hash = hash
                this.confirmed = true
            }
            val reqId = connectionsManager.sendRequest(req) { res, err ->
                AndroidUtilities.runOnUIThread {
                    if (continuation.isActive) {
                        if (err != null) {
                            continuation.resume(Result.Failure(AppError.Network(err.text ?: "Confirm auth failed")))
                        } else {
                            continuation.resume(Result.Success(res is TLRPC.TL_boolTrue))
                        }
                    }
                }
            }
            continuation.invokeOnCancellation {
                connectionsManager.cancelRequest(reqId, true)
            }
        }

    open suspend fun denyBotAuth(botId: Long): Result<Boolean> =
        suspendCancellableCoroutine { continuation ->
            val req = TL_account.updateConnectedBot().apply {
                this.deleted = true
                this.bot = MessagesController.getInstance(currentAccount).getInputUser(botId)
                this.recipients = TL_account.TL_inputBusinessBotRecipients()
            }
            val reqId = connectionsManager.sendRequest(req) { res, err ->
                AndroidUtilities.runOnUIThread {
                    if (res is TLRPC.Updates) {
                        MessagesController.getInstance(currentAccount).processUpdates(res, false)
                    }
                    if (continuation.isActive) {
                        if (err != null) {
                            continuation.resume(Result.Failure(AppError.Network(err.text ?: "Deny bot failed")))
                        } else {
                            continuation.resume(Result.Success(res is TLRPC.Updates))
                        }
                    }
                }
            }
            continuation.invokeOnCancellation {
                connectionsManager.cancelRequest(reqId, true)
            }
        }

    open suspend fun denyUserAuth(hash: Long): Result<Boolean> =
        suspendCancellableCoroutine { continuation ->
            val req = TL_account.resetAuthorization().apply {
                this.hash = hash
            }
            val reqId = connectionsManager.sendRequest(req) { res, err ->
                AndroidUtilities.runOnUIThread {
                    if (continuation.isActive) {
                        if (err != null) {
                            continuation.resume(Result.Failure(AppError.Network(err.text ?: "Deny auth failed")))
                        } else {
                            continuation.resume(Result.Success(res is TLRPC.TL_boolTrue))
                        }
                    }
                }
            }
            continuation.invokeOnCancellation {
                connectionsManager.cancelRequest(reqId, true)
            }
        }
}
