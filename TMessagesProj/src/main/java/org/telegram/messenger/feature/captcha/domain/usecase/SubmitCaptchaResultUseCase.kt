package org.telegram.messenger.feature.captcha.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.captcha.domain.repository.CaptchaRepository

class SubmitCaptchaResultUseCase(
    private val repository: CaptchaRepository
) {
    suspend operator fun invoke(
        currentAccount: Int,
        requestTokens: List<Int>,
        token: String
    ): Result<Unit> {
        return repository.submitCaptchaResult(
            currentAccount = currentAccount,
            requestTokens = requestTokens,
            token = token
        )
    }
}
