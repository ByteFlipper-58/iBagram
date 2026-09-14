package org.telegram.messenger.feature.system.maintabs.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result

open class MainTabsRemoteDataSource(
    currentAccount: Int = 0
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun fetchRemoteTabsConfig(): Result<Unit> {
        return Result.success(Unit)
    }
}
