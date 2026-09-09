package org.telegram.messenger.feature.businesslinks.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.businesslinks.domain.repository.BusinessLinksRepository

class DeleteBusinessLinkUseCase(
    private val repository: BusinessLinksRepository
) {
    suspend operator fun invoke(slug: String): Result<Unit> {
        return repository.deleteLink(slug)
    }
}
