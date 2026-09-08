package org.telegram.messenger.feature.privacy.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.privacy.domain.model.PrivacyRuleModel
import org.telegram.messenger.feature.privacy.domain.model.PrivacyRuleType
import org.telegram.messenger.feature.privacy.domain.repository.PrivacyRepository

class ObservePrivacyRulesUseCase(private val repository: PrivacyRepository) {
    operator fun invoke(type: PrivacyRuleType): Flow<PrivacyRuleModel?> = repository.observePrivacyRules(type)
}
