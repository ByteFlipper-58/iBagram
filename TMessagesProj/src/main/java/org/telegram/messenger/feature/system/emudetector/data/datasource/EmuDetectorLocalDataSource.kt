package org.telegram.messenger.feature.system.emudetector.data.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.EmuDetector
import org.telegram.messenger.feature.system.emudetector.data.mapper.EmuDetectorMapper
import org.telegram.messenger.feature.system.emudetector.domain.model.DetectionCategory
import org.telegram.messenger.feature.system.emudetector.domain.model.DetectionIndicator
import org.telegram.messenger.feature.system.emudetector.domain.model.EmulatorDetectorConfig
import org.telegram.messenger.feature.system.emudetector.domain.model.EmulatorDiagnostics
import org.telegram.messenger.feature.system.emudetector.domain.model.EmulatorHeuristics
import org.telegram.messenger.feature.system.emudetector.domain.model.EnvironmentVerdict

/**
 * Local data source for emulator and virtualized environment diagnostics.
 */
class EmuDetectorLocalDataSource(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val customDetector: (suspend (EmulatorDetectorConfig) -> EmulatorDiagnostics)? = null
) {
    private val mutex = Mutex()
    private var config = EmulatorDetectorConfig()
    private var isTestMode: Boolean = false
    private var testVerdict: EnvironmentVerdict = EnvironmentVerdict.PHYSICAL_DEVICE
    private var testIsEmulator: Boolean = false

    private val _diagnostics = MutableStateFlow<EmulatorDiagnostics?>(null)
    val diagnostics: StateFlow<EmulatorDiagnostics?> = _diagnostics.asStateFlow()

    private val _isEmulator = MutableStateFlow(false)
    val isEmulatorFlow: StateFlow<Boolean> = _isEmulator.asStateFlow()

    fun setTestMode(isTest: Boolean, simulatedVerdict: EnvironmentVerdict = EnvironmentVerdict.PHYSICAL_DEVICE, simulatedIsEmulator: Boolean = false) {
        this.isTestMode = isTest
        this.testVerdict = simulatedVerdict
        this.testIsEmulator = simulatedIsEmulator
    }

    suspend fun detectEnvironment(forceRefresh: Boolean = false): EmulatorDiagnostics {
        return mutex.withLock {
            val cached = _diagnostics.value
            if (!forceRefresh && cached != null) {
                return@withLock cached
            }

            val result = withContext(ioDispatcher) {
                if (isTestMode) {
                    performTestDetection(config)
                } else if (customDetector != null) {
                    customDetector.invoke(config)
                } else {
                    performLegacyDetection(config)
                }
            }

            _diagnostics.value = result
            _isEmulator.value = result.isEmulator
            result
        }
    }

    fun getCachedDiagnostics(): EmulatorDiagnostics? {
        return _diagnostics.value
    }

    fun isEmulator(): Boolean {
        val cached = _diagnostics.value
        if (cached != null) {
            return cached.isEmulator
        }
        if (isTestMode) {
            return testIsEmulator
        }
        return try {
            val context = ApplicationLoader.applicationContext
            if (context != null) {
                val detector = EmuDetector.with(context)
                detector.detect()
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun getConfig(): EmulatorDetectorConfig = config

    fun updateConfig(config: EmulatorDetectorConfig) {
        this.config = config
    }

    fun addCustomPackage(packageName: String) {
        val currentPackages = config.customPackageNames.toMutableList()
        if (!currentPackages.contains(packageName)) {
            currentPackages.add(packageName)
            config = config.copy(customPackageNames = currentPackages)
        }
    }

    fun clearCache() {
        _diagnostics.value = null
        _isEmulator.value = false
    }

    fun observeDiagnostics(): StateFlow<EmulatorDiagnostics?> = diagnostics

    fun observeIsEmulator(): StateFlow<Boolean> = isEmulatorFlow

    private fun performTestDetection(cfg: EmulatorDetectorConfig): EmulatorDiagnostics {
        val indicators = if (testIsEmulator) {
            listOf(
                DetectionIndicator(
                    category = DetectionCategory.BUILD_PROPERTIES,
                    name = "TestEmulatorFingerprint",
                    matchedValue = "goldfish",
                    isTriggered = true,
                    confidenceWeight = 3
                )
            )
        } else {
            emptyList()
        }
        val score = EmulatorHeuristics.calculateConfidenceScore(indicators)
        val verdict = if (testIsEmulator) EnvironmentVerdict.EMULATOR_DETECTED else testVerdict
        return EmulatorDiagnostics(
            verdict = verdict,
            isEmulator = testIsEmulator,
            confidenceScore = score,
            triggeredIndicators = indicators,
            detectedTypes = EmulatorHeuristics.resolveEmulatorTypes(indicators),
            evaluatedAtMs = System.currentTimeMillis()
        )
    }

    private fun performLegacyDetection(config: EmulatorDetectorConfig): EmulatorDiagnostics {
        val indicators = mutableListOf<DetectionIndicator>()
        var isEmu = false

        try {
            val context = ApplicationLoader.applicationContext
            if (context != null) {
                val detector = EmuDetector.with(context)
                detector.setCheckTelephony(config.checkTelephony)
                detector.setCheckPackage(config.checkPackages)
                for (pkg in config.customPackageNames) {
                    detector.addPackageName(pkg)
                }

                val detectedByEmuDetector = detector.detect()
                if (detectedByEmuDetector) {
                    isEmu = true
                    indicators.add(
                        EmuDetectorMapper.createIndicator(
                            category = DetectionCategory.BUILD_PROPERTIES,
                            name = "LegacyEmuDetectorHeuristic",
                            matchedValue = "detected",
                            isTriggered = true
                        )
                    )
                }
            }
        } catch (_: Throwable) {
            // Ignored in headless/unit tests
        }

        val confidenceScore = EmulatorHeuristics.calculateConfidenceScore(indicators)
        val hasCritical = indicators.any { it.isTriggered && it.confidenceWeight >= 3 }
        val verdict = EmulatorHeuristics.evaluateVerdict(confidenceScore, config.confidenceThreshold, hasCritical)

        return EmulatorDiagnostics(
            verdict = verdict,
            isEmulator = isEmu || verdict == EnvironmentVerdict.EMULATOR_DETECTED,
            confidenceScore = confidenceScore,
            triggeredIndicators = indicators,
            detectedTypes = EmulatorHeuristics.resolveEmulatorTypes(indicators),
            evaluatedAtMs = System.currentTimeMillis()
        )
    }
}
