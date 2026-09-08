package org.telegram.messenger.feature.privacy.presentation

import org.telegram.messenger.feature.privacy.domain.model.PasscodeSettingsModel
import org.telegram.messenger.feature.privacy.domain.model.PrivacyRuleModel
import org.telegram.messenger.feature.privacy.domain.model.PrivacyRuleType
import org.telegram.messenger.feature.privacy.domain.model.TwoStepVerificationModel

data class PrivacyUiState(
    val privacyRules: Map<PrivacyRuleType, PrivacyRuleModel> = emptyMap(),
    val blockedPeers: List<Long> = emptyList(),
    val blockedCount: Int = 0,
    val passcodeSettings: PasscodeSettingsModel = PasscodeSettingsModel(),
    val twoStepVerification: TwoStepVerificationModel = TwoStepVerificationModel(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
