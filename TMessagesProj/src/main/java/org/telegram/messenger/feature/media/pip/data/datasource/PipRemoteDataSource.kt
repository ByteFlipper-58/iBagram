package org.telegram.messenger.feature.media.pip.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result

/**
 * Remote/system data source for Picture-in-Picture operations,
 * handling system PiP window parameters and media session action dispatch.
 */
open class PipRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun dispatchPipAction(tag: String, actionId: Int): Result<Unit> {
        return try {
            // Extension point for remote media session or notification action dispatch
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to dispatch PiP action", e))
        }
    }

    open suspend fun applyPictureInPictureParams(aspectRatioWidth: Int, aspectRatioHeight: Int): Result<Unit> {
        return try {
            // Extension point for system-level picture-in-picture params application
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to apply PiP params", e))
        }
    }
}
