package org.telegram.messenger.feature.security.flagsecure.data.mapper

import org.telegram.messenger.feature.security.flagsecure.domain.model.SecurityReasonType
import org.telegram.messenger.feature.security.flagsecure.domain.model.WindowSecurityState

object FlagSecureMapper {

    /**
     * Преобразует тип причины в понятное человеку описание.
     */
    fun mapReasonTitle(reason: SecurityReasonType): String {
        return when (reason) {
            SecurityReasonType.PASSCODE_LOCK -> "Код-пароль приложения (запрет скриншотов)"
            SecurityReasonType.SECRET_CHAT -> "Секретный чат (End-to-End Encryption)"
            SecurityReasonType.PROTECTED_CONTENT -> "Защищенный контент (запрет пересылки)"
            SecurityReasonType.SELF_DESTRUCT_MEDIA -> "Самоуничтожающиеся медиа"
            SecurityReasonType.PAYMENTS -> "Ввод платежных реквизитов"
            SecurityReasonType.BIOMETRIC_PROMPT -> "Биометрическая аутентификация"
            SecurityReasonType.CUSTOM -> "Пользовательское ограничение"
        }
    }

    /**
     * Форматирует текстовую сводку состояния безопасности окна для журнала или отладки.
     */
    fun formatWindowStateSummary(state: WindowSecurityState): String {
        return buildString {
            append("Window [${state.windowId}]: ")
            if (state.isSecured) {
                append("SECURED (Reasons: ${state.activeReasonsCount}) -> ")
                append(state.activeReasons.joinToString { it.name })
            } else {
                append("UNSECURED (0 active reasons)")
            }
        }
    }
}
