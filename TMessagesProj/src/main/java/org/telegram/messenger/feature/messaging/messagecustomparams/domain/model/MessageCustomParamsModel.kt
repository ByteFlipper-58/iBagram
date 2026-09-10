package org.telegram.messenger.feature.messaging.messagecustomparams.domain.model

/**
 * Pure domain models for message-level local parameters:
 * voice transcription, AI summarization, translations, Stars pricing errors, and effects.
 */

data class VoiceTranscriptionParamsModel(
    val text: String? = null,
    val isOpen: Boolean = false,
    val isFinal: Boolean = false,
    val isRated: Boolean = false,
    val isForce: Boolean = false,
    val transcriptionId: Long = 0L,
    val translatedText: String? = null
) {
    val isEmpty: Boolean
        get() = text == null && translatedText == null && !isOpen && !isFinal && !isRated && !isForce && transcriptionId == 0L
}

data class MessageSummaryParamsModel(
    val summaryText: String? = null,
    val translatedSummaryText: String? = null,
    val translatedSummaryLanguage: String? = null,
    val isSummarizedOpen: Boolean = false
) {
    val isEmpty: Boolean
        get() = summaryText == null && translatedSummaryText == null && translatedSummaryLanguage == null && !isSummarizedOpen
}

data class MessageTranslationParamsModel(
    val originalLanguage: String? = null,
    val translatedToLanguage: String? = null,
    val translatedText: String? = null,
    val translatedPollText: String? = null,
    val hasRichMessage: Boolean = false
) {
    val isEmpty: Boolean
        get() = originalLanguage == null && translatedToLanguage == null && translatedText == null && translatedPollText == null && !hasRichMessage
}

data class StarsErrorParamsModel(
    val errorAllowedPriceStars: Long = 0L,
    val errorNewPriceStars: Long = 0L
) {
    val isEmpty: Boolean
        get() = errorAllowedPriceStars == 0L && errorNewPriceStars == 0L
}

data class MessageCustomParamsModel(
    val messageId: Long = 0L,
    val voiceTranscription: VoiceTranscriptionParamsModel? = null,
    val summary: MessageSummaryParamsModel? = null,
    val translation: MessageTranslationParamsModel? = null,
    val starsError: StarsErrorParamsModel? = null,
    val premiumEffectWasPlayed: Boolean = false
) {
    val isEmpty: Boolean
        get() = (voiceTranscription == null || voiceTranscription.isEmpty) &&
                (summary == null || summary.isEmpty) &&
                (translation == null || translation.isEmpty) &&
                (starsError == null || starsError.isEmpty) &&
                !premiumEffectWasPlayed
}

data class MessageCustomParamsState(
    val cachedParamsCount: Int = 0,
    val lastUpdatedMessageId: Long = 0L
)
