package org.telegram.messenger.feature.translate.domain.model

data class LanguageModel(
    val code: String,
    val name: String,
    val nativeName: String,
    val isOfficial: Boolean,
    val isCurrent: Boolean,
    val totalStrings: Int = 0,
    val translatedStrings: Int = 0
)
