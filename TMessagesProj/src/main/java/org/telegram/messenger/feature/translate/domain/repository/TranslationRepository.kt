package org.telegram.messenger.feature.translate.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.translate.domain.model.DialogTranslationStateModel
import org.telegram.messenger.feature.translate.domain.model.LanguageModel
import org.telegram.messenger.feature.translate.domain.model.TranslateSettingsModel
import org.telegram.messenger.feature.translate.domain.model.TranslationResultModel

interface TranslationRepository {
    fun observeTranslateSettings(): Flow<TranslateSettingsModel>
    suspend fun getTranslateSettings(): Result<TranslateSettingsModel>

    suspend fun setChatTranslateEnabled(enabled: Boolean): Result<Unit>
    suspend fun setContextTranslateEnabled(enabled: Boolean): Result<Unit>

    suspend fun addDoNotTranslateLanguage(languageCode: String): Result<Unit>
    suspend fun removeDoNotTranslateLanguage(languageCode: String): Result<Unit>
    suspend fun setDoNotTranslateLanguages(languages: Set<String>): Result<Unit>

    fun observeDialogTranslationState(dialogId: Long): Flow<DialogTranslationStateModel>
    suspend fun getDialogTranslationState(dialogId: Long): Result<DialogTranslationStateModel>
    suspend fun toggleDialogTranslating(dialogId: Long, enabled: Boolean): Result<Unit>
    suspend fun setDialogTranslateTargetLanguage(dialogId: Long, languageCode: String): Result<Unit>

    suspend fun translateText(
        text: String,
        fromLanguage: String?,
        toLanguage: String
    ): Result<TranslationResultModel>

    suspend fun getAvailableLanguages(): Result<List<LanguageModel>>
    suspend fun applyAppLanguage(languageCode: String): Result<Unit>
}
