package org.telegram.messenger.feature.system.appconfig

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.appconfig.data.datasource.AppConfigLocalDataSource
import org.telegram.messenger.feature.system.appconfig.data.datasource.AppConfigRemoteDataSource
import org.telegram.messenger.feature.system.appconfig.data.repository.AppConfigRepositoryImpl
import org.telegram.messenger.feature.system.appconfig.domain.model.AppGlobalConfigState

@OptIn(ExperimentalCoroutinesApi::class)
class AppConfigRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var localDataSource: AppConfigLocalDataSource
    private lateinit var remoteDataSource: AppConfigRemoteDataSource
    private lateinit var repository: AppConfigRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = AppConfigLocalDataSource(
            currentAccount = 0,
            ioDispatcher = testDispatcher,
            testMode = true
        )
        remoteDataSource = AppConfigRemoteDataSource(currentAccount = 0)
        repository = AppConfigRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun testGetConfigReturnsDefaultState() {
        val config = repository.getConfig()
        assertNotNull(config)
        assertEquals(10, config.stars.paidMessagesDefault)
        assertEquals(850, config.stars.suggestedPostCommissionPermille)
        assertEquals(4096, config.limits.messageLengthLimitDefault)
        assertEquals(8192, config.limits.messageLengthLimitPremium)
    }

    @Test
    fun testObserveConfigEmitsInitialState() = runTest(testDispatcher) {
        val observed = repository.observeConfig().first()
        assertNotNull(observed)
        assertEquals(10, observed.stars.paidMessagesDefault)
    }

    @Test
    fun testUpdateConfigValueSucceedsAndUpdatesState() = runTest(testDispatcher) {
        val result = repository.updateConfigValue("custom_feature_flag", true)
        assertTrue(result.isSuccess)

        val updated = repository.getConfig()
        assertEquals(true, updated.customEntries["custom_feature_flag"])
    }

    @Test
    fun testUpdateMultipleConfigValues() = runTest(testDispatcher) {
        repository.updateConfigValue("key1", "value1")
        repository.updateConfigValue("key2", 42)

        val updated = repository.getConfig()
        assertEquals("value1", updated.customEntries["key1"])
        assertEquals(42, updated.customEntries["key2"])
    }

    @Test
    fun testReloadConfigReturnsCurrentStateInTestMode() = runTest(testDispatcher) {
        val result = repository.reloadConfig()
        assertTrue(result.isSuccess)
        val state = result.getOrNull()
        assertNotNull(state)
        assertEquals(10, state!!.stars.paidMessagesDefault)
    }

    @Test
    fun testUpdateInMemoryStateReflectedInRepository() {
        val customState = AppGlobalConfigState(
            phoneCountryIso2 = "fr",
            settingsDisplayPasskeys = true
        )
        localDataSource.updateInMemoryState(customState)

        val retrieved = repository.getConfig()
        assertEquals("fr", retrieved.phoneCountryIso2)
        assertTrue(retrieved.settingsDisplayPasskeys)
    }

    @Test
    fun testRemoteDataSourceReturnsResult() = runTest(testDispatcher) {
        val result = remoteDataSource.fetchRemoteConfig()
        assertTrue(result.isSuccess)
    }
}
