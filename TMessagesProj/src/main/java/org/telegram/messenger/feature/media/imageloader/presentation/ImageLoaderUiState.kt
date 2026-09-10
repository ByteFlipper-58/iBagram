package org.telegram.messenger.feature.media.imageloader.presentation

import org.telegram.messenger.feature.media.imageloader.domain.model.ImageCacheStatsModel
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageCacheTier
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageRequestModel

data class ImageLoaderUiState(
    val requests: List<ImageRequestModel> = emptyList(),
    val activeCount: Int = 0,
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val cacheStats: ImageCacheStatsModel = ImageCacheStatsModel(),
    val selectedTier: ImageCacheTier = ImageCacheTier.DEFAULT,
    val isTrimming: Boolean = false,
    val lastTrimmedLevel: Int = 0,
    val errorMessage: String? = null
)
