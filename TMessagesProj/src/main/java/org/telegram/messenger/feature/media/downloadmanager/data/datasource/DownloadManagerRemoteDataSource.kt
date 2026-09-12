package org.telegram.messenger.feature.media.downloadmanager.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account

/**
 * Remote data source executing MTProto RPC requests for auto-download settings.
 */
open class DownloadManagerRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    /**
     * Fetches default and user auto-download settings presets from MTProto server.
     */
    open suspend fun getAutoDownloadConfig(): Result<TL_account.autoDownloadSettings> {
        val req = TL_account.getAutoDownloadSettings()
        return executeRequest(req)
    }

    /**
     * Saves customized auto-download settings preset to MTProto server.
     */
    open suspend fun saveAutoDownloadSettings(
        settings: TLRPC.TL_autoDownloadSettings,
        low: Boolean = false,
        high: Boolean = false
    ): Result<Boolean> {
        val req = TL_account.saveAutoDownloadSettings().apply {
            this.settings = settings
            this.low = low
            this.high = high
        }
        val result = executeRequest<TLRPC.Bool>(req)
        return when (result) {
            is Result.Success -> Result.Success(true)
            is Result.Failure -> Result.Failure(result.error)
        }
    }
}
