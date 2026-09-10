package org.telegram.messenger.feature.localization.presentation

import org.telegram.messenger.feature.localization.domain.model.LocaleModel
import org.telegram.messenger.feature.localization.domain.model.NameDisplayOrder

sealed class LocalizationEvent {
    data class SelectLocale(val locale: LocaleModel) : LocalizationEvent()
    data class SearchLocales(val query: String) : LocalizationEvent()
    data class Toggle24HourFormat(val enabled: Boolean) : LocalizationEvent()
    data class ChangeNameDisplayOrder(val order: NameDisplayOrder) : LocalizationEvent()
    data class ApplyCustomStrings(val strings: Map<String, String>) : LocalizationEvent()
    object ResetCustomStrings : LocalizationEvent()
}
