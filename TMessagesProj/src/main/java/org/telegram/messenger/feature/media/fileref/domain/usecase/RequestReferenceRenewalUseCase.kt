package org.telegram.messenger.feature.media.fileref.domain.usecase

import org.telegram.messenger.feature.media.fileref.domain.model.FileRefRequestItem
import org.telegram.messenger.feature.media.fileref.domain.repository.FileRefRepository

class RequestReferenceRenewalUseCase(
    private val repository: FileRefRepository
) {
    operator fun invoke(item: FileRefRequestItem): Boolean = repository.requestReferenceRenewal(item)
}
