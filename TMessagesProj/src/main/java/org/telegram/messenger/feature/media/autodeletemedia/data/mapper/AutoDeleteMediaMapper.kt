package org.telegram.messenger.feature.media.autodeletemedia.data.mapper

import org.telegram.messenger.AutoDeleteMediaTask
import org.telegram.messenger.feature.media.autodeletemedia.domain.model.CacheLimitConfig
import org.telegram.messenger.feature.media.autodeletemedia.domain.model.MediaScanFileModel
import java.io.File

object AutoDeleteMediaMapper {

    fun toMediaScanFile(
        file: File,
        lastUsageTimeSec: Long,
        isStory: Boolean = false,
        dialogType: Int = -1,
        keepMediaDays: Int = 30,
        isKeepForever: Boolean = false
    ): MediaScanFileModel {
        val path = file.absolutePath
        val isLocked = runCatching {
            AutoDeleteMediaTask.usingFilePaths.contains(path)
        }.getOrDefault(false)

        return MediaScanFileModel(
            path = path,
            sizeBytes = runCatching { file.length() }.getOrDefault(0L),
            lastUsageTimeSec = lastUsageTimeSec,
            isStory = isStory,
            isLocked = isLocked,
            dialogType = dialogType,
            keepMediaDays = keepMediaDays,
            isKeepForever = isKeepForever
        )
    }

    fun toCacheLimitConfig(maxCacheSizeGb: Int): CacheLimitConfig {
        return CacheLimitConfig(maxCacheSizeGb = maxCacheSizeGb)
    }
}
