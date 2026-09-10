package org.telegram.messenger.feature.security.privacy.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.privacy.domain.repository.PrivacyRepository

class SetPasscodeUseCase(private val repository: PrivacyRepository) {
    suspend operator fun invoke(passcode: String, type: Int): Result<Unit> = repository.setPasscode(passcode, type)
}
