package org.telegram.messenger.feature.system.localization.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.system.localization.domain.model.LocaleModel
import org.telegram.messenger.feature.system.localization.domain.model.LocalizationState
import org.telegram.messenger.feature.system.localization.domain.model.NameDisplayOrder
import java.util.concurrent.ConcurrentHashMap

open class LocalizationLocalDataSource(
    private val currentAccount: Int = 0,
    private val testMode: Boolean = false
) {
    private val lock = Any()
    private val customStringsMap = ConcurrentHashMap<String, String>()
    private val availableLocalesList = mutableListOf<LocaleModel>()

    private val _state = MutableStateFlow(
        LocalizationState(
            currentLocale = LocaleModel(
                code = "en",
                nativeName = "English",
                englishName = "English",
                pluralLangCode = "en",
                isRtl = false,
                isOfficial = true
            ),
            availableLocales = emptyList(),
            customStringOverrides = emptyMap(),
            is24HourFormat = false,
            nameDisplayOrder = NameDisplayOrder.FIRST_LAST,
            isRtl = false
        )
    )

    init {
        initDefaultLocales()
        publishState()
    }

    private fun initDefaultLocales() {
        availableLocalesList.addAll(
            listOf(
                LocaleModel(code = "en", nativeName = "English", englishName = "English", isRtl = false),
                LocaleModel(code = "ru", nativeName = "Русский", englishName = "Russian", isRtl = false),
                LocaleModel(code = "ar", nativeName = "العربية", englishName = "Arabic", isRtl = true),
                LocaleModel(code = "de", nativeName = "Deutsch", englishName = "German", isRtl = false),
                LocaleModel(code = "es", nativeName = "Español", englishName = "Spanish", isRtl = false),
                LocaleModel(code = "fa", nativeName = "فارسی", englishName = "Persian", isRtl = true)
            )
        )
    }

    open fun observeState(): Flow<LocalizationState> = _state.asStateFlow()

    open fun getState(): LocalizationState = _state.value

    open fun getCurrentLocale(): LocaleModel = _state.value.currentLocale

    open fun setCurrentLocale(locale: LocaleModel) {
        synchronized(lock) {
            _state.value = _state.value.copy(
                currentLocale = locale,
                isRtl = locale.isRtl
            )
        }
    }

    open fun getAvailableLocales(): List<LocaleModel> = synchronized(lock) { availableLocalesList.toList() }

    open fun setAvailableLocales(locales: List<LocaleModel>) {
        synchronized(lock) {
            availableLocalesList.clear()
            availableLocalesList.addAll(locales)
            publishState()
        }
    }

    open fun getString(key: String, defaultRes: String?): String {
        return customStringsMap[key] ?: defaultRes ?: key
    }

    open fun setCustomStrings(strings: Map<String, String>) {
        synchronized(lock) {
            customStringsMap.putAll(strings)
            publishState()
        }
    }

    open fun clearCustomStrings() {
        synchronized(lock) {
            customStringsMap.clear()
            publishState()
        }
    }

    open fun set24HourFormat(enabled: Boolean) {
        synchronized(lock) {
            _state.value = _state.value.copy(is24HourFormat = enabled)
        }
    }

    open fun setNameDisplayOrder(order: NameDisplayOrder) {
        synchronized(lock) {
            _state.value = _state.value.copy(nameDisplayOrder = order)
        }
    }

    private fun publishState() {
        _state.value = _state.value.copy(
            availableLocales = availableLocalesList.toList(),
            customStringOverrides = customStringsMap.toMap()
        )
    }
}
