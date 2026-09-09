package org.telegram.messenger.feature.gallerysave.domain.usecase

import org.telegram.messenger.feature.gallerysave.domain.model.GallerySaveConfigModel
import org.telegram.messenger.feature.gallerysave.domain.repository.GallerySaveRepository

class GetGallerySaveConfigUseCase(
    private val repository: GallerySaveRepository
) {
    operator fun invoke(): GallerySaveConfigModel {
        return repository.getConfig()
    }
}
