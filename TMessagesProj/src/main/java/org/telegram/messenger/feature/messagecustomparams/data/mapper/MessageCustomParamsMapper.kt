package org.telegram.messenger.feature.messagecustomparams.data.mapper

import org.telegram.messenger.feature.messagecustomparams.domain.model.MessageCustomParamsModel
import org.telegram.messenger.feature.messagecustomparams.domain.model.MessageSummaryParamsModel
import org.telegram.messenger.feature.messagecustomparams.domain.model.MessageTranslationParamsModel
import org.telegram.messenger.feature.messagecustomparams.domain.model.StarsErrorParamsModel
import org.telegram.messenger.feature.messagecustomparams.domain.model.VoiceTranscriptionParamsModel
import org.telegram.tgnet.TLRPC

object MessageCustomParamsMapper {

    fun toDomain(message: TLRPC.Message): MessageCustomParamsModel {
        val voiceTranscription = if (
            message.voiceTranscription != null ||
            message.translatedVoiceTranscription != null ||
            message.voiceTranscriptionOpen ||
            message.voiceTranscriptionFinal ||
            message.voiceTranscriptionRated ||
            message.voiceTranscriptionForce ||
            message.voiceTranscriptionId != 0L
        ) {
            VoiceTranscriptionParamsModel(
                text = message.voiceTranscription,
                isOpen = message.voiceTranscriptionOpen,
                isFinal = message.voiceTranscriptionFinal,
                isRated = message.voiceTranscriptionRated,
                isForce = message.voiceTranscriptionForce,
                transcriptionId = message.voiceTranscriptionId,
                translatedText = message.translatedVoiceTranscription?.text
            )
        } else null

        val summary = if (
            message.summaryText != null ||
            message.translatedSummaryText != null ||
            message.translatedSummaryLanguage != null ||
            message.summarizedOpen
        ) {
            MessageSummaryParamsModel(
                summaryText = message.summaryText?.text,
                translatedSummaryText = message.translatedSummaryText?.text,
                translatedSummaryLanguage = message.translatedSummaryLanguage,
                isSummarizedOpen = message.summarizedOpen
            )
        } else null

        val translation = if (
            message.originalLanguage != null ||
            message.translatedToLanguage != null ||
            message.translatedText != null ||
            message.translatedPoll != null ||
            message.translatedRichMessage != null
        ) {
            MessageTranslationParamsModel(
                originalLanguage = message.originalLanguage,
                translatedToLanguage = message.translatedToLanguage,
                translatedText = message.translatedText?.text,
                translatedPollText = message.translatedPoll?.question?.text,
                hasRichMessage = message.translatedRichMessage != null
            )
        } else null

        val starsError = if (message.errorAllowedPriceStars != 0L || message.errorNewPriceStars != 0L) {
            StarsErrorParamsModel(
                errorAllowedPriceStars = message.errorAllowedPriceStars,
                errorNewPriceStars = message.errorNewPriceStars
            )
        } else null

        return MessageCustomParamsModel(
            messageId = message.id.toLong(),
            voiceTranscription = voiceTranscription,
            summary = summary,
            translation = translation,
            starsError = starsError,
            premiumEffectWasPlayed = message.premiumEffectWasPlayed
        )
    }

    fun applyToMessage(params: MessageCustomParamsModel, message: TLRPC.Message) {
        val voice = params.voiceTranscription
        if (voice != null) {
            message.voiceTranscription = voice.text
            message.voiceTranscriptionOpen = voice.isOpen
            message.voiceTranscriptionFinal = voice.isFinal
            message.voiceTranscriptionRated = voice.isRated
            message.voiceTranscriptionForce = voice.isForce
            message.voiceTranscriptionId = voice.transcriptionId
            if (voice.translatedText != null) {
                val t = TLRPC.TL_textWithEntities()
                t.text = voice.translatedText
                message.translatedVoiceTranscription = t
            }
        }

        val summary = params.summary
        if (summary != null) {
            if (summary.summaryText != null) {
                val s = TLRPC.TL_textWithEntities()
                s.text = summary.summaryText
                message.summaryText = s
            }
            if (summary.translatedSummaryText != null) {
                val ts = TLRPC.TL_textWithEntities()
                ts.text = summary.translatedSummaryText
                message.translatedSummaryText = ts
            }
            message.translatedSummaryLanguage = summary.translatedSummaryLanguage
            message.summarizedOpen = summary.isSummarizedOpen
        }

        val trans = params.translation
        if (trans != null) {
            message.originalLanguage = trans.originalLanguage
            message.translatedToLanguage = trans.translatedToLanguage
            if (trans.translatedText != null) {
                val tt = TLRPC.TL_textWithEntities()
                tt.text = trans.translatedText
                message.translatedText = tt
            }
        }

        val stars = params.starsError
        if (stars != null) {
            message.errorAllowedPriceStars = stars.errorAllowedPriceStars
            message.errorNewPriceStars = stars.errorNewPriceStars
        }

        message.premiumEffectWasPlayed = params.premiumEffectWasPlayed
    }
}
