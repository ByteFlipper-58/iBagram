package org.telegram.messenger.feature.gallerysave.domain.usecase

import org.telegram.messenger.feature.gallerysave.domain.model.GallerySaveDialogExceptionModel
import org.telegram.messenger.feature.gallerysave.domain.model.GallerySavePeerType
import org.telegram.messenger.feature.gallerysave.domain.repository.GallerySaveRepository

class SetGallerySaveExceptionUseCase(
    private val repository: GallerySaveRepository
) {
    operator fun invoke(peerType: GallerySavePeerType, exception: GallerySaveDialogExceptionModel) {
        repository.setException(peerType, exception)
    }
}
