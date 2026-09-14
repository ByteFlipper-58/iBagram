package org.telegram.messenger.feature.system.adjustpan.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result

open class AdjustPanRemoteDataSource(
    currentAccount: Int = 0
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun syncAdjustPanConfig(): Result<Unit> {
        return Result.success(Unit)
    }
}
