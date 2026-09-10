package org.telegram.messenger.feature.security.privacy.domain.usecase

import org.telegram.messenger.feature.security.privacy.domain.model.PrivacyRuleModel
import org.telegram.messenger.feature.security.privacy.domain.model.PrivacyRuleType
import org.telegram.messenger.feature.security.privacy.domain.repository.PrivacyRepository

class GetPrivacyRulesUseCase(private val repository: PrivacyRepository) {
    suspend operator fun invoke(type: PrivacyRuleType): PrivacyRuleModel? = repository.getPrivacyRules(type)
}
