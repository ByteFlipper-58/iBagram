package org.telegram.messenger.feature.business.businesslinks.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.business.businesslinks.domain.model.BusinessLinkModel
import org.telegram.messenger.feature.business.businesslinks.domain.repository.BusinessLinksRepository

class ObserveBusinessLinksUseCase(
    private val repository: BusinessLinksRepository
) {
    operator fun invoke(): Flow<List<BusinessLinkModel>> {
        return repository.observeBusinessLinks()
    }
}
