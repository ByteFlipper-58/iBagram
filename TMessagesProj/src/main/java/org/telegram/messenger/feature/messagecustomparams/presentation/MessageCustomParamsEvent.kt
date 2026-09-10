package org.telegram.messenger.feature.messagecustomparams.presentation

import org.telegram.messenger.feature.messagecustomparams.domain.model.MessageCustomParamsModel

sealed class MessageCustomParamsEvent {
    data class LoadParams(val messageId: Long) : MessageCustomParamsEvent()
    data class SetParams(val messageId: Long, val params: MessageCustomParamsModel) : MessageCustomParamsEvent()
    data class UpdateTranscription(
        val messageId: Long,
        val text: String?,
        val isFinal: Boolean = false,
        val isOpen: Boolean = false
    ) : MessageCustomParamsEvent()
    data class UpdateTranslation(
        val messageId: Long,
        val targetLanguage: String?,
        val translatedText: String?
    ) : MessageCustomParamsEvent()
    data class UpdateSummary(
        val messageId: Long,
        val summaryText: String?,
        val isOpen: Boolean = false
    ) : MessageCustomParamsEvent()
    data class CopyParams(val fromMessageId: Long, val toMessageId: Long) : MessageCustomParamsEvent()
    data class RemoveParams(val messageId: Long) : MessageCustomParamsEvent()
    object ClearAll : MessageCustomParamsEvent()
    object DismissMessage : MessageCustomParamsEvent()
}
