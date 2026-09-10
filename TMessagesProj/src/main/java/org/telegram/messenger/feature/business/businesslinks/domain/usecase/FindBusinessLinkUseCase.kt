package org.telegram.messenger.feature.business.businesslinks.domain.usecase

import org.telegram.messenger.feature.business.businesslinks.domain.model.BusinessLinkModel
import org.telegram.messenger.feature.business.businesslinks.domain.repository.BusinessLinksRepository

class FindBusinessLinkUseCase(
    private val repository: BusinessLinksRepository
) {
    operator fun invoke(slug: String): BusinessLinkModel? {
        return repository.findLink(slug)
    }
}
