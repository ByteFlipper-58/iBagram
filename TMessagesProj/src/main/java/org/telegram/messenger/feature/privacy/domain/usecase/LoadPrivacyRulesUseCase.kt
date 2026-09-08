package org.telegram.messenger.feature.privacy.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.privacy.domain.repository.PrivacyRepository

class LoadPrivacyRulesUseCase(private val repository: PrivacyRepository) {
    suspend operator fun invoke(): Result<Unit> = repository.loadPrivacyRules()
}
