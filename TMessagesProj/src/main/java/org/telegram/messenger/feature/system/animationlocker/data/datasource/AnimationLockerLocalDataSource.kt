package org.telegram.messenger.feature.system.animationlocker.data.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.feature.system.animationlocker.domain.model.AnimationLockEvaluator
import org.telegram.messenger.feature.system.animationlocker.domain.model.AnimationLockRecord
import org.telegram.messenger.feature.system.animationlocker.domain.model.AnimationLockerConfig
import org.telegram.messenger.feature.system.animationlocker.domain.model.AnimationLockerState
import org.telegram.messenger.feature.system.animationlocker.domain.model.LockScope
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Local data source for animation lock arbitration and notification suppression during UI transitions.
 */
class AnimationLockerLocalDataSource(
    private val account: Int = 0,
    private val onSetAnimationInProgress: ((account: Int, isGlobal: Boolean, handle: Int, allowed: IntArray?) -> Int)? = null,
    private val onAnimationFinish: ((account: Int, isGlobal: Boolean, handle: Int) -> Unit)? = null
) {
    private val locks = ConcurrentHashMap<String, AnimationLockRecord>()
    private var config = AnimationLockerConfig()
    private var isDisabled = false
    private var isTestMode = false

    private val totalAcquired = AtomicLong(0)
    private val totalReleased = AtomicLong(0)
    private val handleGenerator = AtomicInteger(1)

    private val _stateFlow = MutableStateFlow(
        AnimationLockerState(currentAccount = account)
    )
    val stateFlow: StateFlow<AnimationLockerState> = _stateFlow.asStateFlow()

    private val _isLockedFlow = MutableStateFlow(false)
    val isLockedFlow: StateFlow<Boolean> = _isLockedFlow.asStateFlow()

    fun setTestMode(isTest: Boolean) {
        this.isTestMode = isTest
    }

    fun observeState(): StateFlow<AnimationLockerState> = stateFlow

    fun observeIsLocked(): StateFlow<Boolean> = isLockedFlow

    fun acquireLock(
        tag: String? = null,
        allowedNotificationIds: Set<Int> = emptySet(),
        scope: LockScope = LockScope.ALL
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

        if (!isTestMode) {
            if (scope == LockScope.ACCOUNT || scope == LockScope.ALL) {
                accountHandle = callSetAnimationInProgress(account, isGlobal = false, handle = -1, allowed = allowedArray)
            }
            if (scope == LockScope.GLOBAL || scope == LockScope.ALL) {
                globalHandle = callSetAnimationInProgress(account, isGlobal = true, handle = -1, allowed = allowedArray)
            }
        } else {
            accountHandle = handleGenerator.getAndIncrement()
            globalHandle = handleGenerator.getAndIncrement()
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

    fun releaseLock(lockId: String): Boolean {
        val record = locks.remove(lockId) ?: return false

        if (!isTestMode) {
            if (record.accountHandle != -1) {
                callAnimationFinish(account, isGlobal = false, handle = record.accountHandle)
            }
            if (record.globalHandle != -1) {
                callAnimationFinish(account, isGlobal = true, handle = record.globalHandle)
            }
        }

        totalReleased.incrementAndGet()
        updateState()
        return true
    }

    fun releaseAllLocks() {
        for (lockId in locks.keys.toList()) {
            releaseLock(lockId)
        }
    }

    fun setDisabled(disabled: Boolean) {
        if (this.isDisabled == disabled) return
        this.isDisabled = disabled
        if (disabled) {
            releaseAllLocks()
        }
        updateState()
    }

    fun isLocked(): Boolean {
        return !isDisabled && locks.isNotEmpty()
    }

    fun isNotificationAllowed(notificationId: Int): Boolean {
        if (isDisabled || locks.isEmpty()) {
            return true
        }
        return AnimationLockEvaluator.isNotificationAllowed(notificationId, locks.values.toList())
    }

    fun getState(): AnimationLockerState = _stateFlow.value

    fun getConfig(): AnimationLockerConfig = config

    fun updateConfig(newConfig: AnimationLockerConfig) {
        this.config = newConfig
    }

    fun getActiveLocks(): List<AnimationLockRecord> = locks.values.toList()

    fun pruneExpiredLocks(currentTimeMs: Long): Int {
        var pruned = 0
        val timeout = config.safetyTimeoutMs
        for ((id, record) in locks) {
            if (AnimationLockEvaluator.isLockExpired(record, currentTimeMs, timeout)) {
                if (releaseLock(id)) {
                    pruned++
                }
            }
        }
        return pruned
    }

    private fun updateState() {
        val activeList = locks.values.toList()
        val locked = !isDisabled && activeList.isNotEmpty()
        _isLockedFlow.value = locked
        _stateFlow.update {
            it.copy(
                isLocked = locked,
                activeLocksCount = activeList.size,
                activeLocks = activeList,
                isDisabled = isDisabled,
                totalAcquiredLocks = totalAcquired.get(),
                totalReleasedLocks = totalReleased.get()
            )
        }
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
            handleGenerator.getAndIncrement()
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
            // Ignored in headless tests
        }
    }
}
