package org.telegram.messenger.feature.privacy.domain.usecase

import org.telegram.messenger.feature.privacy.domain.model.PasscodeSettingsModel
import org.telegram.messenger.feature.privacy.domain.repository.PrivacyRepository

class GetPasscodeSettingsUseCase(private val repository: PrivacyRepository) {
    operator fun invoke(): PasscodeSettingsModel = repository.getPasscodeSettings()
}
