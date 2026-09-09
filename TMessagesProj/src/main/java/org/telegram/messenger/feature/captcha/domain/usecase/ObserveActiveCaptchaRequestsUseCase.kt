package org.telegram.messenger.feature.captcha.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.captcha.domain.model.CaptchaRequestModel
import org.telegram.messenger.feature.captcha.domain.repository.CaptchaRepository

class ObserveActiveCaptchaRequestsUseCase(
    private val repository: CaptchaRepository
) {
    operator fun invoke(): Flow<List<CaptchaRequestModel>> {
        return repository.observeActiveRequests()
    }
}
