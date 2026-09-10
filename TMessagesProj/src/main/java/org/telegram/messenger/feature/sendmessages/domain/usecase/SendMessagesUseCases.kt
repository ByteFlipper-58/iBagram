package org.telegram.messenger.feature.sendmessages.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.sendmessages.domain.model.ForwardRequestModel
import org.telegram.messenger.feature.sendmessages.domain.model.PendingSendModel
import org.telegram.messenger.feature.sendmessages.domain.model.SendAlbumModel
import org.telegram.messenger.feature.sendmessages.domain.model.SendMediaItem
import org.telegram.messenger.feature.sendmessages.domain.model.SendMediaType
import org.telegram.messenger.feature.sendmessages.domain.model.SendOptionsModel
import org.telegram.messenger.feature.sendmessages.domain.repository.SendMessagesRepository

class SendTextMessageUseCase(
    private val repository: SendMessagesRepository
) {
    companion object {
        const val MAX_TEXT_LENGTH_STANDARD = 4096
        const val MAX_TEXT_LENGTH_PREMIUM = 8192
    }

    suspend operator fun invoke(
        dialogId: Long,
        text: String,
        options: SendOptionsModel = SendOptionsModel(),
        isPremium: Boolean = false
    ): PendingSendModel {
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { "Message text cannot be empty" }

        val maxLength = if (isPremium) MAX_TEXT_LENGTH_PREMIUM else MAX_TEXT_LENGTH_STANDARD
        require(trimmed.length <= maxLength) {
            "Message text exceeds maximum length of $maxLength characters (current: ${trimmed.length})"
        }

        return repository.sendText(dialogId, trimmed, options)
    }
}

class SendMediaMessageUseCase(
    private val repository: SendMessagesRepository
) {
    companion object {
        const val MAX_FILE_SIZE_BYTES_STANDARD = 2000L * 1024L * 1024L // 2GB
        const val MAX_FILE_SIZE_BYTES_PREMIUM = 4000L * 1024L * 1024L  // 4GB
        const val MAX_CAPTION_LENGTH_STANDARD = 1024
        const val MAX_CAPTION_LENGTH_PREMIUM = 2048
    }

    suspend operator fun invoke(
        dialogId: Long,
        item: SendMediaItem,
        options: SendOptionsModel = SendOptionsModel(),
        isPremium: Boolean = false
    ): PendingSendModel {
        require(item.path.isNotBlank()) { "Media file path cannot be empty" }

        val maxFileSize = if (isPremium) MAX_FILE_SIZE_BYTES_PREMIUM else MAX_FILE_SIZE_BYTES_STANDARD
        if (item.sizeBytes > 0L) {
            require(item.sizeBytes <= maxFileSize) {
                "Media file size (${item.sizeBytes} bytes) exceeds limit of $maxFileSize bytes"
            }
        }

        val maxCaption = if (isPremium) MAX_CAPTION_LENGTH_PREMIUM else MAX_CAPTION_LENGTH_STANDARD
        item.caption?.let { caption ->
            require(caption.length <= maxCaption) {
                "Caption exceeds maximum length of $maxCaption characters (current: ${caption.length})"
            }
        }

        return repository.sendMedia(dialogId, item, options)
    }
}

class SendMediaAlbumUseCase(
    private val repository: SendMessagesRepository
) {
    suspend operator fun invoke(
        dialogId: Long,
        items: List<SendMediaItem>,
        options: SendOptionsModel = SendOptionsModel()
    ): List<PendingSendModel> {
        require(items.isNotEmpty()) { "Album must contain at least 1 media item" }
        require(items.size <= SendAlbumModel.MAX_ALBUM_ITEMS) {
            "Album cannot contain more than ${SendAlbumModel.MAX_ALBUM_ITEMS} items (current: ${items.size})"
        }

        // Validate that album items are photos or videos
        val invalidItems = items.filter { it.type != SendMediaType.PHOTO && it.type != SendMediaType.VIDEO }
        require(invalidItems.isEmpty()) {
            "Album items must be PHOTO or VIDEO only, found: ${invalidItems.map { it.type }}"
        }

        return repository.sendAlbum(dialogId, items, options)
    }
}

class ForwardMessagesUseCase(
    private val repository: SendMessagesRepository
) {
    suspend operator fun invoke(request: ForwardRequestModel): List<PendingSendModel> {
        require(request.messageIds.isNotEmpty()) { "Message IDs list to forward cannot be empty" }
        require(request.targetDialogId != 0L) { "Target dialog ID cannot be 0" }
        return repository.forwardMessages(request)
    }
}

class RetrySendMessageUseCase(
    private val repository: SendMessagesRepository
) {
    suspend operator fun invoke(localId: Long): Boolean {
        return repository.retrySend(localId)
    }
}

class CancelSendMessageUseCase(
    private val repository: SendMessagesRepository
) {
    suspend operator fun invoke(localId: Long): Boolean {
        return repository.cancelSend(localId)
    }
}

class ObservePendingSendsUseCase(
    private val repository: SendMessagesRepository
) {
    operator fun invoke(dialogId: Long? = null): Flow<List<PendingSendModel>> {
        return repository.observePendingSends(dialogId)
    }
}

class ValidateSendEligibilityUseCase {
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }

    fun validateText(text: String, isPremium: Boolean = false): ValidationResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ValidationResult.Invalid("Text cannot be empty")
        val maxLen = if (isPremium) SendTextMessageUseCase.MAX_TEXT_LENGTH_PREMIUM else SendTextMessageUseCase.MAX_TEXT_LENGTH_STANDARD
        if (trimmed.length > maxLen) {
            return ValidationResult.Invalid("Text exceeds max length of $maxLen (length: ${trimmed.length})")
        }
        return ValidationResult.Valid
    }

    fun validateAlbum(items: List<SendMediaItem>): ValidationResult {
        if (items.isEmpty()) return ValidationResult.Invalid("Album is empty")
        if (items.size > SendAlbumModel.MAX_ALBUM_ITEMS) {
            return ValidationResult.Invalid("Album contains more than ${SendAlbumModel.MAX_ALBUM_ITEMS} items")
        }
        val unsupported = items.filter { it.type != SendMediaType.PHOTO && it.type != SendMediaType.VIDEO }
        if (unsupported.isNotEmpty()) {
            return ValidationResult.Invalid("Album supports only PHOTO and VIDEO")
        }
        return ValidationResult.Valid
    }
}
