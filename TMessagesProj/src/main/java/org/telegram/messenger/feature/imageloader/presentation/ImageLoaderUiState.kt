package org.telegram.messenger.feature.imageloader.presentation

import org.telegram.messenger.feature.imageloader.domain.model.ImageCacheStatsModel
import org.telegram.messenger.feature.imageloader.domain.model.ImageCacheTier
import org.telegram.messenger.feature.imageloader.domain.model.ImageRequestModel

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
