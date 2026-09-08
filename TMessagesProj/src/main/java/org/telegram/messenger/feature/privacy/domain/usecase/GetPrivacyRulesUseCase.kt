package org.telegram.messenger.feature.privacy.domain.usecase

import org.telegram.messenger.feature.privacy.domain.model.PrivacyRuleModel
import org.telegram.messenger.feature.privacy.domain.model.PrivacyRuleType
import org.telegram.messenger.feature.privacy.domain.repository.PrivacyRepository

class GetPrivacyRulesUseCase(private val repository: PrivacyRepository) {
    suspend operator fun invoke(type: PrivacyRuleType): PrivacyRuleModel? = repository.getPrivacyRules(type)
}
