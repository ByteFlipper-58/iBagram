package org.telegram.messenger.feature.media.sharedmedia.domain.usecase

import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaFilterType
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaItem

/**
 * Юзкейс для фильтрации списка фото и видео (ALL, PHOTOS_ONLY, VIDEOS_ONLY).
 */
class FilterSharedMediaUseCase {

    operator fun invoke(
        items: List<SharedMediaItem>,
        filter: SharedMediaFilterType
    ): List<SharedMediaItem> {
        return when (filter) {
            SharedMediaFilterType.ALL -> items
            SharedMediaFilterType.PHOTOS_ONLY -> items.filter { it.isPhoto && !it.isVideo }
            SharedMediaFilterType.VIDEOS_ONLY -> items.filter { it.isVideo }
        }
    }
}
