package org.telegram.messenger.feature.system.countdowntimer.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.system.countdowntimer.data.datasource.CountdownTimerLocalDataSource
import org.telegram.messenger.feature.system.countdowntimer.data.datasource.CountdownTimerRemoteDataSource
import org.telegram.messenger.feature.system.countdowntimer.domain.model.CountdownTimerState
import org.telegram.messenger.feature.system.countdowntimer.domain.model.CountdownTimerTick
import org.telegram.messenger.feature.system.countdowntimer.domain.repository.CountdownTimerRepository

/**
 * Implementation of [CountdownTimerRepository] coordinating countdown timers
 * and remote time offset synchronization.
 */
class CountdownTimerRepositoryImpl(
    private val localDataSource: CountdownTimerLocalDataSource,
    private val remoteDataSource: CountdownTimerRemoteDataSource
) : CountdownTimerRepository {

    override fun start(timerId: String, seconds: Long): CountdownTimerTick {
        return localDataSource.start(timerId, seconds)
    }

    override fun stop(timerId: String): CountdownTimerTick? {
        return localDataSource.stop(timerId)
    }

    override fun pause(timerId: String): CountdownTimerTick? {
        return localDataSource.pause(timerId)
    }

    override fun resume(timerId: String): CountdownTimerTick? {
        return localDataSource.resume(timerId)
    }

    override fun getTimer(timerId: String): CountdownTimerTick? {
        return localDataSource.getTimer(timerId)
    }

    override fun isRunning(timerId: String): Boolean {
        return localDataSource.isRunning(timerId)
    }

    override fun tick(timerId: String, stepSeconds: Long): CountdownTimerTick? {
        return localDataSource.tick(timerId, stepSeconds)
    }

    override fun clearAll(): CountdownTimerState {
        return localDataSource.clearAll()
    }

    override fun observeTimer(timerId: String): Flow<CountdownTimerTick> {
        return localDataSource.observeTimer(timerId)
    }

    override fun observeState(): StateFlow<CountdownTimerState> {
        return localDataSource.observeState()
    }
}
