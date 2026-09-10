package org.telegram.messenger.feature.animationlocker.presentation

import org.telegram.messenger.feature.animationlocker.domain.model.AnimationLockRecord
import org.telegram.messenger.feature.animationlocker.domain.model.AnimationLockerConfig
import org.telegram.messenger.feature.animationlocker.domain.model.AnimationLockerState

data class AnimationLockerUiState(
    val lockerState: AnimationLockerState = AnimationLockerState(),
    val config: AnimationLockerConfig = AnimationLockerConfig(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val isLocked: Boolean
        get() = lockerState.isLocked

    val activeLocksCount: Int
        get() = lockerState.activeLocksCount

    val activeLocks: List<AnimationLockRecord>
        get() = lockerState.activeLocks

    val isDisabled: Boolean
        get() = lockerState.isDisabled
}
