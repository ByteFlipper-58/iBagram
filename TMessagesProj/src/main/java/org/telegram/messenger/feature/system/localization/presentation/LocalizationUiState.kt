package org.telegram.messenger.feature.system.localization.presentation

import org.telegram.messenger.feature.system.localization.domain.model.LocaleModel
import org.telegram.messenger.feature.system.localization.domain.model.NameDisplayOrder

data class LocalizationUiState(
    val currentLocale: LocaleModel = LocaleModel(
        code = "en",
        nativeName = "English",
        englishName = "English"
    ),
    val availableLocales: List<LocaleModel> = emptyList(),
    val is24HourFormat: Boolean = false,
    val nameDisplayOrder: NameDisplayOrder = NameDisplayOrder.FIRST_LAST,
    val isRtl: Boolean = false,
    val customStringsCount: Int = 0,
    val searchQuery: String = "",
    val filteredLocales: List<LocaleModel> = emptyList(),
    val errorMessage: String? = null
)
