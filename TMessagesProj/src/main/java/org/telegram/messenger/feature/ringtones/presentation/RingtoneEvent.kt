package org.telegram.messenger.feature.ringtones.presentation

sealed class RingtoneEvent {
    data class LoadRingtones(val force: Boolean = false) : RingtoneEvent()
    data class SelectRingtone(val id: Long?) : RingtoneEvent()
    data class TogglePreview(val id: Long) : RingtoneEvent()
    data class UploadFile(
        val filePath: String,
        val fileName: String,
        val durationSec: Int,
        val sizeBytes: Long
    ) : RingtoneEvent()
    data class CancelUpload(val filePath: String) : RingtoneEvent()
    data class DeleteRingtone(val id: Long) : RingtoneEvent()
    data class SaveFromDocument(
        val documentId: Long,
        val title: String,
        val durationSec: Int,
        val sizeBytes: Long,
        val mimeType: String
    ) : RingtoneEvent()
    object ClearError : RingtoneEvent()
}
