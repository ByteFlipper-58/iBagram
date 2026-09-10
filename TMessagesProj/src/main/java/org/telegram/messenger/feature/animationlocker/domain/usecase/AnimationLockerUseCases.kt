package org.telegram.messenger.feature.animationlocker.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.animationlocker.domain.model.AnimationLockRecord
import org.telegram.messenger.feature.animationlocker.domain.model.AnimationLockerConfig
import org.telegram.messenger.feature.animationlocker.domain.model.AnimationLockerState
import org.telegram.messenger.feature.animationlocker.domain.model.LockScope
import org.telegram.messenger.feature.animationlocker.domain.repository.AnimationLockerRepository

class AcquireAnimationLockUseCase(
    private val repository: AnimationLockerRepository
) {
    operator fun invoke(
        tag: String? = null,
        allowedNotificationIds: Set<Int> = emptySet(),
        scope: LockScope = LockScope.ALL
    ): AnimationLockRecord {
        return repository.acquireLock(tag, allowedNotificationIds, scope)
    }
}

class ReleaseAnimationLockUseCase(
    private val repository: AnimationLockerRepository
) {
    operator fun invoke(lockId: String): Boolean {
        return repository.releaseLock(lockId)
    }
}

class ReleaseAllAnimationLocksUseCase(
    private val repository: AnimationLockerRepository
) {
    operator fun invoke() {
        repository.releaseAllLocks()
    }
}

class SetAnimationLockerDisabledUseCase(
    private val repository: AnimationLockerRepository
) {
    operator fun invoke(disabled: Boolean) {
        repository.setDisabled(disabled)
    }
}

class IsAnimationLockedUseCase(
    private val repository: AnimationLockerRepository
) {
    operator fun invoke(): Boolean {
        return repository.isLocked()
    }
}

class IsNotificationAllowedUseCase(
    private val repository: AnimationLockerRepository
) {
    operator fun invoke(notificationId: Int): Boolean {
        return repository.isNotificationAllowed(notificationId)
    }
}

class GetAnimationLockerStateUseCase(
    private val repository: AnimationLockerRepository
) {
    operator fun invoke(): AnimationLockerState {
        return repository.getState()
    }
}

class GetAnimationLockerConfigUseCase(
    private val repository: AnimationLockerRepository
) {
    operator fun invoke(): AnimationLockerConfig {
        return repository.getConfig()
    }
}

class UpdateAnimationLockerConfigUseCase(
    private val repository: AnimationLockerRepository
) {
    operator fun invoke(config: AnimationLockerConfig) {
        repository.updateConfig(config)
    }
}

class ObserveAnimationLockerStateUseCase(
    private val repository: AnimationLockerRepository
) {
    operator fun invoke(): StateFlow<AnimationLockerState> {
        return repository.observeState()
    }
}

class ObserveIsAnimationLockedUseCase(
    private val repository: AnimationLockerRepository
) {
    operator fun invoke(): StateFlow<Boolean> {
        return repository.observeIsLocked()
    }
}
