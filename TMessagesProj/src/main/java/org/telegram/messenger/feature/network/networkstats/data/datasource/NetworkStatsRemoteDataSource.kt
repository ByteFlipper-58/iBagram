package org.telegram.messenger.feature.network.networkstats.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result

/**
 * Remote data source for network stats MTProto extension points.
 */
open class NetworkStatsRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun syncNetworkStats(): Result<Unit> {
        return Result.Success(Unit)
    }

    open suspend fun resetRemoteStats(): Result<Unit> {
        return Result.Success(Unit)
    }
}
