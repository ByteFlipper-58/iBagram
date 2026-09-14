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

    fun mapDocumentToModel(document: org.telegram.tgnet.TLRPC.Document?, localUri: String? = null): org.telegram.messenger.feature.system.ringtones.domain.model.RingtoneModel? {
        if (document == null) return null
        var title: String? = null
        var duration = 0
        for (attr in document.attributes) {
            if (attr is org.telegram.tgnet.TLRPC.TL_documentAttributeAudio) {
                duration = attr.duration.toInt()
                if (!attr.title.isNullOrEmpty()) {
                    title = attr.title
                }
            } else if (attr is org.telegram.tgnet.TLRPC.TL_documentAttributeFilename) {
                if (title == null && !attr.file_name.isNullOrEmpty()) {
                    title = extractTitleFromFileName(attr.file_name)
                }
            }
        }
        return org.telegram.messenger.feature.system.ringtones.domain.model.RingtoneModel(
            id = document.id,
            title = title ?: "Sound",
            durationSec = duration,
            sizeBytes = document.size.toLong(),
            mimeType = document.mime_type ?: "audio/ogg",
            localUri = localUri
        )
    }

    fun mapCachedToneToModel(cachedTone: org.telegram.messenger.ringtone.RingtoneDataStore.CachedTone?): org.telegram.messenger.feature.system.ringtones.domain.model.RingtoneModel? {
        if (cachedTone == null || cachedTone.document == null) return null
        val model = mapDocumentToModel(cachedTone.document, cachedTone.localUri) ?: return null
        return model.copy(
            isUploading = cachedTone.uploading
        )
    }
}
