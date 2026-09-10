package org.telegram.messenger.feature.security.captcha.domain.usecase

import org.telegram.messenger.feature.security.captcha.domain.model.CaptchaRequestModel
import org.telegram.messenger.feature.security.captcha.domain.repository.CaptchaRepository

class GetActiveCaptchaRequestsUseCase(
    private val repository: CaptchaRepository
) {
    operator fun invoke(): List<CaptchaRequestModel> {
        return repository.getActiveRequests()
    }
}
