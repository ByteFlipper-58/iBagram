package org.telegram.messenger.feature.businesslinks.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.businesslinks.domain.model.BusinessLinkModel
import org.telegram.messenger.feature.businesslinks.domain.repository.BusinessLinksRepository

class EditBusinessLinkUseCase(
    private val repository: BusinessLinksRepository
) {
    suspend operator fun invoke(slug: String, title: String?, message: String): Result<BusinessLinkModel> {
        return repository.editLink(slug, title, message)
    }
}
