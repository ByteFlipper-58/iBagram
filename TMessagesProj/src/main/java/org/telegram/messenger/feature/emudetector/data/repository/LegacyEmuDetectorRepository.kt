package org.telegram.messenger.feature.emudetector.data.repository

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
import org.telegram.messenger.EmuInputDevicesDetector
import org.telegram.messenger.feature.emudetector.data.mapper.EmuDetectorMapper
import org.telegram.messenger.feature.emudetector.domain.model.DetectionCategory
import org.telegram.messenger.feature.emudetector.domain.model.DetectionIndicator
import org.telegram.messenger.feature.emudetector.domain.model.EmulatorDetectorConfig
import org.telegram.messenger.feature.emudetector.domain.model.EmulatorDiagnostics
import org.telegram.messenger.feature.emudetector.domain.model.EmulatorHeuristics
import org.telegram.messenger.feature.emudetector.domain.model.EnvironmentVerdict
import org.telegram.messenger.feature.emudetector.domain.repository.EmuDetectorRepository

class LegacyEmuDetectorRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val customDetector: (suspend (EmulatorDetectorConfig) -> EmulatorDiagnostics)? = null
) : EmuDetectorRepository {

    private val mutex = Mutex()
    private var config = EmulatorDetectorConfig()

    private val _diagnostics = MutableStateFlow<EmulatorDiagnostics?>(null)
    override fun observeDiagnostics(): StateFlow<EmulatorDiagnostics?> = _diagnostics.asStateFlow()

    private val _isEmulator = MutableStateFlow(false)
    override fun observeIsEmulator(): StateFlow<Boolean> = _isEmulator.asStateFlow()

    override suspend fun detectEnvironment(forceRefresh: Boolean): EmulatorDiagnostics {
        return mutex.withLock {
            val cached = _diagnostics.value
            if (!forceRefresh && cached != null) {
                return@withLock cached
            }

            val result = withContext(ioDispatcher) {
                if (customDetector != null) {
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

    override fun getCachedDiagnostics(): EmulatorDiagnostics? {
        return _diagnostics.value
    }

    override fun isEmulator(): Boolean {
        val cached = _diagnostics.value
        if (cached != null) {
            return cached.isEmulator
        }
        return try {
            if (customDetector != null) {
                _isEmulator.value
            } else {
                val detector = EmuDetector.with(ApplicationLoader.applicationContext)
                detector.detect()
            }
        } catch (_: Throwable) {
            false
        }
    }

    override fun getConfig(): EmulatorDetectorConfig = config

    override fun updateConfig(config: EmulatorDetectorConfig) {
        this.config = config
    }

    override fun addCustomPackage(packageName: String) {
        val currentPackages = config.customPackageNames.toMutableList()
        if (!currentPackages.contains(packageName)) {
            currentPackages.add(packageName)
            config = config.copy(customPackageNames = currentPackages)
        }
    }

    override fun clearCache() {
        _diagnostics.value = null
        _isEmulator.value = false
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
        } catch (e: Throwable) {
            // Игнорируем ошибки доступа к контексту/рефлексии
        }

        if (config.checkInputDevices) {
            try {
                val inputDevicesDetected = EmuInputDevicesDetector.detect()
                if (inputDevicesDetected) {
                    isEmu = true
                    indicators.add(
                        EmuDetectorMapper.createIndicator(
                            category = DetectionCategory.INPUT_DEVICES,
                            name = "RestrictedInputDevices",
                            matchedValue = "proc/bus/input/devices",
                            isTriggered = true
                        )
                    )
                }
            } catch (e: Throwable) {
                // Игнорируем
            }
        }

        val score = EmulatorHeuristics.calculateConfidenceScore(indicators)
        val verdict = EmulatorHeuristics.evaluateVerdict(
            confidenceScore = score,
            threshold = config.confidenceThreshold,
            hasCriticalIndicator = isEmu
        )
        val types = EmulatorHeuristics.resolveEmulatorTypes(indicators)

        return EmulatorDiagnostics(
            verdict = verdict,
            isEmulator = verdict == EnvironmentVerdict.EMULATOR_DETECTED,
            confidenceScore = score,
            triggeredIndicators = indicators,
            detectedTypes = types,
            evaluatedAtMs = System.currentTimeMillis()
        )
    }
}
