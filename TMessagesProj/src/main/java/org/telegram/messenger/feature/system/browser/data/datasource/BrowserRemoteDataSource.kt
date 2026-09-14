package org.telegram.messenger.feature.system.browser.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource

/**
 * Remote data source extension point for safe browsing policies, URL blacklists,
 * and malicious domain telemetry.
 */
class BrowserRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    suspend fun checkMaliciousDomain(host: String): Result<Boolean> {
        return runCatching {
            false
        }
    }
}
