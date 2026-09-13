package org.telegram.messenger.feature.security.flagsecure

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.telegram.messenger.feature.security.flagsecure.data.datasource.FlagSecureLocalDataSource
import org.telegram.messenger.feature.security.flagsecure.data.repository.FlagSecureRepositoryImpl
import org.telegram.messenger.feature.security.flagsecure.domain.model.SecurityReasonType
import org.telegram.messenger.feature.security.flagsecure.domain.model.SecurityRuleSpec
import org.telegram.messenger.feature.security.flagsecure.domain.model.SecurityRulesEvaluator

class FlagSecureRepositoryImplTest {

    @Test
    fun testInitialWindowStateIsUnsecured() {
        val local = FlagSecureLocalDataSource()
        val repo = FlagSecureRepositoryImpl(local)

        val state = repo.getWindowState("window_main")
        assertFalse(state.isSecured)
        assertEquals(0, state.activeReasonsCount)
        assertTrue(state.activeReasons.isEmpty())
        assertFalse(repo.isWindowSecured("window_main"))
    }

    @Test
    fun testAttachReasonSecuresWindow() {
        val local = FlagSecureLocalDataSource()
        val repo = FlagSecureRepositoryImpl(local)

        val updated = repo.attachReason("window_1", SecurityReasonType.PASSCODE_LOCK)
        assertTrue(updated.isSecured)
        assertEquals(1, updated.activeReasonsCount)
        assertTrue(updated.activeReasons.contains(SecurityReasonType.PASSCODE_LOCK))
        assertTrue(repo.isWindowSecured("window_1"))
    }

    @Test
    fun testAttachReasonWithFalseConditionDoesNotSecure() {
        val local = FlagSecureLocalDataSource()
        val repo = FlagSecureRepositoryImpl(local)

        val updated = repo.attachReason("window_1", SecurityReasonType.SECRET_CHAT, condition = { false })
        assertFalse(updated.isSecured)
        assertEquals(0, updated.activeReasonsCount)
        assertFalse(repo.isWindowSecured("window_1"))
    }

    @Test
    fun testAttachMultipleReasons() {
        val local = FlagSecureLocalDataSource()
        val repo = FlagSecureRepositoryImpl(local)

        repo.attachReason("window_1", SecurityReasonType.PASSCODE_LOCK)
        val updated = repo.attachReason("window_1", SecurityReasonType.PAYMENTS)

        assertTrue(updated.isSecured)
        assertEquals(2, updated.activeReasonsCount)
        assertTrue(updated.activeReasons.contains(SecurityReasonType.PASSCODE_LOCK))
        assertTrue(updated.activeReasons.contains(SecurityReasonType.PAYMENTS))
    }

    @Test
    fun testDetachReason() {
        val local = FlagSecureLocalDataSource()
        val repo = FlagSecureRepositoryImpl(local)

        repo.attachReason("window_1", SecurityReasonType.PASSCODE_LOCK)
        repo.attachReason("window_1", SecurityReasonType.PAYMENTS)

        val afterDetach1 = repo.detachReason("window_1", SecurityReasonType.PASSCODE_LOCK)
        assertTrue(afterDetach1.isSecured)
        assertEquals(1, afterDetach1.activeReasonsCount)
        assertTrue(afterDetach1.activeReasons.contains(SecurityReasonType.PAYMENTS))

        val afterDetach2 = repo.detachReason("window_1", SecurityReasonType.PAYMENTS)
        assertFalse(afterDetach2.isSecured)
        assertEquals(0, afterDetach2.activeReasonsCount)
        assertFalse(repo.isWindowSecured("window_1"))
    }

    @Test
    fun testInvalidateWindowRecomputesDynamicCondition() {
        var dynamicFlag = false
        val local = FlagSecureLocalDataSource()
        val repo = FlagSecureRepositoryImpl(local)

        repo.attachReason("window_1", SecurityReasonType.PROTECTED_CONTENT, condition = { dynamicFlag })
        assertFalse(repo.isWindowSecured("window_1"))

        dynamicFlag = true
        val invalidated = repo.invalidateWindow("window_1")
        assertTrue(invalidated.isSecured)
        assertEquals(1, invalidated.activeReasonsCount)
        assertTrue(repo.isWindowSecured("window_1"))
    }

    @Test
    fun testResetWindow() {
        val local = FlagSecureLocalDataSource()
        val repo = FlagSecureRepositoryImpl(local)

        repo.attachReason("window_1", SecurityReasonType.BIOMETRIC_PROMPT)
        assertTrue(repo.isWindowSecured("window_1"))

        repo.resetWindow("window_1")
        assertFalse(repo.isWindowSecured("window_1"))
        val state = repo.getWindowState("window_1")
        assertFalse(state.isSecured)
        assertEquals(0, state.activeReasonsCount)
    }

    @Test
    fun testObserveWindowStateFlow() = runTest {
        val local = FlagSecureLocalDataSource()
        val repo = FlagSecureRepositoryImpl(local)

        val flow = repo.observeWindowState("window_flow")
        assertEquals(false, flow.first().isSecured)

        repo.attachReason("window_flow", SecurityReasonType.CUSTOM)
        val state = flow.first()
        assertTrue(state.isSecured)
        assertEquals(1, state.activeReasonsCount)
    }

    @Test
    fun testObserveAllWindowStatesFlow() = runTest {
        val local = FlagSecureLocalDataSource()
        val repo = FlagSecureRepositoryImpl(local)

        repo.attachReason("win_a", SecurityReasonType.PASSCODE_LOCK)
        repo.attachReason("win_b", SecurityReasonType.PAYMENTS)

        val allStates = repo.observeAllWindowStates().first()
        assertTrue(allStates.containsKey("win_a"))
        assertTrue(allStates.containsKey("win_b"))
        assertTrue(allStates["win_a"]?.isSecured == true)
        assertTrue(allStates["win_b"]?.isSecured == true)
    }

    @Test
    fun testSecurityRulesEvaluator() {
        val specPasscode = SecurityRuleSpec(hasPasscode = true, allowScreenCapture = false)
        val resultPasscode = SecurityRulesEvaluator.evaluate(specPasscode)
        assertTrue(resultPasscode.shouldSecure)
        assertEquals(listOf(SecurityReasonType.PASSCODE_LOCK), resultPasscode.matchedReasons)

        val specAllowed = SecurityRuleSpec(hasPasscode = true, allowScreenCapture = true)
        val resultAllowed = SecurityRulesEvaluator.evaluate(specAllowed)
        assertFalse(resultAllowed.shouldSecure)

        val specMulti = SecurityRuleSpec(
            hasPasscode = false,
            isSecretChat = true,
            isProtectedPeer = true,
            hasSelfDestructMedia = true,
            isPaymentScreen = true
        )
        val resultMulti = SecurityRulesEvaluator.evaluate(specMulti)
        assertTrue(resultMulti.shouldSecure)
        assertEquals(4, resultMulti.matchedReasons.size)
        assertTrue(resultMulti.matchedReasons.contains(SecurityReasonType.SECRET_CHAT))
        assertTrue(resultMulti.matchedReasons.contains(SecurityReasonType.PROTECTED_CONTENT))
        assertTrue(resultMulti.matchedReasons.contains(SecurityReasonType.SELF_DESTRUCT_MEDIA))
        assertTrue(resultMulti.matchedReasons.contains(SecurityReasonType.PAYMENTS))
    }

    @Test
    fun testCallbackInvokedOnStateChange() {
        var callbackWindowId: String? = null
        var callbackSecured: Boolean? = null

        val local = FlagSecureLocalDataSource(onWindowStateChanged = { winId, isSecured ->
            callbackWindowId = winId
            callbackSecured = isSecured
        })
        val repo = FlagSecureRepositoryImpl(local)

        repo.attachReason("win_cb", SecurityReasonType.PASSCODE_LOCK)
        assertEquals("win_cb", callbackWindowId)
        assertEquals(true, callbackSecured)

        repo.detachReason("win_cb", SecurityReasonType.PASSCODE_LOCK)
        assertEquals("win_cb", callbackWindowId)
        assertEquals(false, callbackSecured)
    }
}
