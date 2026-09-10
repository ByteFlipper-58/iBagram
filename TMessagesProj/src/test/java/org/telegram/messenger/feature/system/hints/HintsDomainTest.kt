package org.telegram.messenger.feature.system.hints

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.feature.system.hints.data.mapper.HintMapper
import org.telegram.messenger.feature.system.hints.domain.model.HintModel
import org.telegram.messenger.feature.system.hints.domain.model.HintType
import org.telegram.messenger.feature.system.hints.domain.model.HintsStateModel
import org.telegram.messenger.feature.system.hints.domain.repository.HintsRepository
import org.telegram.messenger.feature.system.hints.domain.usecase.DoNotShowAgainHintUseCase
import org.telegram.messenger.feature.system.hints.domain.usecase.GetHintUseCase
import org.telegram.messenger.feature.system.hints.domain.usecase.GetHintsStateUseCase
import org.telegram.messenger.feature.system.hints.domain.usecase.IncrementHintUseCase
import org.telegram.messenger.feature.system.hints.domain.usecase.ObserveHintsUseCase
import org.telegram.messenger.feature.system.hints.domain.usecase.ResetAllHintsUseCase
import org.telegram.messenger.feature.system.hints.domain.usecase.ResetHintUseCase
import org.telegram.messenger.feature.system.hints.domain.usecase.ShouldShowHintUseCase
import org.telegram.messenger.feature.system.hints.presentation.HintsEvent
import org.telegram.messenger.feature.system.hints.presentation.HintsViewModel
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalCoroutinesApi::class)
class HintsDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- 1. Domain Model Tests ---

    @Test
    fun `HintType contains expected keys and limits`() {
        assertEquals("needShowRoundHint2", HintType.ROUND_HINT_2.preferenceKey)
        assertEquals(3, HintType.ROUND_HINT_2.showsLimit)
        assertEquals(0.2f, HintType.ROUND_HINT_2.probability, 0.001f)

        assertEquals("groupEmojiPackShownHint", HintType.GROUP_EMOJI_PACK_HINT_SHOWN.preferenceKey)
        assertEquals(1, HintType.GROUP_EMOJI_PACK_HINT_SHOWN.showsLimit)
        assertEquals(1.0f, HintType.GROUP_EMOJI_PACK_HINT_SHOWN.probability, 0.001f)

        assertEquals("accountswitchhint", HintType.ACCOUNT_SWITCH_HINT.preferenceKey)
        assertEquals(3, HintType.ACCOUNT_SWITCH_HINT.showsLimit)

        assertEquals(HintType.GUEST_BOT_PRIVACY, HintType.fromKey("hints_controller_GuestBotPrivacy"))
        assertNull(HintType.fromKey("unknown_key"))
    }

    @Test
    fun `HintModel canShow logic follows limit and probability`() {
        val modelDeterministic = HintModel(
            type = HintType.ACCOUNT_SWITCH_HINT,
            showsCount = 0,
            showsLimit = 3,
            probability = 1.0f
        )
        assertFalse(modelDeterministic.isLimitReached)
        assertTrue(modelDeterministic.canShow(0.5f))

        val modelLimitReached = modelDeterministic.copy(showsCount = 3)
        assertTrue(modelLimitReached.isLimitReached)
        assertFalse(modelLimitReached.canShow(0.1f))

        val modelProbabilistic = HintModel(
            type = HintType.ROUND_HINT_2,
            showsCount = 1,
            showsLimit = 3,
            probability = 0.2f
        )
        assertTrue(modelProbabilistic.canShow(0.15f)) // 0.15 < 0.2
        assertFalse(modelProbabilistic.canShow(0.25f)) // 0.25 >= 0.2
    }

    // --- 2. Mapper Tests ---

    @Test
    fun `HintMapper maps domain model correctly`() {
        val domainModel = HintMapper.toDomainModel(HintType.CHANNEL_GIFT_HINT, 2)
        assertEquals(HintType.CHANNEL_GIFT_HINT, domainModel.type)
        assertEquals(2, domainModel.showsCount)
        assertEquals(3, domainModel.showsLimit)
        assertEquals(0.2f, domainModel.probability, 0.001f)
        assertFalse(domainModel.isLimitReached)
    }

    // --- 3. Use Cases & Fake Repository Tests ---

    private class FakeHintsRepository : HintsRepository {
        private val counts = ConcurrentHashMap<HintType, Int>()
        private val _flow = MutableStateFlow(buildState())

        private fun buildState(): HintsStateModel {
            val map = HintType.entries.associateWith { type ->
                HintModel(
                    type = type,
                    showsCount = counts[type] ?: 0,
                    showsLimit = type.showsLimit,
                    probability = type.probability
                )
            }
            return HintsStateModel(hints = map)
        }

        override fun observeHints(): Flow<HintsStateModel> = _flow

        override fun getHintsState(): HintsStateModel = _flow.value

        override fun getHint(type: HintType): HintModel {
            return _flow.value.getHint(type)
        }

        override fun shouldShowHint(type: HintType): Boolean {
            val model = getHint(type)
            return model.canShow(0.05f)
        }

        override fun incrementHint(type: HintType) {
            val current = counts[type] ?: 0
            counts[type] = current + 1
            _flow.value = buildState()
        }

        override fun doNotShowAgain(type: HintType) {
            counts[type] = type.showsLimit
            _flow.value = buildState()
        }

        override fun resetHint(type: HintType) {
            counts.remove(type)
            _flow.value = buildState()
        }

        override fun resetAllHints() {
            counts.clear()
            _flow.value = buildState()
        }
    }

    @Test
    fun `Use cases perform hint inspection, increment, dismiss, and reset`() = runTest {
        val fakeRepo = FakeHintsRepository()
        val observeHints = ObserveHintsUseCase(fakeRepo)
        val getHintsState = GetHintsStateUseCase(fakeRepo)
        val getHint = GetHintUseCase(fakeRepo)
        val shouldShowHint = ShouldShowHintUseCase(fakeRepo)
        val incrementHint = IncrementHintUseCase(fakeRepo)
        val doNotShowAgain = DoNotShowAgainHintUseCase(fakeRepo)
        val resetHint = ResetHintUseCase(fakeRepo)
        val resetAll = ResetAllHintsUseCase(fakeRepo)

        // Initial state
        val initialState = getHintsState()
        assertEquals(8, initialState.hints.size)
        assertTrue(shouldShowHint(HintType.ACCOUNT_SWITCH_HINT))

        // Increment
        incrementHint(HintType.ACCOUNT_SWITCH_HINT)
        assertEquals(1, getHint(HintType.ACCOUNT_SWITCH_HINT).showsCount)

        // Do not show again
        doNotShowAgain(HintType.ACCOUNT_SWITCH_HINT)
        assertEquals(3, getHint(HintType.ACCOUNT_SWITCH_HINT).showsCount)
        assertTrue(getHint(HintType.ACCOUNT_SWITCH_HINT).isLimitReached)
        assertFalse(shouldShowHint(HintType.ACCOUNT_SWITCH_HINT))

        // Reset individual
        resetHint(HintType.ACCOUNT_SWITCH_HINT)
        assertEquals(0, getHint(HintType.ACCOUNT_SWITCH_HINT).showsCount)

        // Set counts and reset all
        incrementHint(HintType.ROUND_HINT_2)
        incrementHint(HintType.GIFT_MESSAGE_HINT)
        resetAll()
        assertEquals(0, getHint(HintType.ROUND_HINT_2).showsCount)
        assertEquals(0, getHint(HintType.GIFT_MESSAGE_HINT).showsCount)

        val finalFlow = observeHints().first()
        assertEquals(0, finalFlow.getHint(HintType.ROUND_HINT_2).showsCount)
    }

    // --- 4. Presentation ViewModel MVI Tests ---

    @Test
    fun `HintsViewModel handles MVI events correctly`() = runTest {
        val fakeRepo = FakeHintsRepository()
        val viewModel = HintsViewModel(
            observeHintsUseCase = ObserveHintsUseCase(fakeRepo),
            getHintsStateUseCase = GetHintsStateUseCase(fakeRepo),
            shouldShowHintUseCase = ShouldShowHintUseCase(fakeRepo),
            incrementHintUseCase = IncrementHintUseCase(fakeRepo),
            doNotShowAgainHintUseCase = DoNotShowAgainHintUseCase(fakeRepo),
            resetHintUseCase = ResetHintUseCase(fakeRepo),
            resetAllHintsUseCase = ResetAllHintsUseCase(fakeRepo)
        )

        advanceUntilIdle()
        assertEquals(8, viewModel.uiState.value.hintsState.hints.size)

        // CheckShouldShow event
        viewModel.onEvent(HintsEvent.CheckShouldShow(HintType.ACCOUNT_SWITCH_HINT))
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.lastShownDecision)
        assertEquals(HintType.ACCOUNT_SWITCH_HINT, viewModel.uiState.value.lastShownDecision?.first)
        assertTrue(viewModel.uiState.value.lastShownDecision?.second == true)

        // Increment event
        viewModel.onEvent(HintsEvent.Increment(HintType.ACCOUNT_SWITCH_HINT))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.hintsState.getHint(HintType.ACCOUNT_SWITCH_HINT).showsCount)

        // DoNotShowAgain event
        viewModel.onEvent(HintsEvent.DoNotShowAgain(HintType.ACCOUNT_SWITCH_HINT))
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.hintsState.getHint(HintType.ACCOUNT_SWITCH_HINT).showsCount)
        assertNotNull(viewModel.uiState.value.infoMessage)

        // DismissInfo event
        viewModel.onEvent(HintsEvent.DismissInfo)
        assertNull(viewModel.uiState.value.infoMessage)

        // Reset event
        viewModel.onEvent(HintsEvent.Reset(HintType.ACCOUNT_SWITCH_HINT))
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.hintsState.getHint(HintType.ACCOUNT_SWITCH_HINT).showsCount)

        // ResetAll event
        viewModel.onEvent(HintsEvent.Increment(HintType.ROUND_HINT_2))
        viewModel.onEvent(HintsEvent.ResetAll)
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.hintsState.getHint(HintType.ROUND_HINT_2).showsCount)
    }

    // --- 5. DI Container Test ---

    @Test
    fun `AccountFeatureContainer resolves hints dependencies`() {
        val container = AccountFeatureContainer.get(0)
        val fakeRepo = FakeHintsRepository()
        container.hintsRepository = fakeRepo

        assertNotNull(container.hintsRepository)
        assertNotNull(container.observeHintsUseCase)
        assertNotNull(container.getHintsStateUseCase)
        assertNotNull(container.getHintUseCase)
        assertNotNull(container.shouldShowHintUseCase)
        assertNotNull(container.incrementHintUseCase)
        assertNotNull(container.doNotShowAgainHintUseCase)
        assertNotNull(container.resetHintUseCase)
        assertNotNull(container.resetAllHintsUseCase)

        val vm = container.createHintsViewModel()
        assertNotNull(vm)
    }
}
