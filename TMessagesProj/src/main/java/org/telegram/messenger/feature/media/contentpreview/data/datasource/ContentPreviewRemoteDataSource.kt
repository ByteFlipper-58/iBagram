package org.telegram.messenger.feature.media.contentpreview.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result

/**
 * Remote data source for cloud content preview configuration and telemetry.
 */
open class ContentPreviewRemoteDataSource(
    currentAccount: Int = 0
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun fetchContentPreviewPolicy(): Result<Boolean> {
        return try {
            Result.Success(true)
        } catch (_: Throwable) {
            Result.Success(false)
        }
    }
}
