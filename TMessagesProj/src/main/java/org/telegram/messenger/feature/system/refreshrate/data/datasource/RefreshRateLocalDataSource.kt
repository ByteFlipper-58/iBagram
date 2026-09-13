package org.telegram.messenger.feature.system.refreshrate.data.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.system.refreshrate.domain.model.DisplayRefreshModeModel
import org.telegram.messenger.feature.system.refreshrate.domain.model.RefreshRateDirection
import org.telegram.messenger.feature.system.refreshrate.domain.model.RefreshRateHysteresisConfig
import org.telegram.messenger.feature.system.refreshrate.domain.model.RefreshRateStateModel

class RefreshRateLocalDataSource(
    private val config: RefreshRateHysteresisConfig = RefreshRateHysteresisConfig()
) {
    private val lock = Any()

    private val defaultMode60 = DisplayRefreshModeModel(modeId = 1, width = 1080, height = 2400, refreshRate = 60.0f)
    private val defaultModeMax = DisplayRefreshModeModel(modeId = 2, width = 1080, height = 2400, refreshRate = 120.0f)
    private val defaultModes = listOf(defaultMode60, defaultModeMax)

    private val _state = MutableStateFlow(
        RefreshRateStateModel(
            isTrackingActive = false,
            isAdaptiveEnabled = true,
            currentMode = defaultModeMax,
            mode60 = defaultMode60,
            modeMax = defaultModeMax,
            isPreferring60 = false,
            currentFps = 120.0f,
            totalFramesTracked = 0L,
            lastDirection = RefreshRateDirection.NONE,
            availableModes = defaultModes
        )
    )
    val state: StateFlow<RefreshRateStateModel> = _state.asStateFlow()

    private val frameNs = LongArray(config.ringSize)
    private var ringCount = 0
    private var ringPos = 0
    private var ringSumNs = 0L

    private var belowSinceMs = -1L
    private var aboveSinceMs = -1L
    private var lastSwitchMs = 0L

    fun getState(): RefreshRateStateModel = _state.value

    fun startTracking() {
        synchronized(lock) {
            _state.value = _state.value.copy(isTrackingActive = true)
        }
    }

    fun stopTracking() {
        synchronized(lock) {
            resetStatsInternal()
            _state.value = _state.value.copy(isTrackingActive = false)
        }
    }

    fun setAdaptiveEnabled(enabled: Boolean) {
        synchronized(lock) {
            _state.value = _state.value.copy(isAdaptiveEnabled = enabled)
        }
    }

    fun setPreferredMode(mode: DisplayRefreshModeModel) {
        synchronized(lock) {
            val current = _state.value
            val is60 = current.mode60?.modeId == mode.modeId || mode.isApproximately60Hz
            _state.value = current.copy(
                currentMode = mode,
                isPreferring60 = is60,
                lastDirection = if (is60) RefreshRateDirection.DOWN else RefreshRateDirection.UP
            )
        }
    }

    fun recordFrameDuration(durationNs: Long) {
        if (durationNs <= 0) return

        synchronized(lock) {
            pushFrame(durationNs)
            val avgFps = getAvgFps()
            val nowMs = System.currentTimeMillis()

            var updated = _state.value.copy(
                currentFps = avgFps,
                totalFramesTracked = _state.value.totalFramesTracked + 1
            )

            if (updated.isTrackingActive && updated.isAdaptiveEnabled && updated.canSwitchRefreshRate) {
                if (updated.isPreferring60) {
                    if (avgFps >= config.upFpsThreshold) {
                        if (aboveSinceMs < 0) aboveSinceMs = nowMs
                        if (nowMs - aboveSinceMs >= config.stableWindowMs && nowMs - lastSwitchMs >= config.minSwitchIntervalMs) {
                            lastSwitchMs = nowMs
                            aboveSinceMs = -1L
                            belowSinceMs = -1L
                            updated = updated.copy(
                                currentMode = updated.modeMax,
                                isPreferring60 = false,
                                lastDirection = RefreshRateDirection.UP
                            )
                        }
                    } else {
                        aboveSinceMs = -1L
                    }
                } else {
                    if (avgFps <= config.downFpsThreshold) {
                        if (belowSinceMs < 0) belowSinceMs = nowMs
                        if (nowMs - belowSinceMs >= config.stableWindowMs && nowMs - lastSwitchMs >= config.minSwitchIntervalMs) {
                            lastSwitchMs = nowMs
                            belowSinceMs = -1L
                            aboveSinceMs = -1L
                            updated = updated.copy(
                                currentMode = updated.mode60,
                                isPreferring60 = true,
                                lastDirection = RefreshRateDirection.DOWN
                            )
                        }
                    } else {
                        belowSinceMs = -1L
                    }
                }
            }

            _state.value = updated
        }
    }

    fun resetStats() {
        synchronized(lock) {
            resetStatsInternal()
            _state.value = _state.value.copy(
                currentFps = _state.value.currentMode?.refreshRate ?: 60.0f,
                totalFramesTracked = 0L,
                lastDirection = RefreshRateDirection.NONE
            )
        }
    }

    fun getAvailableModes(): List<DisplayRefreshModeModel> = _state.value.availableModes

    private fun pushFrame(durationNs: Long) {
        if (ringCount < config.ringSize) {
            frameNs[ringPos] = durationNs
            ringSumNs += durationNs
            ringCount++
            ringPos = (ringPos + 1) % config.ringSize
        } else {
            ringSumNs -= frameNs[ringPos]
            frameNs[ringPos] = durationNs
            ringSumNs += durationNs
            ringPos = (ringPos + 1) % config.ringSize
        }
    }

    private fun getAvgFps(): Float {
        if (ringCount == 0 || ringSumNs == 0L) return 60.0f
        val avgNs = ringSumNs.toDouble() / ringCount
        val fps = 1_000_000_000.0 / avgNs
        return fps.toFloat().coerceIn(1.0f, 240.0f)
    }

    private fun resetStatsInternal() {
        frameNs.fill(0L)
        ringCount = 0
        ringPos = 0
        ringSumNs = 0L
        belowSinceMs = -1L
        aboveSinceMs = -1L
        lastSwitchMs = 0L
    }
}
