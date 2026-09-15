package org.telegram.messenger.feature.messaging.bottomviews.data.datasource

import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.core.result.Result

class BottomViewsRemoteDataSource(
    private val currentAccount: Int
) {
    private val isLegacyAvailable: Boolean
        get() = ApplicationLoader.applicationContext != null

    suspend fun getRemoteBottomOverlayConfig(dialogId: Long): Result<Map<String, Boolean>> {
        return Result.Success(emptyMap())
    }
}
