package org.telegram.messenger.feature.system.countdowntimer.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource

/**
 * Remote data source extension point for NTP server time synchronization
 * and remote timer calibration.
 */
class CountdownTimerRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    suspend fun fetchServerTimeOffset(): Result<Long> {
        return runCatching {
            0L
        }
    }
}
