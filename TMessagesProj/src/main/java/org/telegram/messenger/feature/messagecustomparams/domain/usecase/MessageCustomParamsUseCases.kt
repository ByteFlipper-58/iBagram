package org.telegram.messenger.feature.messagecustomparams.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.messagecustomparams.domain.model.MessageCustomParamsModel
import org.telegram.messenger.feature.messagecustomparams.domain.model.MessageCustomParamsState
import org.telegram.messenger.feature.messagecustomparams.domain.model.MessageSummaryParamsModel
import org.telegram.messenger.feature.messagecustomparams.domain.model.MessageTranslationParamsModel
import org.telegram.messenger.feature.messagecustomparams.domain.model.VoiceTranscriptionParamsModel
import org.telegram.messenger.feature.messagecustomparams.domain.repository.MessageCustomParamsRepository

class CheckMessageCustomParamsEmptyUseCase {
    operator fun invoke(params: MessageCustomParamsModel?): Boolean {
        if (params == null) return true
        return params.isEmpty
    }
}

class MergeMessageCustomParamsUseCase {
    operator fun invoke(
        existing: MessageCustomParamsModel?,
        updates: MessageCustomParamsModel
    ): MessageCustomParamsModel {
        if (existing == null) return updates

        return existing.copy(
            voiceTranscription = updates.voiceTranscription ?: existing.voiceTranscription,
            summary = updates.summary ?: existing.summary,
            translation = updates.translation ?: existing.translation,
            starsError = updates.starsError ?: existing.starsError,
            premiumEffectWasPlayed = updates.premiumEffectWasPlayed || existing.premiumEffectWasPlayed
        )
    }
}

class ObserveMessageCustomParamsStateUseCase(
    private val repository: MessageCustomParamsRepository
) {
    operator fun invoke(): Flow<MessageCustomParamsState> = repository.observeState()
}

class GetMessageCustomParamsStateUseCase(
    private val repository: MessageCustomParamsRepository
) {
    operator fun invoke(): MessageCustomParamsState = repository.getState()
}

class GetMessageCustomParamsUseCase(
    private val repository: MessageCustomParamsRepository
) {
    operator fun invoke(messageId: Long): MessageCustomParamsModel? =
        repository.getParamsForMessage(messageId)
}

class SetMessageCustomParamsUseCase(
    private val repository: MessageCustomParamsRepository,
    private val mergeUseCase: MergeMessageCustomParamsUseCase = MergeMessageCustomParamsUseCase()
) {
    operator fun invoke(messageId: Long, params: MessageCustomParamsModel) {
        val existing = repository.getParamsForMessage(messageId)
        val merged = mergeUseCase(existing, params)
        repository.setParamsForMessage(messageId, merged)
    }
}

class UpdateVoiceTranscriptionUseCase(
    private val repository: MessageCustomParamsRepository
) {
    operator fun invoke(
        messageId: Long,
        text: String?,
        isFinal: Boolean = false,
        isOpen: Boolean = false,
        transcriptionId: Long = 0L
    ) {
        val existing = repository.getParamsForMessage(messageId) ?: MessageCustomParamsModel(messageId = messageId)
        val currentVoice = existing.voiceTranscription ?: VoiceTranscriptionParamsModel()
        val updatedVoice = currentVoice.copy(
            text = text,
            isFinal = isFinal,
            isOpen = isOpen,
            transcriptionId = if (transcriptionId != 0L) transcriptionId else currentVoice.transcriptionId
        )
        repository.setParamsForMessage(messageId, existing.copy(voiceTranscription = updatedVoice))
    }
}

class UpdateMessageTranslationUseCase(
    private val repository: MessageCustomParamsRepository
) {
    operator fun invoke(
        messageId: Long,
        targetLanguage: String?,
        translatedText: String?
    ) {
        val existing = repository.getParamsForMessage(messageId) ?: MessageCustomParamsModel(messageId = messageId)
        val currentTrans = existing.translation ?: MessageTranslationParamsModel()
        val updatedTrans = currentTrans.copy(
            translatedToLanguage = targetLanguage,
            translatedText = translatedText
        )
        repository.setParamsForMessage(messageId, existing.copy(translation = updatedTrans))
    }
}

class UpdateMessageSummaryUseCase(
    private val repository: MessageCustomParamsRepository
) {
    operator fun invoke(
        messageId: Long,
        summaryText: String?,
        isSummarizedOpen: Boolean = false
    ) {
        val existing = repository.getParamsForMessage(messageId) ?: MessageCustomParamsModel(messageId = messageId)
        val currentSummary = existing.summary ?: MessageSummaryParamsModel()
        val updatedSummary = currentSummary.copy(
            summaryText = summaryText,
            isSummarizedOpen = isSummarizedOpen
        )
        repository.setParamsForMessage(messageId, existing.copy(summary = updatedSummary))
    }
}

class CopyMessageCustomParamsUseCase(
    private val repository: MessageCustomParamsRepository
) {
    operator fun invoke(fromMessageId: Long, toMessageId: Long) =
        repository.copyParams(fromMessageId, toMessageId)
}

class RemoveMessageCustomParamsUseCase(
    private val repository: MessageCustomParamsRepository
) {
    operator fun invoke(messageId: Long) = repository.removeParams(messageId)
}

class ClearAllMessageCustomParamsUseCase(
    private val repository: MessageCustomParamsRepository
) {
    operator fun invoke() = repository.clearAll()
}
