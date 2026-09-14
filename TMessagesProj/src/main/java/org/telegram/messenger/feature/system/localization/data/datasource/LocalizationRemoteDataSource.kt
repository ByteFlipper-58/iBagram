package org.telegram.messenger.feature.system.localization.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.localization.domain.model.LocaleModel

open class LocalizationRemoteDataSource(
    currentAccount: Int = 0
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun fetchRemoteLocales(): Result<List<LocaleModel>> {
        return Result.success(
            listOf(
                LocaleModel(code = "en", nativeName = "English", englishName = "English", isRtl = false, isOfficial = true),
                LocaleModel(code = "ru", nativeName = "Русский", englishName = "Russian", isRtl = false, isOfficial = true),
                LocaleModel(code = "ar", nativeName = "العربية", englishName = "Arabic", isRtl = true, isOfficial = true),
                LocaleModel(code = "de", nativeName = "Deutsch", englishName = "German", isRtl = false, isOfficial = true),
                LocaleModel(code = "es", nativeName = "Español", englishName = "Spanish", isRtl = false, isOfficial = true),
                LocaleModel(code = "fa", nativeName = "فارسی", englishName = "Persian", isRtl = true, isOfficial = true)
            )
        )
    }

    open suspend fun fetchLanguagePack(langCode: String): Result<Map<String, String>> {
        return Result.success(emptyMap())
    }
}
