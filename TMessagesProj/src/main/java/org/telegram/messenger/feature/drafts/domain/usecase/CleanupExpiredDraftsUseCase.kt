package org.telegram.messenger.feature.drafts.domain.usecase

import org.telegram.messenger.feature.drafts.domain.repository.DraftsRepository

class CleanupExpiredDraftsUseCase(
    private val repository: DraftsRepository
) {
    suspend operator fun invoke(
        now: Long = System.currentTimeMillis(),
        expirationPeriodMs: Long = 7L * 24 * 3600 * 1000L
    ): List<Long> {
        return repository.cleanupExpiredDrafts(now, expirationPeriodMs)
    }
}
