package org.telegram.messenger.feature.businesslinks.domain.usecase

import org.telegram.messenger.feature.businesslinks.domain.repository.BusinessLinksRepository

class CanAddNewBusinessLinkUseCase(
    private val repository: BusinessLinksRepository
) {
    operator fun invoke(): Boolean {
        return repository.canAddNew()
    }
}
