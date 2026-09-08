package org.telegram.messenger.feature.translate.domain.model

data class DialogTranslationStateModel(
    val dialogId: Long,
    val isTranslatable: Boolean = false,
    val isTranslating: Boolean = false,
    val targetLanguage: String = "en"
)
