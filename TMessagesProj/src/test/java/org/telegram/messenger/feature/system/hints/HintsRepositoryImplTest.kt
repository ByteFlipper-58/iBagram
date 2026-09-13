package org.telegram.messenger.feature.system.hints

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
import org.telegram.messenger.feature.system.hints.data.datasource.HintsLocalDataSource
import org.telegram.messenger.feature.system.hints.data.datasource.HintsRemoteDataSource
import org.telegram.messenger.feature.system.hints.data.repository.HintsRepositoryImpl
import org.telegram.messenger.feature.system.hints.domain.model.HintType
import org.telegram.ui.Components.HintsController

@OptIn(ExperimentalCoroutinesApi::class)
class HintsRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var localDataSource: HintsLocalDataSource
    private lateinit var remoteDataSource: HintsRemoteDataSource
    private lateinit var repository: HintsRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        localDataSource = HintsLocalDataSource(0)
        remoteDataSource = HintsRemoteDataSource(0)
        repository = HintsRepositoryImpl(
            account = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            mainDispatcher = testDispatcher,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        val state = repository.getHintsState()
        assertNotNull(state)
        assertEquals(HintType.entries.size, state.hints.size)
    }

    @Test
    fun testGetHint() {
        val hint = repository.getHint(HintType.ROUND_HINT_2)
        assertEquals(HintType.ROUND_HINT_2, hint.type)
        assertEquals(0, hint.showsCount)
        assertEquals(3, hint.showsLimit)
    }

    @Test
    fun testIncrementHint() = runTest {
        repository.incrementHint(HintType.CHANNEL_SUGGEST_HINT)
        val hint = repository.getHint(HintType.CHANNEL_SUGGEST_HINT)
        assertEquals(1, hint.showsCount)

        val stateHint = repository.getHintsState().hints[HintType.CHANNEL_SUGGEST_HINT]
        assertEquals(1, stateHint?.showsCount)
    }

    @Test
    fun testDoNotShowAgain() = runTest {
        repository.doNotShowAgain(HintType.ACCOUNT_SWITCH_HINT)
        val hint = repository.getHint(HintType.ACCOUNT_SWITCH_HINT)
        assertEquals(HintType.ACCOUNT_SWITCH_HINT.showsLimit, hint.showsCount)
        assertFalse(repository.shouldShowHint(HintType.ACCOUNT_SWITCH_HINT))
    }

    @Test
    fun testResetHint() = runTest {
        repository.incrementHint(HintType.GIFT_MESSAGE_HINT)
        assertEquals(1, repository.getHint(HintType.GIFT_MESSAGE_HINT).showsCount)

        repository.resetHint(HintType.GIFT_MESSAGE_HINT)
        assertEquals(0, repository.getHint(HintType.GIFT_MESSAGE_HINT).showsCount)
    }

    @Test
    fun testResetAllHints() = runTest {
        repository.incrementHint(HintType.ROUND_HINT_2)
        repository.incrementHint(HintType.CHANNEL_GIFT_HINT)

        repository.resetAllHints()

        assertEquals(0, repository.getHint(HintType.ROUND_HINT_2).showsCount)
        assertEquals(0, repository.getHint(HintType.CHANNEL_GIFT_HINT).showsCount)
    }

    @Test
    fun testShouldShowHint() {
        repository.resetHint(HintType.GROUP_EMOJI_PACK_HINT_SHOWN)
        // Group emoji pack hint has limit 1, probability 1.0f
        assertTrue(repository.shouldShowHint(HintType.GROUP_EMOJI_PACK_HINT_SHOWN))

        repository.incrementHint(HintType.GROUP_EMOJI_PACK_HINT_SHOWN)
        assertFalse(repository.shouldShowHint(HintType.GROUP_EMOJI_PACK_HINT_SHOWN))
    }

    @Test
    fun testStranglerHooks() {
        val repoFromController = HintsController.getHintsRepository(0)
        assertNotNull(repoFromController)
    }
}
