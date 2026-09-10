package org.telegram.messenger.feature.imageloader.presentation

import org.telegram.messenger.feature.imageloader.domain.model.ImageCacheTier
import org.telegram.messenger.feature.imageloader.domain.model.ImageFilterSpec

sealed class ImageLoaderEvent {
    data class EnqueueRequest(
        val key: String,
        val url: String? = null,
        val filter: String? = null,
        val filterSpec: ImageFilterSpec = ImageFilterSpec(),
        val priority: Int = 0,
        val canForce8888: Boolean = false
    ) : ImageLoaderEvent()

    data class CancelRequest(val key: String) : ImageLoaderEvent()
    data class SelectCacheTier(val tier: ImageCacheTier) : ImageLoaderEvent()
    data class ClearCache(val tier: ImageCacheTier? = null) : ImageLoaderEvent()
    data class TrimMemory(val level: Int) : ImageLoaderEvent()
}
