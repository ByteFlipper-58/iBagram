package org.telegram.messenger.feature.system.notifications.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account

/**
 * Remote data source executing MTProto RPC requests for Telegram notification settings.
 */
open class NotificationsRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    /**
     * Updates notification settings for a specific peer or chat category on the MTProto server.
     */
    open suspend fun updateNotifySettings(
        peer: TLRPC.InputNotifyPeer,
        settings: TLRPC.TL_inputPeerNotifySettings
    ): Result<Boolean> {
        val req = TL_account.updateNotifySettings().apply {
            this.peer = peer
            this.settings = settings
        }
        val result = executeRequest<TLRPC.Bool>(req)
        return when (result) {
            is Result.Success -> Result.Success(true)
            is Result.Failure -> Result.Failure(result.error)
        }
    }

    /**
     * Updates reactions notification settings on the MTProto server.
     */
    open suspend fun setReactionsNotifySettings(
        settings: TL_account.TL_reactionsNotifySettings
    ): Result<Boolean> {
        val req = TL_account.setReactionsNotifySettings().apply {
            this.settings = settings
        }
        val result = executeRequest<TLRPC.Bool>(req)
        return when (result) {
            is Result.Success -> Result.Success(true)
            is Result.Failure -> Result.Failure(result.error)
        }
    }

    /**
     * Toggles silent status for contact sign up notifications on the MTProto server.
     */
    open suspend fun setContactSignUpNotification(silent: Boolean): Result<Boolean> {
        val req = TL_account.setContactSignUpNotification().apply {
            this.silent = silent
        }
        val result = executeRequest<TLRPC.Bool>(req)
        return when (result) {
            is Result.Success -> Result.Success(true)
            is Result.Failure -> Result.Failure(result.error)
        }
    }

    /**
     * Retrieves notification exceptions from the MTProto server.
     */
    open suspend fun getNotifyExceptions(
        compareSound: Boolean = true,
        peer: TLRPC.InputNotifyPeer? = null
    ): Result<TLRPC.Updates> {
        val req = TL_account.getNotifyExceptions().apply {
            this.compare_sound = compareSound
            if (peer != null) {
                this.peer = peer
            }
        }
        return executeRequest<TLRPC.Updates>(req)
    }

    /**
     * Resets all notification settings on the MTProto server to default values.
     */
    open suspend fun resetNotifySettings(): Result<Boolean> {
        val req = TL_account.resetNotifySettings()
        val result = executeRequest<TLRPC.Bool>(req)
        return when (result) {
            is Result.Success -> Result.Success(true)
            is Result.Failure -> Result.Failure(result.error)
        }
    }
}
