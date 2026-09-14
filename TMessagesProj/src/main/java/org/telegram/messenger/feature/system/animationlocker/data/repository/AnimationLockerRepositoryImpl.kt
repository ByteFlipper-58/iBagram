package org.telegram.messenger.feature.system.animationlocker.data.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.system.animationlocker.data.datasource.AnimationLockerLocalDataSource
import org.telegram.messenger.feature.system.animationlocker.data.datasource.AnimationLockerRemoteDataSource
import org.telegram.messenger.feature.system.animationlocker.domain.model.AnimationLockRecord
import org.telegram.messenger.feature.system.animationlocker.domain.model.AnimationLockerConfig
import org.telegram.messenger.feature.system.animationlocker.domain.model.AnimationLockerState
import org.telegram.messenger.feature.system.animationlocker.domain.model.LockScope
import org.telegram.messenger.feature.system.animationlocker.domain.repository.AnimationLockerRepository

/**
 * Implementation of [AnimationLockerRepository] coordinating clean local and remote data sources.
 */
class AnimationLockerRepositoryImpl(
    private val localDataSource: AnimationLockerLocalDataSource,
    private val remoteDataSource: AnimationLockerRemoteDataSource
) : AnimationLockerRepository {

    override fun acquireLock(
        tag: String?,
        allowedNotificationIds: Set<Int>,
        scope: LockScope
    ): AnimationLockRecord {
        return localDataSource.acquireLock(tag, allowedNotificationIds, scope)
    }

    override fun releaseLock(lockId: String): Boolean {
        return localDataSource.releaseLock(lockId)
    }

    override fun releaseAllLocks() {
        localDataSource.releaseAllLocks()
    }

    override fun setDisabled(disabled: Boolean) {
        localDataSource.setDisabled(disabled)
    }

    override fun isLocked(): Boolean {
        return localDataSource.isLocked()
    }

    override fun isNotificationAllowed(notificationId: Int): Boolean {
        return localDataSource.isNotificationAllowed(notificationId)
    }

    override fun getState(): AnimationLockerState {
        return localDataSource.getState()
    }

    override fun getConfig(): AnimationLockerConfig {
        return localDataSource.getConfig()
    }

    override fun updateConfig(config: AnimationLockerConfig) {
        localDataSource.updateConfig(config)
    }

    override fun observeState(): StateFlow<AnimationLockerState> {
        return localDataSource.observeState()
    }

    override fun observeIsLocked(): StateFlow<Boolean> {
        return localDataSource.observeIsLocked()
    }
}
