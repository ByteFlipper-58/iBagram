package org.telegram.messenger.feature.animationlocker.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.animationlocker.domain.model.AnimationLockerConfig
import org.telegram.messenger.feature.animationlocker.domain.usecase.AcquireAnimationLockUseCase
import org.telegram.messenger.feature.animationlocker.domain.usecase.GetAnimationLockerConfigUseCase
import org.telegram.messenger.feature.animationlocker.domain.usecase.GetAnimationLockerStateUseCase
import org.telegram.messenger.feature.animationlocker.domain.usecase.ObserveAnimationLockerStateUseCase
import org.telegram.messenger.feature.animationlocker.domain.usecase.ReleaseAllAnimationLocksUseCase
import org.telegram.messenger.feature.animationlocker.domain.usecase.ReleaseAnimationLockUseCase
import org.telegram.messenger.feature.animationlocker.domain.usecase.SetAnimationLockerDisabledUseCase
import org.telegram.messenger.feature.animationlocker.domain.usecase.UpdateAnimationLockerConfigUseCase

class AnimationLockerViewModel(
    private val acquireAnimationLockUseCase: AcquireAnimationLockUseCase,
    private val releaseAnimationLockUseCase: ReleaseAnimationLockUseCase,
    private val releaseAllAnimationLocksUseCase: ReleaseAllAnimationLocksUseCase,
    private val setAnimationLockerDisabledUseCase: SetAnimationLockerDisabledUseCase,
    private val getAnimationLockerStateUseCase: GetAnimationLockerStateUseCase,
    private val getAnimationLockerConfigUseCase: GetAnimationLockerConfigUseCase,
    private val updateAnimationLockerConfigUseCase: UpdateAnimationLockerConfigUseCase,
    private val observeAnimationLockerStateUseCase: ObserveAnimationLockerStateUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {

    private val _uiState = MutableStateFlow(
        AnimationLockerUiState(
            lockerState = getAnimationLockerStateUseCase(),
            config = getAnimationLockerConfigUseCase()
        )
    )
    val uiState: StateFlow<AnimationLockerUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            observeAnimationLockerStateUseCase().collect { state ->
                _uiState.update { it.copy(lockerState = state) }
            }
        }
    }

    fun onEvent(event: AnimationLockerEvent) {
        when (event) {
            is AnimationLockerEvent.AcquireLock -> {
                try {
                    acquireAnimationLockUseCase(
                        tag = event.tag,
                        allowedNotificationIds = event.allowedNotificationIds,
                        scope = event.scope
                    )
                } catch (e: Throwable) {
                    _uiState.update { it.copy(errorMessage = e.message ?: "Failed to acquire animation lock") }
                }
            }
            is AnimationLockerEvent.ReleaseLock -> {
                try {
                    releaseAnimationLockUseCase(event.lockId)
                } catch (e: Throwable) {
                    _uiState.update { it.copy(errorMessage = e.message ?: "Failed to release animation lock") }
                }
            }
            is AnimationLockerEvent.ReleaseAllLocks -> {
                try {
                    releaseAllAnimationLocksUseCase()
                } catch (e: Throwable) {
                    _uiState.update { it.copy(errorMessage = e.message ?: "Failed to release all animation locks") }
                }
            }
            is AnimationLockerEvent.SetDisabled -> {
                try {
                    setAnimationLockerDisabledUseCase(event.disabled)
                } catch (e: Throwable) {
                    _uiState.update { it.copy(errorMessage = e.message ?: "Failed to set disabled state") }
                }
            }
            is AnimationLockerEvent.UpdateConfig -> {
                try {
                    updateAnimationLockerConfigUseCase(event.config)
                    _uiState.update { it.copy(config = event.config) }
                } catch (e: Throwable) {
                    _uiState.update { it.copy(errorMessage = e.message ?: "Failed to update config") }
                }
            }
            is AnimationLockerEvent.DismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    fun onCleared() {
        scope.cancel()
    }
}
