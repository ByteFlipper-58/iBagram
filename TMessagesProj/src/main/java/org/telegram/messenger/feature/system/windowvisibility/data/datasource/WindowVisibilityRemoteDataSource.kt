package org.telegram.messenger.feature.system.windowvisibility.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource

/**
 * Remote data source for window visibility remote configurations and policy updates.
 */
open class WindowVisibilityRemoteDataSource(
    currentAccount: Int = 0
) : BaseRemoteDataSource(currentAccount) {
    // System extension point for remote visibility configurations if needed
}
