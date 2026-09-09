package org.telegram.messenger.feature.businesslinks.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.businesslinks.domain.model.BusinessLinkModel
import org.telegram.messenger.feature.businesslinks.domain.repository.BusinessLinksRepository

class LoadBusinessLinksUseCase(
    private val repository: BusinessLinksRepository
) {
    suspend operator fun invoke(forceReload: Boolean = false): Result<List<BusinessLinkModel>> {
        return repository.loadBusinessLinks(forceReload)
    }
}
