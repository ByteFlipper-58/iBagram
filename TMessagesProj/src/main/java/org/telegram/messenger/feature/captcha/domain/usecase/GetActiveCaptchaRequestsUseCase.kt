package org.telegram.messenger.feature.captcha.domain.usecase

import org.telegram.messenger.feature.captcha.domain.model.CaptchaRequestModel
import org.telegram.messenger.feature.captcha.domain.repository.CaptchaRepository

class GetActiveCaptchaRequestsUseCase(
    private val repository: CaptchaRepository
) {
    operator fun invoke(): List<CaptchaRequestModel> {
        return repository.getActiveRequests()
    }
}
