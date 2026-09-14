package org.telegram.messenger.feature.system.datastorage.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result

/**
 * Remote data source extension point for cloud storage limits, cleanup policies,
 * and remote auto-download profiles.
 */
class DataStorageRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    suspend fun fetchStoragePolicy(): Result<Boolean> {
        return Result.Success(true)
    }
}
