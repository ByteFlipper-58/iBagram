package org.telegram.messenger.feature.security.captcha.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.captcha.domain.repository.CaptchaRepository

class CancelCaptchaUseCase(
    private val repository: CaptchaRepository
) {
    operator fun invoke(
        currentAccount: Int,
        action: String,
        keyId: String
    ): Result<Unit> {
        return repository.cancelCaptcha(
            currentAccount = currentAccount,
            action = action,
            keyId = keyId
        )
    }
}
