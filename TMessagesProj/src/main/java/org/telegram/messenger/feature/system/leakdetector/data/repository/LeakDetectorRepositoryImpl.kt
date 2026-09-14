package org.telegram.messenger.feature.system.leakdetector.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.system.leakdetector.data.datasource.LeakDetectorLocalDataSource
import org.telegram.messenger.feature.system.leakdetector.data.datasource.LeakDetectorRemoteDataSource
import org.telegram.messenger.feature.system.leakdetector.domain.model.LeakDetectorConfig
import org.telegram.messenger.feature.system.leakdetector.domain.model.LeakDetectorState
import org.telegram.messenger.feature.system.leakdetector.domain.model.LeakReport
import org.telegram.messenger.feature.system.leakdetector.domain.model.TrackedClassStats
import org.telegram.messenger.feature.system.leakdetector.domain.repository.LeakDetectorRepository

/**
 * Implementation of [LeakDetectorRepository] coordinating clean local and remote data sources.
 */
class LeakDetectorRepositoryImpl(
    private val localDataSource: LeakDetectorLocalDataSource,
    private val remoteDataSource: LeakDetectorRemoteDataSource
) : LeakDetectorRepository {

    override fun start(config: LeakDetectorConfig) {
        localDataSource.start(config)
    }

    override fun stop() {
        localDataSource.stop()
    }

    override fun track(tagOrClassName: String, instance: Any) {
        localDataSource.track(tagOrClassName, instance)
    }

    override fun triggerCheck(): List<LeakReport> {
        return localDataSource.triggerCheck()
    }

    override fun confirmLeak(className: String): LeakReport? {
        return localDataSource.confirmLeak(className)
    }

    override fun getLiveCount(className: String): Int {
        return localDataSource.getLiveCount(className)
    }

    override fun getReportedLeaks(): List<LeakReport> {
        return localDataSource.getReportedLeaks()
    }

    override fun getTrackedStats(): List<TrackedClassStats> {
        return localDataSource.getTrackedStats()
    }

    override fun reset() {
        localDataSource.reset()
    }

    override fun observeState(): StateFlow<LeakDetectorState> {
        return localDataSource.observeState()
    }

    override fun observeLeaks(): Flow<LeakReport> {
        return localDataSource.observeLeaks()
    }
}
