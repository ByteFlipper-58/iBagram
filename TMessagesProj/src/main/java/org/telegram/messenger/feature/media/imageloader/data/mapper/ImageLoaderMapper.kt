package org.telegram.messenger.feature.media.imageloader.data.mapper

import org.telegram.messenger.feature.media.imageloader.domain.model.FrameExtractType
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageDownscaleSpec
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageFilterSpec
import java.util.Locale

object ImageLoaderMapper {

    fun parseFilter(filter: String?): ImageFilterSpec {
        if (filter.isNullOrEmpty()) return ImageFilterSpec()

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
        var dimIndex = 0

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
                        if (dimIndex == 0) {
                            width = num
                            dimIndex++
                        } else if (dimIndex == 1) {
                            height = num
                            dimIndex++
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

    fun calculateInSampleSize(srcWidth: Int, srcHeight: Int, reqWidth: Int, reqHeight: Int): Int {
        if (srcWidth <= 0 || srcHeight <= 0 || (reqWidth <= 0 && reqHeight <= 0)) {
            return 1
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
        return inSampleSize
    }

    fun calculateDownscale(srcWidth: Int, srcHeight: Int, reqWidth: Int, reqHeight: Int): ImageDownscaleSpec {
        val sampleSize = calculateInSampleSize(srcWidth, srcHeight, reqWidth, reqHeight)
        val targetW = if (reqWidth > 0) reqWidth else srcWidth
        val targetH = if (reqHeight > 0) reqHeight else srcHeight
        val scaledW = (srcWidth / sampleSize).coerceAtLeast(1)
        val scaledH = (srcHeight / sampleSize).coerceAtLeast(1)

        return ImageDownscaleSpec(
            srcWidth = srcWidth,
            srcHeight = srcHeight,
            targetWidth = targetW,
            targetHeight = targetH,
            sampleSize = sampleSize,
            scaledWidth = scaledW,
            scaledHeight = scaledH
        )
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = 1024L
        val mb = kb * 1024L
        val gb = mb * 1024L

        return when {
            bytes >= gb -> String.format(Locale.US, "%.1f GB", bytes.toDouble() / gb)
            bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes.toDouble() / mb)
            bytes >= kb -> String.format(Locale.US, "%.1f KB", bytes.toDouble() / kb)
            else -> "$bytes B"
        }
    }
}
