package org.telegram.messenger.feature.privacy.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.privacy.domain.model.PrivacyRuleModel
import org.telegram.messenger.feature.privacy.domain.model.PrivacyRuleType
import org.telegram.messenger.feature.privacy.domain.repository.PrivacyRepository

class SetPrivacyRuleUseCase(private val repository: PrivacyRepository) {
    suspend operator fun invoke(type: PrivacyRuleType, rule: PrivacyRuleModel): Result<Unit> =
        repository.setPrivacyRules(type, rule)
}
