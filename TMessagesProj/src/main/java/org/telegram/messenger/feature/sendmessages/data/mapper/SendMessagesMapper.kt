package org.telegram.messenger.feature.sendmessages.data.mapper

import org.telegram.messenger.feature.sendmessages.domain.model.SendMediaItem
import org.telegram.messenger.feature.sendmessages.domain.model.SendMediaType

object SendMessagesMapper {

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = 1024L
        val mb = kb * 1024L
        val gb = mb * 1024L

        return when {
            bytes >= gb -> String.format(java.util.Locale.US, "%.1f GB", bytes.toDouble() / gb)
            bytes >= mb -> String.format(java.util.Locale.US, "%.1f MB", bytes.toDouble() / mb)
            bytes >= kb -> String.format(java.util.Locale.US, "%.1f KB", bytes.toDouble() / kb)
            else -> "$bytes B"
        }
    }

    fun calculateUploadProgress(uploadedBytes: Long, totalBytes: Long): Float {
        if (totalBytes <= 0L || uploadedBytes <= 0L) return 0f
        return (uploadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
    }

    fun isVisualMedia(type: SendMediaType): Boolean {
        return type == SendMediaType.PHOTO || type == SendMediaType.VIDEO || type == SendMediaType.ROUND_VIDEO
    }

    fun isAudioMedia(type: SendMediaType): Boolean {
        return type == SendMediaType.AUDIO || type == SendMediaType.VOICE
    }

    fun canGroupIntoAlbum(items: List<SendMediaItem>): Boolean {
        if (items.size !in 1..10) return false
        return items.all { it.type == SendMediaType.PHOTO || it.type == SendMediaType.VIDEO }
    }
}
