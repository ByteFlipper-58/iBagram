package org.telegram.messenger.feature.system.animationlocker.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.feature.system.animationlocker.domain.model.AnimationLockerConfig

/**
 * Remote data source extension point for animation locker configuration synchronization.
 */
class AnimationLockerRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    suspend fun fetchAnimationLockerConfig(): Result<AnimationLockerConfig> {
        return runCatching {
            AnimationLockerConfig()
        }
    }
}
