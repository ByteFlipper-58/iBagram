package org.telegram.messenger.feature.system.floatingdebug.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource

/**
 * Remote data source extension point for remote debug items and developer overrides.
 */
class FloatingDebugRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    suspend fun fetchRemoteDebugFlags(): Result<Map<String, Boolean>> {
        return runCatching {
            emptyMap()
        }
    }
}
