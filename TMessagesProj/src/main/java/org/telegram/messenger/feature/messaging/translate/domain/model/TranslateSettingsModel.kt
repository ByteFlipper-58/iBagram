package org.telegram.messenger.feature.messaging.translate.domain.model

data class TranslateSettingsModel(
    val isChatTranslateEnabled: Boolean = true,
    val isContextTranslateEnabled: Boolean = false,
    val doNotTranslateLanguages: Set<String> = emptySet(),
    val defaultLanguage: String = "en"
)
