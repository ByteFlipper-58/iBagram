package org.telegram.messenger.feature.media.autodeletemedia.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.autodeletemedia.domain.model.AutoDeleteRunResult

/**
 * Remote data source for cloud auto-delete media retention policies and telemetry.
 */
open class AutoDeleteMediaRemoteDataSource(
    currentAccount: Int = 0
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun fetchMediaRetentionPolicy(): Result<Boolean> {
        return try {
            Result.Success(true)
        } catch (_: Throwable) {
            Result.Success(false)
        }
    }

    open suspend fun reportAutoDeleteAnalytics(result: AutoDeleteRunResult): Result<Boolean> {
        return try {
            Result.Success(true)
        } catch (_: Throwable) {
            Result.Success(false)
        }
    }
}
