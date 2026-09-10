package org.telegram.messenger.feature.media.gallerysave.domain.usecase

import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySaveDialogExceptionModel
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySavePeerType
import org.telegram.messenger.feature.media.gallerysave.domain.repository.GallerySaveRepository

class SetGallerySaveExceptionUseCase(
    private val repository: GallerySaveRepository
) {
    operator fun invoke(peerType: GallerySavePeerType, exception: GallerySaveDialogExceptionModel) {
        repository.setException(peerType, exception)
    }
}
