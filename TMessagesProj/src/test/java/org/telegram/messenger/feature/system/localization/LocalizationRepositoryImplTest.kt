package org.telegram.messenger.feature.system.localization

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.localization.data.datasource.LocalizationLocalDataSource
import org.telegram.messenger.feature.system.localization.data.datasource.LocalizationRemoteDataSource
import org.telegram.messenger.feature.system.localization.data.repository.LocalizationRepositoryImpl
import org.telegram.messenger.feature.system.localization.domain.model.LocaleModel
import org.telegram.messenger.feature.system.localization.domain.model.NameDisplayOrder

class LocalizationRepositoryImplTest {

    private lateinit var localDataSource: LocalizationLocalDataSource
    private lateinit var remoteDataSource: LocalizationRemoteDataSource
    private lateinit var repository: LocalizationRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = LocalizationLocalDataSource(testMode = true)
        remoteDataSource = LocalizationRemoteDataSource()
        repository = LocalizationRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun testInitialState() {
        val state = repository.getState()
        assertNotNull(state)
        assertEquals("en", repository.getCurrentLocale().code)
        assertTrue(repository.getAvailableLocales().isNotEmpty())
    }

    @Test
    fun testSetCurrentLocale() {
        val newLocale = LocaleModel(code = "ru", nativeName = "Русский", englishName = "Russian", isRtl = false)
        repository.setCurrentLocale(newLocale)
        assertEquals("ru", repository.getCurrentLocale().code)
        assertEquals(false, repository.getState().isRtl)
    }

    @Test
    fun testCustomStrings() {
        assertEquals("test_val", repository.getString("unknown_key", "test_val"))
        repository.setCustomStrings(mapOf("test_key" to "Hello World"))
        assertEquals("Hello World", repository.getString("test_key"))
        repository.clearCustomStrings()
        assertEquals("fallback", repository.getString("test_key", "fallback"))
    }

    @Test
    fun testPreferences() {
        repository.set24HourFormat(true)
        assertTrue(repository.getState().is24HourFormat)

        repository.setNameDisplayOrder(NameDisplayOrder.LAST_FIRST)
        assertEquals(NameDisplayOrder.LAST_FIRST, repository.getState().nameDisplayOrder)
    }

    @Test
    fun testRemoteLocalesFetch() = runBlocking {
        val result = remoteDataSource.fetchRemoteLocales()
        assertTrue(result.isSuccess)
        val locales = result.getOrNull()
        assertNotNull(locales)
        assertTrue(locales!!.any { it.code == "en" })
    }
}
