package org.telegram.messenger.feature.system.litemode.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource

/**
 * Remote data source for LiteMode remote presets and power saving parameters.
 */
open class LiteModeRemoteDataSource(
    currentAccount: Int = 0
) : BaseRemoteDataSource(currentAccount) {
    // System extension point for remote power saver / lite mode presets if needed
}
