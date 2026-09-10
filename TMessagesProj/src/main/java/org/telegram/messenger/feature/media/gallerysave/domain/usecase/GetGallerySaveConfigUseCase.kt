package org.telegram.messenger.feature.media.gallerysave.domain.usecase

import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySaveConfigModel
import org.telegram.messenger.feature.media.gallerysave.domain.repository.GallerySaveRepository

class GetGallerySaveConfigUseCase(
    private val repository: GallerySaveRepository
) {
    operator fun invoke(): GallerySaveConfigModel {
        return repository.getConfig()
    }
}
