package org.telegram.messenger.feature.sendmessages.domain.model

enum class SendMediaType {
    TEXT,
    PHOTO,
    VIDEO,
    AUDIO,
    DOCUMENT,
    VOICE,
    ROUND_VIDEO,
    STICKER,
    CONTACT,
    LOCATION,
    POLL
}

enum class SendStatus {
    PENDING,
    PREPARING,
    UPLOADING,
    SENDING,
    SUCCESS,
    FAILED,
    CANCELLED
}

enum class ForwardMode {
    STANDARD,
    HIDE_NAMES,
    HIDE_CAPTIONS
}

data class SendOptionsModel(
    val notify: Boolean = true,
    val scheduleDate: Int = 0, // Unix timestamp in seconds, 0 = immediate
    val scheduleRepeatPeriod: Int = 0,
    val ttl: Int = 0, // View-once or self-destruct in seconds
    val replyToMsgId: Int = 0,
    val topMsgId: Int = 0,
    val payStars: Long = 0L,
    val effectId: Long = 0L,
    val sendAsPeerId: Long = 0L,
    val invertMedia: Boolean = false,
    val hasSpoiler: Boolean = false
) {
    val isScheduled: Boolean get() = scheduleDate > 0
    val isSilent: Boolean get() = !notify
    val isPaid: Boolean get() = payStars > 0L
    val isReply: Boolean get() = replyToMsgId > 0
}

data class SendMediaItem(
    val id: String,
    val path: String,
    val type: SendMediaType,
    val caption: String? = null,
    val sizeBytes: Long = 0L,
    val durationSeconds: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val hasSpoiler: Boolean = false,
    val waveform: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SendMediaItem
        if (id != other.id) return false
        if (path != other.path) return false
        if (type != other.type) return false
        if (caption != other.caption) return false
        if (sizeBytes != other.sizeBytes) return false
        if (durationSeconds != other.durationSeconds) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (hasSpoiler != other.hasSpoiler) return false
        if (waveform != null) {
            if (other.waveform == null) return false
            if (!waveform.contentEquals(other.waveform)) return false
        } else if (other.waveform != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (caption?.hashCode() ?: 0)
        result = 31 * result + sizeBytes.hashCode()
        result = 31 * result + durationSeconds
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + hasSpoiler.hashCode()
        result = 31 * result + (waveform?.contentHashCode() ?: 0)
        return result
    }
}

data class SendAlbumModel(
    val albumId: Long,
    val items: List<SendMediaItem>,
    val options: SendOptionsModel = SendOptionsModel()
) {
    init {
        require(items.size in 1..MAX_ALBUM_ITEMS) {
            "Media album must contain between 1 and $MAX_ALBUM_ITEMS items, got ${items.size}"
        }
    }

    companion object {
        const val MAX_ALBUM_ITEMS = 10
    }
}

data class ForwardRequestModel(
    val targetDialogId: Long,
    val sourceDialogId: Long,
    val messageIds: List<Int>,
    val mode: ForwardMode = ForwardMode.STANDARD,
    val options: SendOptionsModel = SendOptionsModel()
)

data class PendingSendModel(
    val localId: Long,
    val dialogId: Long,
    val type: SendMediaType,
    val text: String? = null,
    val status: SendStatus = SendStatus.PENDING,
    val progress: Float = 0f,
    val uploadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val options: SendOptionsModel = SendOptionsModel(),
    val errorReason: String? = null,
    val retryCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isUploading: Boolean get() = status == SendStatus.UPLOADING
    val isFailed: Boolean get() = status == SendStatus.FAILED
    val isFinished: Boolean get() = status == SendStatus.SUCCESS || status == SendStatus.CANCELLED
}

data class SendMessagesState(
    val pendingSends: List<PendingSendModel> = emptyList(),
    val activeUploadsCount: Int = 0,
    val isSending: Boolean = false
)
