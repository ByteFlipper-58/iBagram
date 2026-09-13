package org.telegram.messenger.feature.system.refreshrate.data.datasource

import org.telegram.messenger.core.result.Result

class RefreshRateRemoteDataSource {

    suspend fun isAdaptiveRefreshRateSupportedRemotely(): Result<Boolean> {
        return runCatching {
            Result.success(true)
        }.getOrElse { e ->
            Result.failure(e.message ?: "Remote check failed", e)
        }
    }
}
