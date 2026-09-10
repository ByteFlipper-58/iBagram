package org.telegram.messenger.feature.system.animationlocker.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.system.animationlocker.domain.model.AnimationLockRecord
import org.telegram.messenger.feature.system.animationlocker.domain.model.AnimationLockerConfig
import org.telegram.messenger.feature.system.animationlocker.domain.model.AnimationLockerState
import org.telegram.messenger.feature.system.animationlocker.domain.model.LockScope

/**
 * Контракт репозитория блокировщика уведомлений во время анимаций.
 */
interface AnimationLockerRepository {

    /**
     * Захватывает блокировку уведомлений на время анимации.
     * @param tag пользовательская метка анимации (например "chat_transition", "emoji_picker")
     * @param allowedNotificationIds набор идентификаторов уведомлений, которым разрешено выполняться
     * @param scope область действия (ACCOUNT, GLOBAL, ALL)
     */
    fun acquireLock(
        tag: String? = null,
        allowedNotificationIds: Set<Int> = emptySet(),
        scope: LockScope = LockScope.ALL
    ): AnimationLockRecord

    /**
     * Освобождает ранее захваченную блокировку.
     */
    fun releaseLock(lockId: String): Boolean

    /**
     * Освобождает все активные блокировки для указанного аккаунта или глобально.
     */
    fun releaseAllLocks()

    /**
     * Включает или отключает блокировщик (при disabled = true блокировки игнорируются).
     */
    fun setDisabled(disabled: Boolean)

    /**
     * Проверяет, активна ли блокировка в данный момент.
     */
    fun isLocked(): Boolean

    /**
     * Проверяет, разрешено ли уведомление при текущих активных блокировках.
     */
    fun isNotificationAllowed(notificationId: Int): Boolean

    /**
     * Возвращает текущий снимок состояния блокировщика.
     */
    fun getState(): AnimationLockerState

    /**
     * Возвращает текущую конфигурацию блокировщика.
     */
    fun getConfig(): AnimationLockerConfig

    /**
     * Обновляет конфигурацию блокировщика.
     */
    fun updateConfig(config: AnimationLockerConfig)

    /**
     * Реактивный поток состояния блокировщика.
     */
    fun observeState(): StateFlow<AnimationLockerState>

    /**
     * Реактивный поток признака активности блокировки.
     */
    fun observeIsLocked(): StateFlow<Boolean>
}
