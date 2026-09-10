package org.telegram.messenger.feature.messaging.mentions.domain.usecase

/**
 * Проверяет корректность юзернейма пользователя или бота (символы a-z, A-Z, 0-9, _).
 */
class ValidateUsernameUseCase {

    operator fun invoke(username: String?): Boolean {
        if (username.isNullOrEmpty()) return false
        for (i in username.indices) {
            val ch = username[i]
            val isValidChar = (ch in 'a'..'z') || (ch in 'A'..'Z') || (ch in '0'..'9') || (ch == '_')
            if (!isValidChar) return false
        }
        return true
    }
}
