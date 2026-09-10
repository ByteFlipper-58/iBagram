package org.telegram.messenger.feature.ringtones.domain.model

enum class RingtoneErrorCode {
    NONE,
    TOO_LONG,
    TOO_BIG,
    UNSUPPORTED_FORMAT,
    FILE_NOT_FOUND
}

enum class RingtoneUploadStatus {
    PENDING,
    UPLOADING,
    CONVERTING,
    COMPLETED,
    FAILED
}

data class RingtoneModel(
    val id: Long,
    val title: String,
    val durationSec: Int,
    val sizeBytes: Long,
    val mimeType: String,
    val localUri: String? = null,
    val isUploading: Boolean = false,
    val isConverting: Boolean = false,
    val date: Long = System.currentTimeMillis()
)

data class RingtoneValidationResult(
    val isValid: Boolean,
    val errorCode: RingtoneErrorCode = RingtoneErrorCode.NONE,
    val message: String? = null
)

data class RingtoneLimitsModel(
    val maxDurationSeconds: Int = 5,
    val maxSizeBytes: Long = 300 * 1024L, // 300 KB
    val supportedMimeTypes: Set<String> = setOf(
        "audio/mpeg3",
        "audio/mpeg",
        "audio/ogg",
        "audio/m4a"
    ),
    val supportedExtensions: Set<String> = setOf(
        "mp3",
        "ogg",
        "m4a"
    )
)

data class RingtoneState(
    val ringtones: List<RingtoneModel> = emptyList(),
    val selectedRingtoneId: Long? = null,
    val isLoading: Boolean = false,
    val limits: RingtoneLimitsModel = RingtoneLimitsModel()
)
