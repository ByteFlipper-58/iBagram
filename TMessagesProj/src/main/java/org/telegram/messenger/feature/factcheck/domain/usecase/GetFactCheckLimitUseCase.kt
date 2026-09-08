package org.telegram.messenger.feature.factcheck.domain.usecase

import org.telegram.messenger.feature.factcheck.domain.repository.FactCheckRepository

class GetFactCheckLimitUseCase(
    private val repository: FactCheckRepository
) {
    suspend operator fun invoke(): Int {
        return repository.getFactCheckLimit()
    }
}
