package org.telegram.messenger.feature.system.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.settings.data.datasource.SettingsLocalDataSource
import org.telegram.messenger.feature.system.settings.data.datasource.SettingsRemoteDataSource
import org.telegram.messenger.feature.system.settings.data.repository.SettingsRepositoryImpl
import org.telegram.messenger.feature.system.settings.domain.model.SettingsModel

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var localDataSource: SettingsLocalDataSource
    private lateinit var remoteDataSource: SettingsRemoteDataSource
    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        localDataSource = SettingsLocalDataSource(currentAccount = 0, mainDispatcher = testDispatcher)
        localDataSource.setTestMode(SettingsModel())
        remoteDataSource = SettingsRemoteDataSource(currentAccount = 0)
        repository = SettingsRepositoryImpl(localDataSource, remoteDataSource)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGetAndObserveSettings() = runTest {
        val initial = repository.getSettings()
        assertEquals(16, initial.fontSize)
        assertEquals(17, initial.bubbleRadius)

        val observed = repository.observeSettings().first()
        assertEquals(16, observed.fontSize)
    }

    @Test
    fun testUpdateSettings() = runTest {
        val r1 = repository.updateFontSize(20)
        assertTrue(r1 is Result.Success)
        assertEquals(20, repository.getSettings().fontSize)

        val r2 = repository.updateBubbleRadius(10)
        assertTrue(r2 is Result.Success)
        assertEquals(10, repository.getSettings().bubbleRadius)

        val r3 = repository.updateSaveToGallery(true)
        assertTrue(r3 is Result.Success)
        assertTrue(repository.getSettings().saveToGallery)

        val r4 = repository.updateStreamMedia(false)
        assertTrue(r4 is Result.Success)
        assertFalse(repository.getSettings().streamMedia)

        val r5 = repository.updateSuggestStickers(false)
        assertTrue(r5 is Result.Success)
        assertFalse(repository.getSettings().suggestStickers)

        val r6 = repository.updateInappCamera(false)
        assertTrue(r6 is Result.Success)
        assertFalse(repository.getSettings().inappCamera)

        val r7 = repository.updateDistanceSystemType(1)
        assertTrue(r7 is Result.Success)
        assertEquals(1, repository.getSettings().distanceSystemType)

        val r8 = repository.updateSyncContacts(false)
        assertTrue(r8 is Result.Success)
        assertFalse(repository.getSettings().syncContacts)

        val r9 = repository.updateSuggestContacts(false)
        assertTrue(r9 is Result.Success)
        assertFalse(repository.getSettings().suggestContacts)

        val r10 = repository.updateShowCallsTab(true)
        assertTrue(r10 is Result.Success)
        assertTrue(repository.getSettings().showCallsTab)
    }

    @Test
    fun testRemoteDataSourceFallback() = runTest {
        val res = remoteDataSource.fetchGlobalPrivacySettings()
        assertTrue(res is Result.Success)
    }
}
