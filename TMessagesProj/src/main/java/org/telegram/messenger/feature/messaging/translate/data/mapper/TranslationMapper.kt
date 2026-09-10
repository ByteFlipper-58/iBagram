package org.telegram.messenger.feature.messaging.translate.data.mapper

import org.telegram.messenger.LocaleController
import org.telegram.messenger.feature.messaging.translate.domain.model.DialogTranslationStateModel
import org.telegram.messenger.feature.messaging.translate.domain.model.LanguageModel
import org.telegram.messenger.feature.messaging.translate.domain.model.TranslateSettingsModel
import org.telegram.messenger.feature.messaging.translate.domain.model.TranslationResultModel
import org.telegram.tgnet.TLRPC

object TranslationMapper {

    fun mapLocaleInfo(info: LocaleController.LocaleInfo, currentShortName: String?): LanguageModel {
        val code = info.pluralLangCode ?: info.shortName ?: ""
        val isCurrent = currentShortName != null && (info.shortName == currentShortName || info.pluralLangCode == currentShortName)
        return LanguageModel(
            code = code,
            name = info.name ?: code,
            nativeName = info.nameEnglish ?: info.name ?: code,
            isOfficial = !info.isUnofficial(),
            isCurrent = isCurrent
        )
    }

    fun mapTranslateResult(
        result: TLRPC.TL_messages_translateResult,
        fromLanguage: String?,
        toLanguage: String
    ): TranslationResultModel {
        val builder = StringBuilder()
        for (i in 0 until result.result.size) {
            val textWithEntities = result.result[i]
            if (i > 0) {
                builder.append("\n\n")
            }
            builder.append(textWithEntities.text)
        }
        return TranslationResultModel(
            text = builder.toString(),
            fromLanguage = fromLanguage,
            toLanguage = toLanguage
        )
    }

    fun mapDialogState(
        dialogId: Long,
        isTranslatable: Boolean,
        isTranslating: Boolean,
        targetLanguage: String
    ): DialogTranslationStateModel {
        return DialogTranslationStateModel(
            dialogId = dialogId,
            isTranslatable = isTranslatable,
            isTranslating = isTranslating,
            targetLanguage = targetLanguage
        )
    }

    fun mapSettings(
        isChatEnabled: Boolean,
        isContextEnabled: Boolean,
        doNotTranslate: Set<String>,
        defaultLang: String
    ): TranslateSettingsModel {
        return TranslateSettingsModel(
            isChatTranslateEnabled = isChatEnabled,
            isContextTranslateEnabled = isContextEnabled,
            doNotTranslateLanguages = doNotTranslate,
            defaultLanguage = defaultLang
        )
    }
}
