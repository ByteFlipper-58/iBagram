package org.telegram.messenger.feature.fileref.domain.usecase

import org.telegram.messenger.feature.fileref.domain.model.FileRefRequestItem
import org.telegram.messenger.feature.fileref.domain.repository.FileRefRepository

class RequestReferenceRenewalUseCase(
    private val repository: FileRefRepository
) {
    operator fun invoke(item: FileRefRequestItem): Boolean = repository.requestReferenceRenewal(item)
}
