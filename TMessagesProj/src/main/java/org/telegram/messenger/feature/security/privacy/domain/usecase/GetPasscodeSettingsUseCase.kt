package org.telegram.messenger.feature.security.privacy.domain.usecase

import org.telegram.messenger.feature.security.privacy.domain.model.PasscodeSettingsModel
import org.telegram.messenger.feature.security.privacy.domain.repository.PrivacyRepository

class GetPasscodeSettingsUseCase(private val repository: PrivacyRepository) {
    operator fun invoke(): PasscodeSettingsModel = repository.getPasscodeSettings()
}
