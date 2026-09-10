package org.telegram.messenger.feature.security.privacy.domain.usecase

import org.telegram.messenger.feature.security.privacy.domain.repository.PrivacyRepository

class CheckPasscodeUseCase(private val repository: PrivacyRepository) {
    operator fun invoke(passcode: String): Boolean = repository.checkPasscode(passcode)
}
