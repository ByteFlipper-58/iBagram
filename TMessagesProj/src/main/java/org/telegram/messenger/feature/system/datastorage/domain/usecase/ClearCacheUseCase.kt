package org.telegram.messenger.feature.system.datastorage.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.datastorage.domain.repository.DataStorageRepository

class ClearCacheUseCase(
    private val repository: DataStorageRepository
) {
    suspend operator fun invoke(
        clearPhotos: Boolean = true,
        clearVideos: Boolean = true,
        clearDocuments: Boolean = true,
        clearMusic: Boolean = true,
        clearAudio: Boolean = true,
        clearStickers: Boolean = true,
        clearStories: Boolean = true,
        clearOther: Boolean = true
    ): Result<Unit> = repository.clearCache(
        clearPhotos = clearPhotos,
        clearVideos = clearVideos,
        clearDocuments = clearDocuments,
        clearMusic = clearMusic,
        clearAudio = clearAudio,
        clearStickers = clearStickers,
        clearStories = clearStories,
        clearOther = clearOther
    )
}
