package org.telegram.messenger.feature.system.leakdetector.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.feature.system.leakdetector.domain.model.LeakReport

/**
 * Remote data source extension point for memory leak telemetry and incident reporting.
 */
class LeakDetectorRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    suspend fun reportMemoryLeak(report: LeakReport): Result<Boolean> {
        return runCatching {
            true
        }
    }
}
