package org.telegram.messenger.feature.security.captcha.domain.model

sealed class CaptchaAction {
    object Login : CaptchaAction()
    object SignUp : CaptchaAction()
    data class Custom(val actionName: String) : CaptchaAction()

    val rawAction: String
        get() = when (this) {
            is Login -> "login"
            is SignUp -> "signup"
            is Custom -> actionName
        }

    companion object {
        fun fromString(action: String): CaptchaAction {
            return when (action.lowercase()) {
                "login" -> Login
                "signup" -> SignUp
                else -> Custom(action)
            }
        }
    }
}
