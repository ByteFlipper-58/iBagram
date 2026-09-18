package org.telegram.messenger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TranscribeAudioControllerTest {

    private lateinit var controller: TranscribeAudioController

    @Before
    fun setUp() {
        controller = TranscribeAudioController.getInstance(0)
        controller.cleanup()
    }

    @Test
    fun testSingletonPerAccount() {
        val controller0 = TranscribeAudioController.getInstance(0)
        val controller1 = TranscribeAudioController.getInstance(1)
        val controller0Again = TranscribeAudioController.getInstance(0)

        assertNotNull(controller0)
        assertNotNull(controller1)
        assertEquals(controller0, controller0Again)
    }

    @Test
    fun testButtonInteractionThreshold() {
        assertEquals(0, controller.transcribeButtonPressed)
        assertFalse(controller.didPressTranscribeButtonEnough())

        controller.pressTranscribeButton()
        assertEquals(1, controller.transcribeButtonPressed)
        assertFalse(controller.didPressTranscribeButtonEnough())

        controller.pressTranscribeButton()
        assertEquals(2, controller.transcribeButtonPressed)
        assertTrue(controller.didPressTranscribeButtonEnough())

        // Extra presses should not exceed limit
        controller.pressTranscribeButton()
        assertEquals(2, controller.transcribeButtonPressed)
        assertTrue(controller.didPressTranscribeButtonEnough())
    }

    @Test
    fun testTrialLimitsAndDurationMax() {
        controller.transcribeAudioTrialWeeklyNumber = 5
        controller.transcribeAudioTrialCurrentNumber = 3
        controller.transcribeAudioTrialDurationMax = 300
        controller.transcribeAudioTrialCooldownUntil = 0

        // Valid audio within 300s duration and with remaining trials
        assertTrue(controller.canTranscribeTrial(60, 1000))
        assertTrue(controller.canTranscribeTrial(300, 1000))

        // Exceeds max duration (301 > 300)
        assertFalse(controller.canTranscribeTrial(301, 1000))

        // No weekly quota configured
        controller.transcribeAudioTrialWeeklyNumber = 0
        assertFalse(controller.canTranscribeTrial(60, 1000))
    }

    @Test
    fun testCooldownAndLockState() {
        controller.transcribeAudioTrialWeeklyNumber = 2
        controller.transcribeAudioTrialCurrentNumber = 0
        controller.transcribeAudioTrialCooldownUntil = 2000

        // Non-premium user during active cooldown with 0 current trials -> LOCKED
        assertTrue(controller.isTranscribeTrialLocked(isPremium = false, currentTimeSeconds = 1500))

        // Premium user is never locked
        assertFalse(controller.isTranscribeTrialLocked(isPremium = true, currentTimeSeconds = 1500))

        // After cooldown expires (2500 > 2000) -> NOT LOCKED
        assertFalse(controller.isTranscribeTrialLocked(isPremium = false, currentTimeSeconds = 2500))

        // If user has trials remaining (> 0), not locked even before cooldown
        controller.transcribeAudioTrialCurrentNumber = 1
        assertFalse(controller.isTranscribeTrialLocked(isPremium = false, currentTimeSeconds = 1500))
    }

    @Test
    fun testTrialCountResolution() {
        controller.transcribeAudioTrialWeeklyNumber = 5
        controller.transcribeAudioTrialCurrentNumber = 2
        controller.transcribeAudioTrialCooldownUntil = 2000

        // During active cooldown (currentTime <= cooldownUntil), return current remaining
        assertEquals(2, controller.getTranscribeTrialCount(currentTimeSeconds = 1500))

        // After cooldown expired (currentTime > cooldownUntil), return full weekly allowance
        assertEquals(5, controller.getTranscribeTrialCount(currentTimeSeconds = 2500))

        // When weekly number is 0
        controller.transcribeAudioTrialWeeklyNumber = 0
        assertEquals(0, controller.getTranscribeTrialCount(currentTimeSeconds = 2500))
    }

    @Test
    fun testAppConfigUpdates() {
        controller.transcribeAudioTrialWeeklyNumber = 2
        controller.transcribeAudioTrialCurrentNumber = 1
        controller.transcribeAudioTrialDurationMax = 300
        controller.transcribeAudioTrialCooldownUntil = 0
        controller.groupTranscribeLevelMin = 1

        controller.onAppConfigUpdate(
            weeklyNumber = 5,
            durationMax = 600,
            cooldownUntil = 5000,
            groupLevelMin = 3,
            currentTimeSeconds = 1000
        )

        assertEquals(5, controller.transcribeAudioTrialWeeklyNumber)
        assertEquals(600, controller.transcribeAudioTrialDurationMax)
        assertEquals(5000, controller.transcribeAudioTrialCooldownUntil)
        assertEquals(3, controller.groupTranscribeLevelMin)
    }

    @Test
    fun testCleanup() {
        controller.transcribeButtonPressed = 2
        controller.transcribeAudioTrialWeeklyNumber = 10
        controller.transcribeAudioTrialCurrentNumber = 8
        controller.transcribeAudioTrialDurationMax = 600
        controller.transcribeAudioTrialCooldownUntil = 9999
        controller.groupTranscribeLevelMin = 5

        controller.cleanup()

        assertEquals(0, controller.transcribeButtonPressed)
        assertEquals(300, controller.transcribeAudioTrialDurationMax)
        assertEquals(0, controller.transcribeAudioTrialCooldownUntil)
        assertEquals(1, controller.groupTranscribeLevelMin)
        assertFalse(controller.didPressTranscribeButtonEnough())
    }
}
