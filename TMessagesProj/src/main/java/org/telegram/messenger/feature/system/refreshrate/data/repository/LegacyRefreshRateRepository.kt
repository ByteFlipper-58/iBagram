package org.telegram.messenger.feature.system.refreshrate.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.refreshrate.domain.model.DisplayRefreshModeModel
import org.telegram.messenger.feature.system.refreshrate.domain.model.RefreshRateDirection
import org.telegram.messenger.feature.system.refreshrate.domain.model.RefreshRateHysteresisConfig
import org.telegram.messenger.feature.system.refreshrate.domain.model.RefreshRateStateModel
import org.telegram.messenger.feature.system.refreshrate.domain.repository.RefreshRateRepository

class LegacyRefreshRateRepository(
    private val config: RefreshRateHysteresisConfig = RefreshRateHysteresisConfig()
) : RefreshRateRepository {

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

    private val frameNs = LongArray(config.ringSize)
    private var ringCount = 0
    private var ringPos = 0
    private var ringSumNs = 0L

    private var belowSinceMs = -1L
    private var aboveSinceMs = -1L
    private var lastSwitchMs = 0L

    override fun observeState(): Flow<RefreshRateStateModel> = _state.asStateFlow()

    override fun getState(): RefreshRateStateModel = _state.value

    override fun startTracking(): Result<Unit> {
        synchronized(lock) {
            val current = _state.value
            _state.value = current.copy(isTrackingActive = true)
            return Result.Success(Unit)
        }
    }

    override fun stopTracking(): Result<Unit> {
        synchronized(lock) {
            resetStatsInternal()
            val current = _state.value
            _state.value = current.copy(isTrackingActive = false)
            return Result.Success(Unit)
        }
    }

    override fun setAdaptiveEnabled(enabled: Boolean): Result<Unit> {
        synchronized(lock) {
            val current = _state.value
            _state.value = current.copy(isAdaptiveEnabled = enabled)
            return Result.Success(Unit)
        }
    }

    override fun setPreferredMode(mode: DisplayRefreshModeModel): Result<Unit> {
        synchronized(lock) {
            val current = _state.value
            val is60 = current.mode60?.modeId == mode.modeId || mode.isApproximately60Hz
            _state.value = current.copy(
                currentMode = mode,
                isPreferring60 = is60,
                lastDirection = if (is60) RefreshRateDirection.DOWN else RefreshRateDirection.UP
            )
            return Result.Success(Unit)
        }
    }

    override fun recordFrameDuration(durationNs: Long): Result<Unit> {
        if (durationNs <= 0) return Result.Success(Unit)

        synchronized(lock) {
            pushFrame(durationNs)
            val avgFps = getAvgFps()
            val nowMs = System.currentTimeMillis()

            var updated = _state.value.copy(
                currentFps = avgFps,
                totalFramesTracked = _state.value.totalFramesTracked + 1
            )

            if (updated.isAdaptiveEnabled && updated.isTrackingActive && ringCount >= 30) {
                val canSwitch = (nowMs - lastSwitchMs) >= config.minSwitchIntervalMs
                val preferHigh = !updated.isPreferring60

                if (preferHigh) {
                    if (avgFps <= config.downFpsThreshold) {
                        if (belowSinceMs < 0) belowSinceMs = nowMs
                        if ((nowMs - belowSinceMs) >= config.stableWindowMs && canSwitch) {
                            val mode60 = updated.mode60 ?: defaultMode60
                            updated = updated.copy(
                                currentMode = mode60,
                                isPreferring60 = true,
                                lastDirection = RefreshRateDirection.DOWN
                            )
                            lastSwitchMs = nowMs
                            belowSinceMs = -1L
                            aboveSinceMs = -1L
                        }
                    } else {
                        belowSinceMs = -1L
                    }
                } else {
                    if (avgFps >= config.upFpsThreshold) {
                        if (aboveSinceMs < 0) aboveSinceMs = nowMs
                        if ((nowMs - aboveSinceMs) >= config.stableWindowMs && canSwitch) {
                            val modeMax = updated.modeMax ?: defaultModeMax
                            updated = updated.copy(
                                currentMode = modeMax,
                                isPreferring60 = false,
                                lastDirection = RefreshRateDirection.UP
                            )
                            lastSwitchMs = nowMs
                            belowSinceMs = -1L
                            aboveSinceMs = -1L
                        }
                    } else {
                        aboveSinceMs = -1L
                    }
                }
            }

            _state.value = updated
            return Result.Success(Unit)
        }
    }

    override fun resetStats(): Result<Unit> {
        synchronized(lock) {
            resetStatsInternal()
            _state.value = _state.value.copy(
                currentFps = 60.0f,
                totalFramesTracked = 0L,
                lastDirection = RefreshRateDirection.NONE
            )
            return Result.Success(Unit)
        }
    }

    override fun getAvailableModes(): List<DisplayRefreshModeModel> {
        return _state.value.availableModes
    }

    private fun pushFrame(totalNs: Long) {
        if (ringCount < config.ringSize) {
            ringCount++
        } else {
            ringSumNs -= frameNs[ringPos]
        }
        frameNs[ringPos] = totalNs
        ringSumNs += totalNs
        ringPos++
        if (ringPos == config.ringSize) ringPos = 0
    }

    private fun getAvgFps(): Float {
        if (ringCount == 0) return 0f
        val avgFrameNs = ringSumNs.toDouble() / ringCount.toDouble()
        if (avgFrameNs <= 0.0) return 0f
        return (1_000_000_000.0 / avgFrameNs).toFloat()
    }

    private fun resetStatsInternal() {
        ringCount = 0
        ringPos = 0
        ringSumNs = 0L
        belowSinceMs = -1L
        aboveSinceMs = -1L
    }
}
