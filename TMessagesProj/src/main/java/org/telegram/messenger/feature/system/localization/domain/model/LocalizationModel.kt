package org.telegram.messenger.feature.system.localization.domain.model

enum class PluralQuantity {
    ZERO,
    ONE,
    TWO,
    FEW,
    MANY,
    OTHER
}

enum class NameDisplayOrder {
    FIRST_LAST,
    LAST_FIRST
}

data class LocaleModel(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val baseLangCode: String? = null,
    val pluralLangCode: String = code,
    val isRtl: Boolean = false,
    val isLocal: Boolean = false,
    val isOfficial: Boolean = true,
    val version: Int = 0
)

data class RelativeTimeModel(
    val formatted: String,
    val isRecent: Boolean = false
)

data class LocalizationConfigModel(
    val currentLocale: LocaleModel,
    val is24HourFormat: Boolean = false,
    val nameDisplayOrder: NameDisplayOrder = NameDisplayOrder.FIRST_LAST,
    val isRtl: Boolean = false
)

data class LocalizationState(
    val currentLocale: LocaleModel = LocaleModel(
        code = "en",
        nativeName = "English",
        englishName = "English",
        pluralLangCode = "en",
        isRtl = false,
        isOfficial = true
    ),
    val availableLocales: List<LocaleModel> = emptyList(),
    val customStringOverrides: Map<String, String> = emptyMap(),
    val is24HourFormat: Boolean = false,
    val nameDisplayOrder: NameDisplayOrder = NameDisplayOrder.FIRST_LAST,
    val isRtl: Boolean = false
)
