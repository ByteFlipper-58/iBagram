package org.telegram.messenger.feature.translate.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.translate.domain.repository.TranslationRepository

class SetChatTranslateEnabledUseCase(
    private val repository: TranslationRepository
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> {
        return repository.setChatTranslateEnabled(enabled)
    }
}
