package org.telegram.messenger.feature.system.emudetector.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.feature.system.emudetector.domain.model.EmulatorDetectorConfig

/**
 * Remote data source extension point for emulator detection rules and blacklists.
 */
class EmuDetectorRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    suspend fun fetchEmulatorDetectorConfig(): Result<EmulatorDetectorConfig> {
        return runCatching {
            EmulatorDetectorConfig()
        }
    }
}
