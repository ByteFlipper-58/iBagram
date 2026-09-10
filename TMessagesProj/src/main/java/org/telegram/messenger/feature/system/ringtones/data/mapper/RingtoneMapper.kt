package org.telegram.messenger.feature.system.ringtones.data.mapper

import java.util.Locale

object RingtoneMapper {

    fun formatDuration(durationSec: Int): String {
        val minutes = durationSec / 60
        val seconds = durationSec % 60
        return String.format(Locale.US, "%d:%02d", minutes, seconds)
    }

    fun formatFileSize(sizeBytes: Long): String {
        return when {
            sizeBytes >= 1024 * 1024 -> {
                String.format(Locale.US, "%.1f MB", sizeBytes.toDouble() / (1024.0 * 1024.0))
            }
            sizeBytes >= 1024 -> {
                String.format(Locale.US, "%.1f KB", sizeBytes.toDouble() / 1024.0)
            }
            else -> "$sizeBytes B"
        }
    }

    fun resolveMimeTypeFromExtension(ext: String?): String {
        val cleanExt = ext?.lowercase()?.trimStart('.') ?: ""
        return when (cleanExt) {
            "ogg" -> "audio/ogg"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/m4a"
            else -> "audio/mpeg"
        }
    }

    fun extractTitleFromFileName(fileName: String): String {
        return fileName.substringBeforeLast('.')
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
            .ifEmpty { "Sound" }
    }
}
