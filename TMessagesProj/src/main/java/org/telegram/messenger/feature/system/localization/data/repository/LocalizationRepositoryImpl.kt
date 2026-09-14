package org.telegram.messenger.feature.system.localization.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.localization.data.datasource.LocalizationLocalDataSource
import org.telegram.messenger.feature.system.localization.data.datasource.LocalizationRemoteDataSource
import org.telegram.messenger.feature.system.localization.domain.model.LocaleModel
import org.telegram.messenger.feature.system.localization.domain.model.LocalizationState
import org.telegram.messenger.feature.system.localization.domain.model.NameDisplayOrder
import org.telegram.messenger.feature.system.localization.domain.repository.LocalizationRepository

class LocalizationRepositoryImpl(
    private val localDataSource: LocalizationLocalDataSource,
    private val remoteDataSource: LocalizationRemoteDataSource
) : LocalizationRepository {

    override fun observeState(): Flow<LocalizationState> = localDataSource.observeState()

    override fun getState(): LocalizationState = localDataSource.getState()

    override fun getCurrentLocale(): LocaleModel = localDataSource.getCurrentLocale()

    override fun setCurrentLocale(locale: LocaleModel) {
        localDataSource.setCurrentLocale(locale)
    }

    override fun getAvailableLocales(): List<LocaleModel> = localDataSource.getAvailableLocales()

    override fun setAvailableLocales(locales: List<LocaleModel>) {
        localDataSource.setAvailableLocales(locales)
    }

    override fun getString(key: String, defaultRes: String?): String {
        return localDataSource.getString(key, defaultRes)
    }

    override fun setCustomStrings(strings: Map<String, String>) {
        localDataSource.setCustomStrings(strings)
    }

    override fun clearCustomStrings() {
        localDataSource.clearCustomStrings()
    }

    override fun set24HourFormat(enabled: Boolean) {
        localDataSource.set24HourFormat(enabled)
    }

    override fun setNameDisplayOrder(order: NameDisplayOrder) {
        localDataSource.setNameDisplayOrder(order)
    }
}
