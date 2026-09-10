package org.telegram.messenger.feature.media.imageloader.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.media.imageloader.domain.model.FrameExtractType
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageCacheTier
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageDownscaleSpec
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageFilterSpec
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageLoaderState
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageLoadingStatus
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageRequestModel
import org.telegram.messenger.feature.media.imageloader.domain.repository.ImageLoaderRepository

class ParseImageFilterUseCase {
    operator fun invoke(filter: String?): ImageFilterSpec {
        if (filter.isNullOrEmpty()) {
            return ImageFilterSpec()
        }

        var width = 0
        var height = 0
        var isWallpaper = false
        var isIgnoreCacheForSmall = false
        var isBlur = false
        var blurRadius = 0
        var isAutoplay = false
        var isAutoplayNonLoop = false
        var isRound = false
        var ignoreOrientation = false
        var checkExif = false
        var frameType = FrameExtractType.NONE

        val parts = filter.split("_")
        var dimCount = 0

        for (part in parts) {
            when {
                part == "f" -> isWallpaper = true
                part == "isc" -> isIgnoreCacheForSmall = true
                part == "b" -> {
                    isBlur = true
                    if (blurRadius == 0) blurRadius = 3
                }
                part.startsWith("b") && part.length > 1 && part.substring(1).toIntOrNull() != null -> {
                    isBlur = true
                    blurRadius = part.substring(1).toInt()
                }
                part == "g" -> isAutoplay = true
                part == "gl" -> {
                    isAutoplay = true
                    isAutoplayNonLoop = true
                }
                part == "r" -> isRound = true
                part == "ignoreOrientation" -> ignoreOrientation = true
                part == "exif" -> checkExif = true
                part == "firstframe" -> frameType = FrameExtractType.FIRST_FRAME
                part == "lastframe" -> frameType = FrameExtractType.LAST_FRAME
                part == "lastreactframe" -> frameType = FrameExtractType.LAST_REACT_FRAME
                part == "pframe" -> frameType = FrameExtractType.PREVIEW_FRAME
                else -> {
                    val num = part.toIntOrNull()
                    if (num != null) {
                        if (dimCount == 0) {
                            width = num
                            dimCount++
                        } else if (dimCount == 1) {
                            height = num
                            dimCount++
                        }
                    }
                }
            }
        }

        return ImageFilterSpec(
            width = width,
            height = height,
            isWallpaper = isWallpaper,
            isIgnoreCacheForSmall = isIgnoreCacheForSmall,
            isBlur = isBlur,
            blurRadius = blurRadius,
            isAutoplay = isAutoplay,
            isAutoplayNonLoop = isAutoplayNonLoop,
            isRound = isRound,
            ignoreOrientation = ignoreOrientation,
            checkExif = checkExif,
            frameExtractType = frameType
        )
    }
}

class FormatImageFilterUseCase {
    operator fun invoke(spec: ImageFilterSpec): String {
        val parts = mutableListOf<String>()
        if (spec.width > 0 && spec.height > 0) {
            parts.add("${spec.width}_${spec.height}")
        } else if (spec.width > 0) {
            parts.add("${spec.width}")
        }

        if (spec.isWallpaper) parts.add("f")
        if (spec.isIgnoreCacheForSmall) parts.add("isc")
        if (spec.isBlur) {
            if (spec.blurRadius > 0 && spec.blurRadius != 3) {
                parts.add("b${spec.blurRadius}")
            } else {
                parts.add("b")
            }
        }
        if (spec.isAutoplayNonLoop) {
            parts.add("gl")
        } else if (spec.isAutoplay) {
            parts.add("g")
        }
        if (spec.isRound) parts.add("r")
        if (spec.ignoreOrientation) parts.add("ignoreOrientation")
        if (spec.checkExif) parts.add("exif")

        when (spec.frameExtractType) {
            FrameExtractType.FIRST_FRAME -> parts.add("firstframe")
            FrameExtractType.LAST_FRAME -> parts.add("lastframe")
            FrameExtractType.LAST_REACT_FRAME -> parts.add("lastreactframe")
            FrameExtractType.PREVIEW_FRAME -> parts.add("pframe")
            FrameExtractType.NONE -> {}
        }

        return parts.joinToString("_")
    }
}

class BuildImageCacheKeyUseCase {
    operator fun invoke(baseKey: String, filter: String?): String {
        val trimmedBase = baseKey.trim()
        val trimmedFilter = filter?.trim()
        return if (!trimmedFilter.isNullOrEmpty()) {
            "${trimmedBase}_${trimmedFilter}"
        } else {
            trimmedBase
        }
    }
}

class CalculateImageDownscaleUseCase {
    operator fun invoke(srcWidth: Int, srcHeight: Int, reqWidth: Int, reqHeight: Int): ImageDownscaleSpec {
        if (srcWidth <= 0 || srcHeight <= 0) {
            return ImageDownscaleSpec(srcWidth, srcHeight, reqWidth, reqHeight, 1, 0, 0)
        }
        if (reqWidth <= 0 && reqHeight <= 0) {
            return ImageDownscaleSpec(srcWidth, srcHeight, reqWidth, reqHeight, 1, srcWidth, srcHeight)
        }

        val targetW = if (reqWidth > 0) reqWidth else srcWidth
        val targetH = if (reqHeight > 0) reqHeight else srcHeight

        var inSampleSize = 1
        if (srcHeight > targetH || srcWidth > targetW) {
            val halfHeight = srcHeight / 2
            val halfWidth = srcWidth / 2

            while ((halfHeight / inSampleSize) >= targetH && (halfWidth / inSampleSize) >= targetW) {
                inSampleSize *= 2
            }
        }

        val scaledW = (srcWidth / inSampleSize).coerceAtLeast(1)
        val scaledH = (srcHeight / inSampleSize).coerceAtLeast(1)

        return ImageDownscaleSpec(
            srcWidth = srcWidth,
            srcHeight = srcHeight,
            targetWidth = targetW,
            targetHeight = targetH,
            sampleSize = inSampleSize,
            scaledWidth = scaledW,
            scaledHeight = scaledH
        )
    }
}

class EvaluateImageCacheEligibilityUseCase {
    operator fun invoke(spec: ImageFilterSpec, width: Int = 0, height: Int = 0): ImageCacheTier {
        if (spec.isWallpaper) {
            return ImageCacheTier.WALLPAPER
        }
        if (spec.frameExtractType == FrameExtractType.FIRST_FRAME ||
            spec.frameExtractType == FrameExtractType.LAST_FRAME ||
            spec.frameExtractType == FrameExtractType.LAST_REACT_FRAME
        ) {
            return ImageCacheTier.LOTTIE
        }
        val effectiveW = if (spec.width > 0) spec.width else width
        val effectiveH = if (spec.height > 0) spec.height else height

        if (!spec.isIgnoreCacheForSmall && effectiveW > 0 && effectiveH > 0 && effectiveW <= 100 && effectiveH <= 100) {
            return ImageCacheTier.SMALL
        }
        return ImageCacheTier.DEFAULT
    }
}

class ObserveImageLoaderStateUseCase(private val repository: ImageLoaderRepository) {
    operator fun invoke(): Flow<ImageLoaderState> = repository.observeState()
}

class GetImageLoaderStateUseCase(private val repository: ImageLoaderRepository) {
    operator fun invoke(): ImageLoaderState = repository.getState()
}

class EnqueueImageRequestUseCase(
    private val repository: ImageLoaderRepository,
    private val evaluateTier: EvaluateImageCacheEligibilityUseCase,
    private val buildKey: BuildImageCacheKeyUseCase
) {
    operator fun invoke(
        key: String,
        url: String? = null,
        filter: String? = null,
        filterSpec: ImageFilterSpec = ImageFilterSpec(),
        priority: Int = 0,
        canForce8888: Boolean = false
    ): ImageRequestModel {
        val canonicalKey = buildKey(key, filter)
        val tier = evaluateTier(filterSpec)

        // Check if already in cache
        if (repository.hasInCache(canonicalKey, tier)) {
            repository.recordCacheHit(tier)
            return ImageRequestModel(
                key = canonicalKey,
                url = url,
                filter = filter,
                filterSpec = filterSpec,
                targetTier = tier,
                priority = priority,
                canForce8888 = canForce8888,
                status = ImageLoadingStatus.LOADED,
                progress = 1.0f
            )
        }

        repository.recordCacheMiss(tier)
        val request = ImageRequestModel(
            key = canonicalKey,
            url = url,
            filter = filter,
            filterSpec = filterSpec,
            targetTier = tier,
            priority = priority,
            canForce8888 = canForce8888,
            status = ImageLoadingStatus.QUEUED
        )
        repository.enqueueRequest(request)
        return request
    }
}

class CancelImageRequestUseCase(private val repository: ImageLoaderRepository) {
    operator fun invoke(key: String): Boolean = repository.cancelRequest(key)
}

class TrimImageMemoryUseCase(private val repository: ImageLoaderRepository) {
    operator fun invoke(level: Int) {
        repository.trimMemory(level)
    }
}

class ClearImageCacheUseCase(private val repository: ImageLoaderRepository) {
    operator fun invoke(tier: ImageCacheTier? = null) {
        repository.clearCache(tier)
    }
}
