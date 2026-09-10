package org.telegram.messenger.feature.system.animationlocker.presentation

import org.telegram.messenger.feature.system.animationlocker.domain.model.AnimationLockerConfig
import org.telegram.messenger.feature.system.animationlocker.domain.model.LockScope

sealed interface AnimationLockerEvent {
    data class AcquireLock(
        val tag: String? = null,
        val allowedNotificationIds: Set<Int> = emptySet(),
        val scope: LockScope = LockScope.ALL
    ) : AnimationLockerEvent

    data class ReleaseLock(val lockId: String) : AnimationLockerEvent
    data object ReleaseAllLocks : AnimationLockerEvent
    data class SetDisabled(val disabled: Boolean) : AnimationLockerEvent
    data class UpdateConfig(val config: AnimationLockerConfig) : AnimationLockerEvent
    data object DismissError : AnimationLockerEvent
}
