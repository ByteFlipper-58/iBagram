package org.telegram.messenger.feature.messaging.draftmeasure.data.datasource

import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.core.result.Result

class DraftMeasureRemoteDataSource(
    private val currentAccount: Int
) {
    private val isLegacyAvailable: Boolean
        get() = ApplicationLoader.applicationContext != null

    suspend fun getDraftViewportConfig(): Result<Map<String, Any>> {
        return Result.Success(emptyMap())
    }
}
