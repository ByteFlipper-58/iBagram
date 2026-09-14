package org.telegram.messenger.feature.system.emudetector.data.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.system.emudetector.data.datasource.EmuDetectorLocalDataSource
import org.telegram.messenger.feature.system.emudetector.data.datasource.EmuDetectorRemoteDataSource
import org.telegram.messenger.feature.system.emudetector.domain.model.EmulatorDetectorConfig
import org.telegram.messenger.feature.system.emudetector.domain.model.EmulatorDiagnostics
import org.telegram.messenger.feature.system.emudetector.domain.repository.EmuDetectorRepository

/**
 * Implementation of [EmuDetectorRepository] coordinating clean local and remote data sources.
 */
class EmuDetectorRepositoryImpl(
    private val localDataSource: EmuDetectorLocalDataSource,
    private val remoteDataSource: EmuDetectorRemoteDataSource
) : EmuDetectorRepository {

    override suspend fun detectEnvironment(forceRefresh: Boolean): EmulatorDiagnostics {
        return localDataSource.detectEnvironment(forceRefresh)
    }

    override fun getCachedDiagnostics(): EmulatorDiagnostics? {
        return localDataSource.getCachedDiagnostics()
    }

    override fun isEmulator(): Boolean {
        return localDataSource.isEmulator()
    }

    override fun getConfig(): EmulatorDetectorConfig {
        return localDataSource.getConfig()
    }

    override fun updateConfig(config: EmulatorDetectorConfig) {
        localDataSource.updateConfig(config)
    }

    override fun addCustomPackage(packageName: String) {
        localDataSource.addCustomPackage(packageName)
    }

    override fun clearCache() {
        localDataSource.clearCache()
    }

    override fun observeDiagnostics(): StateFlow<EmulatorDiagnostics?> {
        return localDataSource.observeDiagnostics()
    }

    override fun observeIsEmulator(): StateFlow<Boolean> {
        return localDataSource.observeIsEmulator()
    }
}
