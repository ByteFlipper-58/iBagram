package org.telegram.messenger.feature.security.flagsecure

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.telegram.messenger.feature.security.flagsecure.data.mapper.FlagSecureMapper
import org.telegram.messenger.feature.security.flagsecure.data.repository.LegacyFlagSecureRepository
import org.telegram.messenger.feature.security.flagsecure.domain.model.SecurityReasonType
import org.telegram.messenger.feature.security.flagsecure.domain.model.SecurityRuleSpec
import org.telegram.messenger.feature.security.flagsecure.domain.model.SecurityRulesEvaluator
import org.telegram.messenger.feature.security.flagsecure.domain.model.WindowSecurityState
import org.telegram.messenger.feature.security.flagsecure.domain.usecase.AttachSecurityReasonUseCase
import org.telegram.messenger.feature.security.flagsecure.domain.usecase.DetachSecurityReasonUseCase
import org.telegram.messenger.feature.security.flagsecure.domain.usecase.GetWindowSecurityStateUseCase
import org.telegram.messenger.feature.security.flagsecure.domain.usecase.InvalidateWindowSecurityUseCase
import org.telegram.messenger.feature.security.flagsecure.domain.usecase.ObserveAllWindowStatesUseCase
import org.telegram.messenger.feature.security.flagsecure.domain.usecase.ObserveWindowStateUseCase
import org.telegram.messenger.feature.security.flagsecure.domain.usecase.ResetWindowSecurityUseCase
import org.telegram.messenger.feature.security.flagsecure.presentation.FlagSecureEvent
import org.telegram.messenger.feature.security.flagsecure.presentation.FlagSecureViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class FlagSecureDomainTest {

    @Test
    fun testSecurityRulesEvaluator() {
        val cleanSpec = SecurityRuleSpec(
            hasPasscode = false,
            allowScreenCapture = true,
            isSecretChat = false,
            isProtectedPeer = false
        )
        val cleanResult = SecurityRulesEvaluator.evaluate(cleanSpec)
        assertFalse(cleanResult.shouldSecure)
        assertTrue(cleanResult.matchedReasons.isEmpty())

        val passcodeSpec = SecurityRuleSpec(
            hasPasscode = true,
            allowScreenCapture = false
        )
        val passcodeResult = SecurityRulesEvaluator.evaluate(passcodeSpec)
        assertTrue(passcodeResult.shouldSecure)
        assertEquals(listOf(SecurityReasonType.PASSCODE_LOCK), passcodeResult.matchedReasons)

        val fullProtectedSpec = SecurityRuleSpec(
            isSecretChat = true,
            isProtectedPeer = true,
            hasSelfDestructMedia = true,
            isPaymentScreen = true
        )
        val fullResult = SecurityRulesEvaluator.evaluate(fullProtectedSpec)
        assertTrue(fullResult.shouldSecure)
        assertEquals(4, fullResult.matchedReasons.size)
        assertTrue(fullResult.matchedReasons.contains(SecurityReasonType.SECRET_CHAT))
        assertTrue(fullResult.matchedReasons.contains(SecurityReasonType.PROTECTED_CONTENT))
        assertTrue(fullResult.matchedReasons.contains(SecurityReasonType.SELF_DESTRUCT_MEDIA))
        assertTrue(fullResult.matchedReasons.contains(SecurityReasonType.PAYMENTS))
    }

    @Test
    fun testCalculateNewStateMath() {
        val initial = WindowSecurityState(windowId = "w1")
        val state1 = SecurityRulesEvaluator.calculateNewState(
            windowId = "w1",
            currentReasons = emptyList(),
            reasonToAdd = SecurityReasonType.SECRET_CHAT
        )
        assertTrue(state1.isSecured)
        assertEquals(1, state1.activeReasonsCount)
        assertEquals(listOf(SecurityReasonType.SECRET_CHAT), state1.activeReasons)

        val state2 = SecurityRulesEvaluator.calculateNewState(
            windowId = "w1",
            currentReasons = state1.activeReasons,
            reasonToAdd = SecurityReasonType.PASSCODE_LOCK
        )
        assertTrue(state2.isSecured)
        assertEquals(2, state2.activeReasonsCount)

        val state3 = SecurityRulesEvaluator.calculateNewState(
            windowId = "w1",
            currentReasons = state2.activeReasons,
            reasonToRemove = SecurityReasonType.SECRET_CHAT
        )
        assertTrue(state3.isSecured)
        assertEquals(1, state3.activeReasonsCount)
        assertEquals(listOf(SecurityReasonType.PASSCODE_LOCK), state3.activeReasons)

        val state4 = SecurityRulesEvaluator.calculateNewState(
            windowId = "w1",
            currentReasons = state3.activeReasons,
            reasonToRemove = SecurityReasonType.PASSCODE_LOCK
        )
        assertFalse(state4.isSecured)
        assertEquals(0, state4.activeReasonsCount)
    }

    @Test
    fun testMapperFormatting() {
        val title = FlagSecureMapper.mapReasonTitle(SecurityReasonType.SECRET_CHAT)
        assertTrue(title.contains("Секретный чат"))

        val state = WindowSecurityState(
            windowId = "main",
            isSecured = true,
            activeReasonsCount = 1,
            activeReasons = listOf(SecurityReasonType.PASSCODE_LOCK)
        )
        val summary = FlagSecureMapper.formatWindowStateSummary(state)
        assertTrue(summary.contains("SECURED"))
        assertTrue(summary.contains("PASSCODE_LOCK"))
    }

    @Test
    fun testRepositoryAttachDetachAndInvalidate() {
        val transitions = mutableListOf<Pair<String, Boolean>>()
        val repo = LegacyFlagSecureRepository(
            onWindowStateChanged = { winId, secured ->
                transitions.add(winId to secured)
            }
        )

        assertFalse(repo.isWindowSecured("w1"))

        var conditionActive = true
        val state1 = repo.attachReason("w1", SecurityReasonType.SECRET_CHAT) { conditionActive }
        assertTrue(state1.isSecured)
        assertEquals(1, state1.activeReasonsCount)
        assertEquals(listOf("w1" to true), transitions)

        // Invalidating when condition becomes false
        conditionActive = false
        val state2 = repo.invalidateWindow("w1")
        assertFalse(state2.isSecured)
        assertEquals(0, state2.activeReasonsCount)
        assertEquals(listOf("w1" to true, "w1" to false), transitions)

        // Re-enabling condition and invalidating
        conditionActive = true
        val state3 = repo.invalidateWindow("w1")
        assertTrue(state3.isSecured)
        assertEquals(listOf("w1" to true, "w1" to false, "w1" to true), transitions)

        // Detaching reason
        val state4 = repo.detachReason("w1", SecurityReasonType.SECRET_CHAT)
        assertFalse(state4.isSecured)
        assertEquals(listOf("w1" to true, "w1" to false, "w1" to true, "w1" to false), transitions)
    }

    @Test
    fun testMultiWindowTrackingAndReset() {
        val repo = LegacyFlagSecureRepository()

        repo.attachReason("window_chat", SecurityReasonType.SECRET_CHAT)
        repo.attachReason("window_passcode", SecurityReasonType.PASSCODE_LOCK)

        assertTrue(repo.isWindowSecured("window_chat"))
        assertTrue(repo.isWindowSecured("window_passcode"))
        assertFalse(repo.isWindowSecured("window_settings"))

        val allStates = repo.getAllWindowStates()
        assertEquals(2, allStates.size)
        assertTrue(allStates.containsKey("window_chat"))
        assertTrue(allStates.containsKey("window_passcode"))

        repo.resetWindow("window_chat")
        assertFalse(repo.isWindowSecured("window_chat"))
        assertEquals(1, repo.getAllWindowStates().size)
    }

    @Test
    fun testFlagSecureViewModelMviFlow() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)

        val repo = LegacyFlagSecureRepository()

        val viewModel = FlagSecureViewModel(
            initialWindowId = "main",
            attachSecurityReasonUseCase = AttachSecurityReasonUseCase(repo),
            detachSecurityReasonUseCase = DetachSecurityReasonUseCase(repo),
            invalidateWindowSecurityUseCase = InvalidateWindowSecurityUseCase(repo),
            getWindowSecurityStateUseCase = GetWindowSecurityStateUseCase(repo),
            resetWindowSecurityUseCase = ResetWindowSecurityUseCase(repo),
            observeWindowStateUseCase = ObserveWindowStateUseCase(repo),
            observeAllWindowStatesUseCase = ObserveAllWindowStatesUseCase(repo),
            scope = scope
        )

        advanceUntilIdle()

        assertEquals("main", viewModel.uiState.value.currentWindowId)
        assertFalse(viewModel.uiState.value.isSecured)

        viewModel.onEvent(FlagSecureEvent.AttachReason("main", SecurityReasonType.PASSCODE_LOCK))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSecured)
        assertEquals(1, viewModel.uiState.value.activeReasonsCount)
        assertTrue(viewModel.uiState.value.activeReasons.contains(SecurityReasonType.PASSCODE_LOCK))

        viewModel.onEvent(FlagSecureEvent.DetachReason("main", SecurityReasonType.PASSCODE_LOCK))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSecured)
        assertEquals(0, viewModel.uiState.value.activeReasonsCount)

        viewModel.onCleared()
    }
}
