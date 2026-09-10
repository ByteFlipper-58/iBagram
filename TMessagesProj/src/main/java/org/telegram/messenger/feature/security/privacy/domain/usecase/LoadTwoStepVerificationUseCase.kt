package org.telegram.messenger.feature.security.privacy.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.privacy.domain.model.TwoStepVerificationModel
import org.telegram.messenger.feature.security.privacy.domain.repository.PrivacyRepository

class LoadTwoStepVerificationUseCase(private val repository: PrivacyRepository) {
    suspend operator fun invoke(): Result<TwoStepVerificationModel> = repository.loadTwoStepVerification()
}
