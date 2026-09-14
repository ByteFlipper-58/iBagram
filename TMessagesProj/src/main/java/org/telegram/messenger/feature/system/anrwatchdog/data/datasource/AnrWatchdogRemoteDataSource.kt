package org.telegram.messenger.feature.system.anrwatchdog.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.feature.system.anrwatchdog.domain.model.AnrIncident

/**
 * Remote data source extension point for ANR telemetry and incident reporting.
 */
class AnrWatchdogRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    suspend fun reportAnrIncident(incident: AnrIncident): Result<Boolean> {
        return runCatching {
            true
        }
    }
}
