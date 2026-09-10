package org.telegram.messenger.feature.messaging.translate.presentation

sealed class TranslateEvent {
    object LoadSettings : TranslateEvent()
    object LoadAvailableLanguages : TranslateEvent()
    data class LoadDialogState(val dialogId: Long) : TranslateEvent()
    data class SetChatTranslateEnabled(val enabled: Boolean) : TranslateEvent()
    data class SetContextTranslateEnabled(val enabled: Boolean) : TranslateEvent()
    data class AddDoNotTranslateLanguage(val languageCode: String) : TranslateEvent()
    data class RemoveDoNotTranslateLanguage(val languageCode: String) : TranslateEvent()
    data class SetDoNotTranslateLanguages(val languages: Set<String>) : TranslateEvent()
    data class ToggleDialogTranslating(val dialogId: Long, val enabled: Boolean) : TranslateEvent()
    data class SetDialogTargetLanguage(val dialogId: Long, val languageCode: String) : TranslateEvent()
    data class TranslateText(
        val text: String,
        val fromLanguage: String? = null,
        val toLanguage: String
    ) : TranslateEvent()
    data class ApplyAppLanguage(val languageCode: String) : TranslateEvent()
    object ClearMessages : TranslateEvent()
}
