package org.telegram.messenger.feature.business.businesslinks.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.businesslinks.domain.model.BusinessLinkInputModel
import org.telegram.messenger.feature.business.businesslinks.domain.model.BusinessLinkModel
import org.telegram.messenger.feature.business.businesslinks.domain.repository.BusinessLinksRepository

class CreateBusinessLinkUseCase(
    private val repository: BusinessLinksRepository
) {
    suspend operator fun invoke(input: BusinessLinkInputModel? = null): Result<BusinessLinkModel> {
        return repository.createLink(input)
    }
}
