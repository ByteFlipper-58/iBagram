package org.telegram.messenger.feature.system.localization.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.localization.domain.model.LocaleModel
import org.telegram.messenger.feature.system.localization.domain.model.LocalizationState
import org.telegram.messenger.feature.system.localization.domain.model.NameDisplayOrder

interface LocalizationRepository {
    fun observeState(): Flow<LocalizationState>
    fun getState(): LocalizationState
    fun getCurrentLocale(): LocaleModel
    fun setCurrentLocale(locale: LocaleModel)
    fun getAvailableLocales(): List<LocaleModel>
    fun setAvailableLocales(locales: List<LocaleModel>)
    fun getString(key: String, defaultRes: String? = null): String
    fun setCustomStrings(strings: Map<String, String>)
    fun clearCustomStrings()
    fun set24HourFormat(enabled: Boolean)
    fun setNameDisplayOrder(order: NameDisplayOrder)
}
