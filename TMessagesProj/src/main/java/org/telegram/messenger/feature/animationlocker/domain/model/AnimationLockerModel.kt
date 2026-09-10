package org.telegram.messenger.feature.animationlocker.domain.model

/**
 * Область действия блокировки уведомлений во время анимации.
 */
enum class LockScope {
    ACCOUNT,
    GLOBAL,
    ALL
}

/**
 * Запись активной блокировки анимации.
 */
data class AnimationLockRecord(
    val lockId: String,
    val tag: String? = null,
    val allowedNotificationIds: Set<Int> = emptySet(),
    val account: Int = 0,
    val scope: LockScope = LockScope.ALL,
    val lockedAtMs: Long = 0L,
    val accountHandle: Int = -1,
    val globalHandle: Int = -1
)

/**
 * Текущее состояние блокировщика уведомлений анимаций.
 */
data class AnimationLockerState(
    val isLocked: Boolean = false,
    val activeLocksCount: Int = 0,
    val activeLocks: List<AnimationLockRecord> = emptyList(),
    val isDisabled: Boolean = false,
    val currentAccount: Int = 0,
    val totalAcquiredLocks: Long = 0L,
    val totalReleasedLocks: Long = 0L
)

/**
 * Конфигурация блокировщика.
 */
data class AnimationLockerConfig(
    val defaultScope: LockScope = LockScope.ALL,
    val safetyTimeoutMs: Long = 10_000L
)

/**
 * Чистая доменная логика фильтрации и арбитража уведомлений во время анимаций.
 */
object AnimationLockEvaluator {

    /**
     * Проверяет, разрешено ли уведомление при текущем наборе активных блокировок.
     * Если список блокировок пуст, разрешены все уведомления.
     * Если хотя бы одна активная блокировка не содержит id в allowedNotificationIds, уведомление откладывается.
     */
    fun isNotificationAllowed(
        notificationId: Int,
        activeLocks: List<AnimationLockRecord>
    ): Boolean {
        if (activeLocks.isEmpty()) {
            return true
        }
        for (lock in activeLocks) {
            if (!lock.allowedNotificationIds.contains(notificationId)) {
                return false
            }
        }
        return true
    }

    /**
     * Проверяет, просрочена ли блокировка по таймауту безопасности.
     */
    fun isLockExpired(
        lock: AnimationLockRecord,
        currentTimeMs: Long,
        timeoutMs: Long
    ): Boolean {
        return (currentTimeMs - lock.lockedAtMs) > timeoutMs
    }
}
