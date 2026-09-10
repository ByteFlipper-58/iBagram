package org.telegram.messenger.feature.security.captcha.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.security.captcha.domain.model.CaptchaRequestModel
import org.telegram.messenger.feature.security.captcha.domain.repository.CaptchaRepository

class ObserveActiveCaptchaRequestsUseCase(
    private val repository: CaptchaRepository
) {
    operator fun invoke(): Flow<List<CaptchaRequestModel>> {
        return repository.observeActiveRequests()
    }
}
