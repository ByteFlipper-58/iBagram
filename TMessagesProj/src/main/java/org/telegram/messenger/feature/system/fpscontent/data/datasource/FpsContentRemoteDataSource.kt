package org.telegram.messenger.feature.system.fpscontent.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource

/**
 * Remote data source extension point for remote frame rate policy configuration.
 */
class FpsContentRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    suspend fun fetchTargetFpsPolicy(): Result<Int> {
        return runCatching {
            60
        }
    }
}
