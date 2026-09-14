package org.telegram.messenger.feature.system.settings.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account

/**
 * Remote data source for cloud-backed settings synchronization via MTProto.
 */
open class SettingsRemoteDataSource(
    currentAccount: Int = 0
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun fetchGlobalPrivacySettings(): Result<Boolean> {
        return try {
            val req = TL_account.getGlobalPrivacySettings()
            val result = executeRequest<TLRPC.GlobalPrivacySettings>(req)
            if (result.isSuccess) {
                Result.Success(true)
            } else {
                Result.Success(false)
            }
        } catch (_: Throwable) {
            Result.Success(false)
        }
    }
}
