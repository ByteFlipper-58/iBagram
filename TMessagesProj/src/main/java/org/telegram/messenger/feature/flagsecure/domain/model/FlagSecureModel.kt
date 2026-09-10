package org.telegram.messenger.feature.flagsecure.domain.model

/**
 * Причины включения флага защиты окна (FLAG_SECURE).
 */
enum class SecurityReasonType {
    PASSCODE_LOCK,
    SECRET_CHAT,
    PROTECTED_CONTENT,
    SELF_DESTRUCT_MEDIA,
    PAYMENTS,
    BIOMETRIC_PROMPT,
    CUSTOM
}

/**
 * Состояние безопасности отдельного окна приложения.
 */
data class WindowSecurityState(
    val windowId: String,
    val isSecured: Boolean = false,
    val activeReasonsCount: Int = 0,
    val activeReasons: List<SecurityReasonType> = emptyList(),
    val lastUpdatedMs: Long = 0L
)

/**
 * Снимок контекста для вычисления необходимости установки FLAG_SECURE.
 */
data class SecurityRuleSpec(
    val hasPasscode: Boolean = false,
    val allowScreenCapture: Boolean = false,
    val isSecretChat: Boolean = false,
    val isProtectedPeer: Boolean = false,
    val hasSelfDestructMedia: Boolean = false,
    val isPaymentScreen: Boolean = false
)

/**
 * Результат оценки правил безопасности окна.
 */
data class SecurityEvaluationResult(
    val shouldSecure: Boolean,
    val matchedReasons: List<SecurityReasonType>
)

/**
 * Чистый доменный калькулятор правил безопасности окна.
 */
object SecurityRulesEvaluator {

    /**
     * Оценивает контекст экрана и формирует список активных причин для включения FLAG_SECURE.
     */
    fun evaluate(spec: SecurityRuleSpec): SecurityEvaluationResult {
        val reasons = mutableListOf<SecurityReasonType>()

        // 1. Пароль установлен и запрещен захват экрана
        if (spec.hasPasscode && !spec.allowScreenCapture) {
            reasons.add(SecurityReasonType.PASSCODE_LOCK)
        }

        // 2. Секретный чат с E2EE шифрованием
        if (spec.isSecretChat) {
            reasons.add(SecurityReasonType.SECRET_CHAT)
        }

        // 3. Защищенный канал/чат с запретом пересылки и сохранения (No Forwards)
        if (spec.isProtectedPeer) {
            reasons.add(SecurityReasonType.PROTECTED_CONTENT)
        }

        // 4. Одноразовые или самоуничтожающиеся медиафайлы
        if (spec.hasSelfDestructMedia) {
            reasons.add(SecurityReasonType.SELF_DESTRUCT_MEDIA)
        }

        // 5. Экран ввода платежных данных
        if (spec.isPaymentScreen) {
            reasons.add(SecurityReasonType.PAYMENTS)
        }

        return SecurityEvaluationResult(
            shouldSecure = reasons.isNotEmpty(),
            matchedReasons = reasons
        )
    }

    /**
     * Вычисляет итоговое состояние окна при изменении счетчика активных причин.
     */
    fun calculateNewState(
        windowId: String,
        currentReasons: List<SecurityReasonType>,
        reasonToAdd: SecurityReasonType? = null,
        reasonToRemove: SecurityReasonType? = null,
        timestampMs: Long = System.currentTimeMillis()
    ): WindowSecurityState {
        val updated = currentReasons.toMutableList()
        if (reasonToAdd != null) {
            updated.add(reasonToAdd)
        }
        if (reasonToRemove != null) {
            updated.remove(reasonToRemove)
        }

        return WindowSecurityState(
            windowId = windowId,
            isSecured = updated.isNotEmpty(),
            activeReasonsCount = updated.size,
            activeReasons = updated.distinct(),
            lastUpdatedMs = timestampMs
        )
    }
}
