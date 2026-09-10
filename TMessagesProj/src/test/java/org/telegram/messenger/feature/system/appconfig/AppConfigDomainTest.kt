package org.telegram.messenger.feature.system.appconfig

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.appconfig.data.mapper.AppConfigMapper
import org.telegram.messenger.feature.system.appconfig.data.repository.LegacyAppConfigRepository
import org.telegram.messenger.feature.system.appconfig.domain.model.AppGlobalConfigState
import org.telegram.messenger.feature.system.appconfig.domain.usecase.GetAiComposeConfigUseCase
import org.telegram.messenger.feature.system.appconfig.domain.usecase.GetAppConfigUseCase
import org.telegram.messenger.feature.system.appconfig.domain.usecase.GetAppLimitsUseCase
import org.telegram.messenger.feature.system.appconfig.domain.usecase.GetMessageLimitsUseCase
import org.telegram.messenger.feature.system.appconfig.domain.usecase.GetPollsConfigUseCase
import org.telegram.messenger.feature.system.appconfig.domain.usecase.GetRichMessageLimitsUseCase
import org.telegram.messenger.feature.system.appconfig.domain.usecase.GetStarsPricingConfigUseCase
import org.telegram.messenger.feature.system.appconfig.domain.usecase.GetTonPricingConfigUseCase
import org.telegram.messenger.feature.system.appconfig.domain.usecase.ObserveAppConfigUseCase
import org.telegram.messenger.feature.system.appconfig.domain.usecase.ReloadAppConfigUseCase
import org.telegram.messenger.feature.system.appconfig.domain.usecase.UpdateAppConfigValueUseCase
import org.telegram.messenger.feature.system.appconfig.presentation.AppConfigEvent
import org.telegram.messenger.feature.system.appconfig.presentation.AppConfigViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class AppConfigDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDefaultMessageLimits() {
        val repository = LegacyAppConfigRepository(currentAccount = 0, ioDispatcher = testDispatcher)
        val getMessageLimits = GetMessageLimitsUseCase(repository)

        // Default user limit is 4096 characters
        val defaultLimit = getMessageLimits(isPremium = false)
        assertEquals(4096, defaultLimit)

        // Premium user limit is 8192 characters (2x)
        val premiumLimit = getMessageLimits(isPremium = true)
        assertEquals(8192, premiumLimit)

        val appLimits = GetAppLimitsUseCase(repository)()
        assertEquals(20, appLimits.botsCreateLimitDefault)
        assertEquals(40, appLimits.botsCreateLimitPremium)
        assertEquals(100, appLimits.storiesAlbumsLimit)
        assertEquals(100, appLimits.stargiftsCollectionsLimit)
    }

    @Test
    fun testStarsAndTonPricingDefaults() {
        val repository = LegacyAppConfigRepository(currentAccount = 0, ioDispatcher = testDispatcher)
        val getStarsConfig = GetStarsPricingConfigUseCase(repository)
        val getTonConfig = GetTonPricingConfigUseCase(repository)

        val stars = getStarsConfig()
        assertEquals(10, stars.paidMessagesDefault)
        assertEquals(850, stars.suggestedPostCommissionPermille)
        assertEquals(5, stars.suggestedPostMin)
        assertEquals(100_000, stars.suggestedPostMax)
        assertEquals(800, stars.resaleCommissionPermille)
        assertEquals(125, stars.resaleMin)
        assertEquals(35_000, stars.resaleMax)
        assertTrue(stars.ratingLearnMoreUrl.startsWith("https://"))
        assertFalse(stars.topUpInvoiceDisabled)
        assertTrue(stars.giftsEnabled)

        val ton = getTonConfig()
        assertEquals(850, ton.suggestedPostCommissionPermille)
        assertEquals(10_000_000L, ton.suggestedPostAmountMinNano)
        assertEquals(10_000_000_000_000L, ton.suggestedPostAmountMaxNano)
        assertEquals(3.0, ton.usdRate, 0.001)
        assertEquals(800, ton.resaleCommissionPermille)
    }

    @Test
    fun testRichMessageAndPollsConfigLimits() {
        val repository = LegacyAppConfigRepository(currentAccount = 0, ioDispatcher = testDispatcher)
        val getRichMessageLimits = GetRichMessageLimitsUseCase(repository)
        val getPollsConfig = GetPollsConfigUseCase(repository)
        val getAiCompose = GetAiComposeConfigUseCase(repository)

        val rich = getRichMessageLimits()
        assertEquals(32_768, rich.lengthLimit)
        assertEquals(500, rich.maxBlocks)
        assertEquals(16, rich.maxDepth)
        assertEquals(50, rich.maxMedia)
        assertEquals(20, rich.maxTableCols)
        assertEquals("premium", rich.posting)

        val polls = getPollsConfig()
        assertEquals(12, polls.answersMax)
        assertEquals(12, polls.countriesMax)
        assertEquals(100, polls.answerLengthMax)
        assertEquals(255, polls.questionLengthMax)
        assertEquals(200, polls.solutionLengthMax)
        assertEquals(300, polls.captionLengthMax)
        assertEquals(86400L * 30L, polls.closePeriodMaxSeconds)
        assertEquals(300L, polls.answerDeletePeriodSeconds)

        val ai = getAiCompose()
        assertEquals(3, ai.toneExamplesNum)
        assertEquals(12, ai.toneTitleLengthMax)
        assertEquals(1024, ai.tonePromptLengthMax)
        assertEquals(5, ai.toneSavedLimitDefault)
        assertEquals(20, ai.toneSavedLimitPremium)
    }

    @Test
    fun testLegacyAppConfigRepositoryStateFlowAndCustomUpdates() = runTest(testDispatcher) {
        val repository = LegacyAppConfigRepository(currentAccount = 0, ioDispatcher = testDispatcher)
        val initialConfig = repository.getConfig()
        assertNotNull(initialConfig)
        assertEquals("en", initialConfig.phoneCountryIso2)

        // Test updating custom config value
        val updateResult = repository.updateConfigValue("custom_feature_flag", true)
        assertTrue(updateResult.isSuccess)

        val updatedConfig = repository.getConfig()
        assertEquals(true, updatedConfig.customEntries["custom_feature_flag"])

        // Test reload config
        val reloadResult = repository.reloadConfig()
        assertTrue(reloadResult.isSuccess)
    }

    @Test
    fun testAppConfigViewModelMviFlow() = runTest(testDispatcher) {
        val repository = LegacyAppConfigRepository(currentAccount = 0, ioDispatcher = testDispatcher)
        val observeUseCase = ObserveAppConfigUseCase(repository)
        val reloadUseCase = ReloadAppConfigUseCase(repository)
        val updateUseCase = UpdateAppConfigValueUseCase(repository)

        val viewModel = AppConfigViewModel(
            observeAppConfig = observeUseCase,
            reloadAppConfig = reloadUseCase,
            updateAppConfigValue = updateUseCase,
            repository = repository
        )

        advanceUntilIdle()

        // Initial state
        val initialState = viewModel.uiState.value
        assertFalse(initialState.isLoading)
        assertEquals(4096, initialState.config.limits.messageLengthLimitDefault)

        // Event: UpdateKey
        viewModel.onEvent(AppConfigEvent.UpdateKey("client_cache_ttl_sec", 3600))
        advanceUntilIdle()

        val updatedState = viewModel.uiState.value
        assertEquals(3600, updatedState.config.customEntries["client_cache_ttl_sec"])

        // Event: Refresh
        viewModel.onEvent(AppConfigEvent.Refresh)
        advanceUntilIdle()

        val refreshedState = viewModel.uiState.value
        assertFalse(refreshedState.isLoading)
        assertTrue(refreshedState.lastUpdatedTimestamp > 0L)
    }
}
