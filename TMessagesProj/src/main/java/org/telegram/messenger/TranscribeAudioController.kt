package org.telegram.messenger

import android.content.SharedPreferences

/**
 * Controller responsible for managing audio and voice message transcription trial limits,
 * button interaction thresholds, cooldown timers, and minimum group levels.
 * Extracted from MessagesController as part of Phase 4 (Batch 4.8) modularization.
 */
class TranscribeAudioController(currentAccount: Int) : BaseController(currentAccount) {

    companion object {
        @JvmStatic
        private val instances = arrayOfNulls<TranscribeAudioController>(UserConfig.MAX_ACCOUNT_COUNT)
        private val lockObjects = Array(UserConfig.MAX_ACCOUNT_COUNT) { Any() }

        @JvmStatic
        fun getInstance(accountNum: Int): TranscribeAudioController {
            var local = instances[accountNum]
            if (local == null) {
                synchronized(lockObjects[accountNum]) {
                    local = instances[accountNum]
                    if (local == null) {
                        local = TranscribeAudioController(accountNum)
                        instances[accountNum] = local
                    }
                }
            }
            return local!!
        }
    }

    var transcribeAudioTrialWeeklyNumber: Int = if (BuildVars.DEBUG_PRIVATE_VERSION) 2 else 0
    var transcribeAudioTrialDurationMax: Int = 300
    var transcribeAudioTrialCooldownUntil: Int = 0
    var transcribeAudioTrialCurrentNumber: Int = transcribeAudioTrialWeeklyNumber
    var groupTranscribeLevelMin: Int = 1
    var transcribeButtonPressed: Int = 0

    private val preferences: SharedPreferences?
        get() = try {
            MessagesController.getMainSettings(currentAccount)
        } catch (_: Throwable) {
            null
        }

    init {
        loadSettings()
    }

    fun loadSettings() {
        preferences?.let { prefs ->
            transcribeButtonPressed = prefs.getInt("transcribeButtonPressed", 0)
            groupTranscribeLevelMin = prefs.getInt("groupTranscribeLevelMin", 1)
            transcribeAudioTrialWeeklyNumber = prefs.getInt(
                "transcribeAudioTrialWeeklyNumber",
                if (BuildVars.DEBUG_PRIVATE_VERSION) 2 else 0
            )
            transcribeAudioTrialCurrentNumber = prefs.getInt(
                "transcribeAudioTrialCurrentNumber",
                transcribeAudioTrialWeeklyNumber
            )
            transcribeAudioTrialDurationMax = prefs.getInt("transcribeAudioTrialDurationMax", 300)
            transcribeAudioTrialCooldownUntil = prefs.getInt("transcribeAudioTrialCooldownUntil", 0)
        }
    }

    fun didPressTranscribeButtonEnough(): Boolean {
        return transcribeButtonPressed >= 2
    }

    fun pressTranscribeButton() {
        if (transcribeButtonPressed < 2) {
            transcribeButtonPressed++
            preferences?.edit()?.putInt("transcribeButtonPressed", transcribeButtonPressed)?.apply()
            syncToMessagesController()
        }
    }

    fun updateTranscribeAudioTrialCurrentNumber(num: Int) {
        if (num != transcribeAudioTrialCurrentNumber) {
            transcribeAudioTrialCurrentNumber = num
            preferences?.edit()?.putInt("transcribeAudioTrialCurrentNumber", num)?.apply()
            syncToMessagesController()
        }
    }

    fun updateTranscribeAudioTrialCooldownUntil(until: Int) {
        if (until != transcribeAudioTrialCooldownUntil) {
            transcribeAudioTrialCooldownUntil = until
            preferences?.edit()?.putInt("transcribeAudioTrialCooldownUntil", until)?.apply()
            scheduleTranscriptionUpdate()
            syncToMessagesController()
        }
    }

    fun canTranscribeTrial(durationSeconds: Int, currentTimeSeconds: Int = getCurrentTime()): Boolean {
        if (transcribeAudioTrialWeeklyNumber <= 0 || durationSeconds > transcribeAudioTrialDurationMax) {
            return false
        }
        return transcribeAudioTrialCooldownUntil == 0 ||
               currentTimeSeconds > transcribeAudioTrialCooldownUntil ||
               transcribeAudioTrialCurrentNumber > 0
    }

    fun getTranscribeTrialCount(currentTimeSeconds: Int = getCurrentTime()): Int {
        if (transcribeAudioTrialWeeklyNumber <= 0) return 0
        if (transcribeAudioTrialCooldownUntil == 0 || currentTimeSeconds > transcribeAudioTrialCooldownUntil) {
            return transcribeAudioTrialWeeklyNumber
        }
        return transcribeAudioTrialCurrentNumber
    }

    fun isTranscribeTrialLocked(isPremium: Boolean, currentTimeSeconds: Int = getCurrentTime()): Boolean {
        if (isPremium) return false
        return transcribeAudioTrialCooldownUntil != 0 &&
               currentTimeSeconds <= transcribeAudioTrialCooldownUntil &&
               transcribeAudioTrialCurrentNumber <= 0
    }

    fun checkTrialCooldown(currentTimeSeconds: Int = getCurrentTime()) {
        if (transcribeAudioTrialWeeklyNumber > 0 &&
            transcribeAudioTrialCooldownUntil != 0 &&
            currentTimeSeconds > transcribeAudioTrialCooldownUntil) {
            transcribeAudioTrialCurrentNumber = transcribeAudioTrialWeeklyNumber
            preferences?.edit()?.putInt("transcribeAudioTrialCurrentNumber", transcribeAudioTrialCurrentNumber)?.apply()
            syncToMessagesController()
        }
    }

    fun onAppConfigUpdate(
        weeklyNumber: Int?,
        durationMax: Int?,
        cooldownUntil: Int?,
        groupLevelMin: Int?,
        currentTimeSeconds: Int = getCurrentTime()
    ) {
        val editor = preferences?.edit()
        var changed = false

        if (weeklyNumber != null && weeklyNumber != transcribeAudioTrialWeeklyNumber) {
            transcribeAudioTrialWeeklyNumber = weeklyNumber
            editor?.putInt("transcribeAudioTrialWeeklyNumber", weeklyNumber)
            if (transcribeAudioTrialCurrentNumber <= 0 &&
                (transcribeAudioTrialCooldownUntil == 0 || currentTimeSeconds > transcribeAudioTrialCooldownUntil)) {
                transcribeAudioTrialCurrentNumber = weeklyNumber
                editor?.putInt("transcribeAudioTrialCurrentNumber", weeklyNumber)
            } else if (transcribeAudioTrialCurrentNumber > weeklyNumber) {
                transcribeAudioTrialCurrentNumber = weeklyNumber
                editor?.putInt("transcribeAudioTrialCurrentNumber", weeklyNumber)
            }
            changed = true
        }

        if (durationMax != null && durationMax != transcribeAudioTrialDurationMax) {
            transcribeAudioTrialDurationMax = durationMax
            editor?.putInt("transcribeAudioTrialDurationMax", durationMax)
            changed = true
        }

        if (cooldownUntil != null && cooldownUntil != transcribeAudioTrialCooldownUntil) {
            transcribeAudioTrialCooldownUntil = cooldownUntil
            editor?.putInt("transcribeAudioTrialCooldownUntil", cooldownUntil)
            scheduleTranscriptionUpdate()
            changed = true
        }

        if (groupLevelMin != null && groupLevelMin != this.groupTranscribeLevelMin) {
            this.groupTranscribeLevelMin = groupLevelMin
            editor?.putInt("groupTranscribeLevelMin", groupLevelMin)
            changed = true
        }

        if (changed) {
            editor?.apply()
            syncToMessagesController()
        }
    }

    fun scheduleTranscriptionUpdate() {
        try {
            AndroidUtilities.runOnUIThread {
                AndroidUtilities.cancelRunOnUIThread(notifyTranscriptionAudioCooldownUpdate)
                val currentTime = getCurrentTime()
                val wait = (transcribeAudioTrialCooldownUntil - currentTime).toLong()
                if (wait > 0) {
                    AndroidUtilities.runOnUIThread(notifyTranscriptionAudioCooldownUpdate, wait * 1000)
                }
            }
        } catch (_: Throwable) {
            // JVM test or background thread fallback
        }
    }

    private val notifyTranscriptionAudioCooldownUpdate = Runnable {
        try {
            notificationCenter.postNotificationName(NotificationCenter.updateTranscriptionLock)
        } catch (_: Throwable) {}
    }

    fun getCurrentTime(): Int {
        return try {
            connectionsManager.currentTime
        } catch (_: Throwable) {
            (System.currentTimeMillis() / 1000).toInt()
        }
    }

    fun syncToMessagesController() {
        try {
            val mc = messagesController
            mc.transcribeAudioTrialWeeklyNumber = transcribeAudioTrialWeeklyNumber
            mc.transcribeAudioTrialDurationMax = transcribeAudioTrialDurationMax
            mc.transcribeAudioTrialCooldownUntil = transcribeAudioTrialCooldownUntil
            mc.transcribeAudioTrialCurrentNumber = transcribeAudioTrialCurrentNumber
            mc.groupTranscribeLevelMin = groupTranscribeLevelMin
            mc.transcribeButtonPressed = transcribeButtonPressed
        } catch (_: Throwable) {
            // Null or unavailable in tests
        }
    }

    fun cleanup() {
        transcribeButtonPressed = 0
        transcribeAudioTrialWeeklyNumber = if (BuildVars.DEBUG_PRIVATE_VERSION) 2 else 0
        transcribeAudioTrialDurationMax = 300
        transcribeAudioTrialCooldownUntil = 0
        transcribeAudioTrialCurrentNumber = transcribeAudioTrialWeeklyNumber
        groupTranscribeLevelMin = 1
    }
}
