package org.telegram.messenger.feature.fileref.domain.usecase

import org.telegram.messenger.feature.fileref.domain.repository.FileRefRepository

class NotifyReferenceRenewedUseCase(
    private val repository: FileRefRepository
) {
    operator fun invoke(locationKey: String, parentKey: String, refLength: Int = 0) {
        repository.notifyReferenceRenewed(locationKey, parentKey, refLength)
    }
}
