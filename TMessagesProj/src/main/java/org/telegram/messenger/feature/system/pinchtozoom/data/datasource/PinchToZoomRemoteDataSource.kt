package org.telegram.messenger.feature.system.pinchtozoom.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result

open class PinchToZoomRemoteDataSource(
    currentAccount: Int = 0
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun syncPinchToZoomConfig(): Result<Unit> {
        return Result.success(Unit)
    }
}
