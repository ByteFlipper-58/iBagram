package org.telegram.messenger.feature.gallerysave.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.gallerysave.domain.model.GallerySaveConfigModel
import org.telegram.messenger.feature.gallerysave.domain.repository.GallerySaveRepository

class ObserveGallerySaveConfigUseCase(
    private val repository: GallerySaveRepository
) {
    operator fun invoke(): Flow<GallerySaveConfigModel> {
        return repository.observeConfig()
    }
}
