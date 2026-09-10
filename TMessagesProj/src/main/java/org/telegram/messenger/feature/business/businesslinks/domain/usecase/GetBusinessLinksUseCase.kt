package org.telegram.messenger.feature.business.businesslinks.domain.usecase

import org.telegram.messenger.feature.business.businesslinks.domain.model.BusinessLinkModel
import org.telegram.messenger.feature.business.businesslinks.domain.repository.BusinessLinksRepository

class GetBusinessLinksUseCase(
    private val repository: BusinessLinksRepository
) {
    suspend operator fun invoke(): List<BusinessLinkModel> {
        return repository.getBusinessLinks()
    }
}
