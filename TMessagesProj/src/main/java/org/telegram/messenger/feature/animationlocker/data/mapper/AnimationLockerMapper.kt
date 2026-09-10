package org.telegram.messenger.feature.animationlocker.data.mapper

import org.telegram.messenger.feature.animationlocker.domain.model.AnimationLockRecord
import org.telegram.messenger.feature.animationlocker.domain.model.AnimationLockerState

object AnimationLockerMapper {

    /**
     * Форматирует сводку состояния блокировщика для отладки и системного журнала.
     */
    fun formatLockerSummary(state: AnimationLockerState): String {
        return buildString {
            append("AnimationLocker [account=${state.currentAccount}]: ")
            if (state.isDisabled) {
                append("DISABLED ")
            }
            if (state.isLocked) {
                append("LOCKED (Active locks: ${state.activeLocksCount})")
                for (lock in state.activeLocks) {
                    append("\n  - Lock[${lock.lockId}] tag=${lock.tag ?: "none"} scope=${lock.scope} allowed=${lock.allowedNotificationIds.size}")
                }
            } else {
                append("IDLE (0 active locks)")
            }
        }
    }

    /**
     * Создает строковое представление отдельной блокировки.
     */
    fun formatLockRecord(record: AnimationLockRecord): String {
        val tagStr = record.tag?.let { " tag=\"$it\"" } ?: ""
        val allowedStr = if (record.allowedNotificationIds.isNotEmpty()) {
            " allowed=[${record.allowedNotificationIds.joinToString()}]"
        } else {
            " allowed=none"
        }
        return "AnimationLock(id=${record.lockId}$tagStr scope=${record.scope}$allowedStr)"
    }
}
