package org.telegram.messenger.feature.security.privacy.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.security.privacy.domain.model.PrivacyRuleModel
import org.telegram.messenger.feature.security.privacy.domain.model.PrivacyRuleType
import org.telegram.messenger.feature.security.privacy.domain.repository.PrivacyRepository

class ObservePrivacyRulesUseCase(private val repository: PrivacyRepository) {
    operator fun invoke(type: PrivacyRuleType): Flow<PrivacyRuleModel?> = repository.observePrivacyRules(type)
}
