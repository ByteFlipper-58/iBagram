package org.telegram.messenger.feature.system.appconfig.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.feature.system.appconfig.domain.model.AppGlobalConfigState

/**
 * Remote data source extension point for global app configuration synced from Telegram MTProto backend.
 */
class AppConfigRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    suspend fun fetchRemoteConfig(): Result<AppGlobalConfigState?> {
        return runCatching {
            null
        }
    }
}
