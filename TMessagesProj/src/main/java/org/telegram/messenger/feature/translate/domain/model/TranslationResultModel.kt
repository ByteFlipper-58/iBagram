package org.telegram.messenger.feature.translate.domain.model

data class TranslationResultModel(
    val text: String,
    val fromLanguage: String? = null,
    val toLanguage: String
)
