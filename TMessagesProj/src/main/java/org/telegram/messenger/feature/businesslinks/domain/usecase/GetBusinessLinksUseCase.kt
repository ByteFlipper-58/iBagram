package org.telegram.messenger.feature.businesslinks.domain.usecase

import org.telegram.messenger.feature.businesslinks.domain.model.BusinessLinkModel
import org.telegram.messenger.feature.businesslinks.domain.repository.BusinessLinksRepository

class GetBusinessLinksUseCase(
    private val repository: BusinessLinksRepository
) {
    suspend operator fun invoke(): List<BusinessLinkModel> {
        return repository.getBusinessLinks()
    }
}
