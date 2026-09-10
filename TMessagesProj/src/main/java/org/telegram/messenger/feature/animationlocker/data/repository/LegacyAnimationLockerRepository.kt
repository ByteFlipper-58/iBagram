package org.telegram.messenger.feature.animationlocker.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.feature.animationlocker.domain.model.AnimationLockEvaluator
import org.telegram.messenger.feature.animationlocker.domain.model.AnimationLockRecord
import org.telegram.messenger.feature.animationlocker.domain.model.AnimationLockerConfig
import org.telegram.messenger.feature.animationlocker.domain.model.AnimationLockerState
import org.telegram.messenger.feature.animationlocker.domain.model.LockScope
import org.telegram.messenger.feature.animationlocker.domain.repository.AnimationLockerRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class LegacyAnimationLockerRepository(
    private val account: Int = 0,
    private val onSetAnimationInProgress: ((account: Int, isGlobal: Boolean, handle: Int, allowed: IntArray?) -> Int)? = null,
    private val onAnimationFinish: ((account: Int, isGlobal: Boolean, handle: Int) -> Unit)? = null
) : AnimationLockerRepository {

    private val locks = ConcurrentHashMap<String, AnimationLockRecord>()
    private var config = AnimationLockerConfig()
    private var isDisabled = false

    private val totalAcquired = AtomicLong(0)
    private val totalReleased = AtomicLong(0)
    private val handleGenerator = AtomicInteger(1)

    private val _stateFlow = MutableStateFlow(
        AnimationLockerState(currentAccount = account)
    )
    override fun observeState(): StateFlow<AnimationLockerState> = _stateFlow.asStateFlow()

    private val _isLockedFlow = MutableStateFlow(false)
    override fun observeIsLocked(): StateFlow<Boolean> = _isLockedFlow.asStateFlow()

    override fun acquireLock(
        tag: String?,
        allowedNotificationIds: Set<Int>,
        scope: LockScope
    ): AnimationLockRecord {
        val lockId = UUID.randomUUID().toString()
        if (isDisabled) {
            val record = AnimationLockRecord(
                lockId = lockId,
                tag = tag,
                allowedNotificationIds = allowedNotificationIds,
                account = account,
                scope = scope,
                lockedAtMs = System.currentTimeMillis()
            )
            return record
        }

        val allowedArray = if (allowedNotificationIds.isNotEmpty()) {
            allowedNotificationIds.toIntArray()
        } else {
            null
        }

        var accountHandle = -1
        var globalHandle = -1

        if (scope == LockScope.ACCOUNT || scope == LockScope.ALL) {
            accountHandle = callSetAnimationInProgress(account, isGlobal = false, handle = -1, allowed = allowedArray)
        }
        if (scope == LockScope.GLOBAL || scope == LockScope.ALL) {
            globalHandle = callSetAnimationInProgress(account, isGlobal = true, handle = -1, allowed = allowedArray)
        }

        val record = AnimationLockRecord(
            lockId = lockId,
            tag = tag,
            allowedNotificationIds = allowedNotificationIds,
            account = account,
            scope = scope,
            lockedAtMs = System.currentTimeMillis(),
            accountHandle = accountHandle,
            globalHandle = globalHandle
        )

        locks[lockId] = record
        totalAcquired.incrementAndGet()
        updateState()

        return record
    }

    override fun releaseLock(lockId: String): Boolean {
        val record = locks.remove(lockId) ?: return false

        if (record.accountHandle != -1) {
            callAnimationFinish(account, isGlobal = false, handle = record.accountHandle)
        }
        if (record.globalHandle != -1) {
            callAnimationFinish(account, isGlobal = true, handle = record.globalHandle)
        }

        totalReleased.incrementAndGet()
        updateState()
        return true
    }

    override fun releaseAllLocks() {
        for (lockId in locks.keys.toList()) {
            releaseLock(lockId)
        }
    }

    override fun setDisabled(disabled: Boolean) {
        if (this.isDisabled == disabled) return
        this.isDisabled = disabled
        if (disabled) {
            releaseAllLocks()
        }
        updateState()
    }

    override fun isLocked(): Boolean {
        return !isDisabled && locks.isNotEmpty()
    }

    override fun isNotificationAllowed(notificationId: Int): Boolean {
        if (isDisabled) return true
        return AnimationLockEvaluator.isNotificationAllowed(notificationId, locks.values.toList())
    }

    override fun getState(): AnimationLockerState {
        return _stateFlow.value
    }

    override fun getConfig(): AnimationLockerConfig = config

    override fun updateConfig(config: AnimationLockerConfig) {
        this.config = config
    }

    private fun updateState() {
        val activeList = locks.values.toList()
        val locked = !isDisabled && activeList.isNotEmpty()
        val newState = AnimationLockerState(
            isLocked = locked,
            activeLocksCount = activeList.size,
            activeLocks = activeList,
            isDisabled = isDisabled,
            currentAccount = account,
            totalAcquiredLocks = totalAcquired.get(),
            totalReleasedLocks = totalReleased.get()
        )
        _stateFlow.value = newState
        _isLockedFlow.value = locked
    }

    private fun callSetAnimationInProgress(
        account: Int,
        isGlobal: Boolean,
        handle: Int,
        allowed: IntArray?
    ): Int {
        if (onSetAnimationInProgress != null) {
            return onSetAnimationInProgress.invoke(account, isGlobal, handle, allowed)
        }
        return try {
            if (isGlobal) {
                NotificationCenter.getGlobalInstance().setAnimationInProgress(handle, allowed)
            } else {
                NotificationCenter.getInstance(account).setAnimationInProgress(handle, allowed)
            }
        } catch (_: Throwable) {
            handleGenerator.incrementAndGet()
        }
    }

    private fun callAnimationFinish(account: Int, isGlobal: Boolean, handle: Int) {
        if (onAnimationFinish != null) {
            onAnimationFinish.invoke(account, isGlobal, handle)
            return
        }
        try {
            if (isGlobal) {
                NotificationCenter.getGlobalInstance().onAnimationFinish(handle)
            } else {
                NotificationCenter.getInstance(account).onAnimationFinish(handle)
            }
        } catch (_: Throwable) {
            // Игнорируем в тестах
        }
    }
}
