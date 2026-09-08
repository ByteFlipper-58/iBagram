package org.telegram.messenger.feature.privacy.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.privacy.domain.model.TwoStepVerificationModel
import org.telegram.messenger.feature.privacy.domain.repository.PrivacyRepository

class ObserveTwoStepVerificationUseCase(private val repository: PrivacyRepository) {
    operator fun invoke(): Flow<TwoStepVerificationModel> = repository.observeTwoStepVerification()
}
