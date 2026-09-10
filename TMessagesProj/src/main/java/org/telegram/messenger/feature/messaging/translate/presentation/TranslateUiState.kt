package org.telegram.messenger.feature.messaging.translate.presentation

import org.telegram.messenger.feature.messaging.translate.domain.model.DialogTranslationStateModel
import org.telegram.messenger.feature.messaging.translate.domain.model.LanguageModel
import org.telegram.messenger.feature.messaging.translate.domain.model.TranslateSettingsModel
import org.telegram.messenger.feature.messaging.translate.domain.model.TranslationResultModel

data class TranslateUiState(
    val settings: TranslateSettingsModel = TranslateSettingsModel(),
    val availableLanguages: List<LanguageModel> = emptyList(),
    val currentDialogState: DialogTranslationStateModel? = null,
    val lastTranslation: TranslationResultModel? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null
)
