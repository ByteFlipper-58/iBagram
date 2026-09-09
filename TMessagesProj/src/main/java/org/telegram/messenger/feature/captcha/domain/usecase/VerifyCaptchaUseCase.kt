package org.telegram.messenger.feature.captcha.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.captcha.domain.model.CaptchaResult
import org.telegram.messenger.feature.captcha.domain.repository.CaptchaRepository

class VerifyCaptchaUseCase(
    private val repository: CaptchaRepository
) {
    suspend operator fun invoke(
        currentAccount: Int,
        requestToken: Int,
        action: String,
        keyId: String
    ): Result<CaptchaResult> {
        return repository.verifyCaptcha(
            currentAccount = currentAccount,
            requestToken = requestToken,
            action = action,
            keyId = keyId
        )
    }
}
