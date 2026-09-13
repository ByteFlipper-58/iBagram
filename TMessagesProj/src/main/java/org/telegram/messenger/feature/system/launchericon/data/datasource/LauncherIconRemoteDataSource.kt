package org.telegram.messenger.feature.system.launchericon.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource

/**
 * Remote data source for launcher icon remote configuration and premium badges.
 */
open class LauncherIconRemoteDataSource(
    currentAccount: Int = 0
) : BaseRemoteDataSource(currentAccount) {
    // System extension point for remote launcher icon configs if needed
}
