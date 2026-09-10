package org.telegram.messenger.feature.emudetector

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.telegram.messenger.feature.emudetector.data.mapper.EmuDetectorMapper
import org.telegram.messenger.feature.emudetector.data.repository.LegacyEmuDetectorRepository
import org.telegram.messenger.feature.emudetector.domain.model.DetectionCategory
import org.telegram.messenger.feature.emudetector.domain.model.DetectionIndicator
import org.telegram.messenger.feature.emudetector.domain.model.EmulatorDetectorConfig
import org.telegram.messenger.feature.emudetector.domain.model.EmulatorDiagnostics
import org.telegram.messenger.feature.emudetector.domain.model.EmulatorHeuristics
import org.telegram.messenger.feature.emudetector.domain.model.EmulatorType
import org.telegram.messenger.feature.emudetector.domain.model.EnvironmentVerdict
import org.telegram.messenger.feature.emudetector.domain.usecase.AddCustomPackageNameUseCase
import org.telegram.messenger.feature.emudetector.domain.usecase.ClearDetectorCacheUseCase
import org.telegram.messenger.feature.emudetector.domain.usecase.DetectEnvironmentUseCase
import org.telegram.messenger.feature.emudetector.domain.usecase.GetCachedDiagnosticsUseCase
import org.telegram.messenger.feature.emudetector.domain.usecase.GetDetectorConfigUseCase
import org.telegram.messenger.feature.emudetector.domain.usecase.IsEmulatorUseCase
import org.telegram.messenger.feature.emudetector.domain.usecase.ObserveDiagnosticsUseCase
import org.telegram.messenger.feature.emudetector.domain.usecase.ObserveIsEmulatorUseCase
import org.telegram.messenger.feature.emudetector.domain.usecase.UpdateDetectorConfigUseCase
import org.telegram.messenger.feature.emudetector.presentation.EmuDetectorEvent
import org.telegram.messenger.feature.emudetector.presentation.EmuDetectorViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class EmuDetectorDomainTest {

    @Test
    fun testHeuristicsScoreAndThresholds() {
        val cleanIndicators = listOf(
            DetectionIndicator(DetectionCategory.BUILD_PROPERTIES, "BuildModel", "Pixel 8", isTriggered = false, confidenceWeight = 2),
            DetectionIndicator(DetectionCategory.FILESYSTEM_ARTIFACTS, "QemuPipes", null, isTriggered = false, confidenceWeight = 3)
        )
        val cleanScore = EmulatorHeuristics.calculateConfidenceScore(cleanIndicators)
        assertEquals(0, cleanScore)
        val cleanVerdict = EmulatorHeuristics.evaluateVerdict(cleanScore, threshold = 2, hasCriticalIndicator = false)
        assertEquals(EnvironmentVerdict.PHYSICAL_DEVICE, cleanVerdict)

        val suspiciousIndicators = listOf(
            DetectionIndicator(DetectionCategory.PACKAGES, "com.example.emu", isTriggered = true, confidenceWeight = 1)
        )
        val suspScore = EmulatorHeuristics.calculateConfidenceScore(suspiciousIndicators)
        assertEquals(1, suspScore)
        val suspVerdict = EmulatorHeuristics.evaluateVerdict(suspScore, threshold = 2, hasCriticalIndicator = false)
        assertEquals(EnvironmentVerdict.SUSPICIOUS_ENVIRONMENT, suspVerdict)

        val emuIndicators = listOf(
            DetectionIndicator(DetectionCategory.KERNEL_DRIVERS, "goldfish", isTriggered = true, confidenceWeight = 3),
            DetectionIndicator(DetectionCategory.BUILD_PROPERTIES, "BuildHardware", "goldfish", isTriggered = true, confidenceWeight = 2)
        )
        val emuScore = EmulatorHeuristics.calculateConfidenceScore(emuIndicators)
        assertEquals(5, emuScore)
        val emuVerdict = EmulatorHeuristics.evaluateVerdict(emuScore, threshold = 2, hasCriticalIndicator = false)
        assertEquals(EnvironmentVerdict.EMULATOR_DETECTED, emuVerdict)
    }

    @Test
    fun testEmulatorTypeResolution() {
        val indicators = listOf(
            DetectionIndicator(DetectionCategory.BUILD_PROPERTIES, "BuildManufacturer", "Genymotion", isTriggered = true),
            DetectionIndicator(DetectionCategory.FILESYSTEM_ARTIFACTS, "NoxFiles", "BigNoxGameHD", isTriggered = true),
            DetectionIndicator(DetectionCategory.INPUT_DEVICES, "InputDevice", "bluestacks_input", isTriggered = true),
            DetectionIndicator(DetectionCategory.BUILD_PROPERTIES, "BuildProduct", "google_sdk", isTriggered = true)
        )

        val types = EmulatorHeuristics.resolveEmulatorTypes(indicators)
        assertTrue(types.contains(EmulatorType.GENYMOTION))
        assertTrue(types.contains(EmulatorType.NOX))
        assertTrue(types.contains(EmulatorType.BLUESTACKS))
        assertTrue(types.contains(EmulatorType.GENERIC_SDK))
    }

    @Test
    fun testDiagnosticsSummaryFormatting() {
        val diagnostics = EmulatorDiagnostics(
            verdict = EnvironmentVerdict.EMULATOR_DETECTED,
            isEmulator = true,
            confidenceScore = 6,
            triggeredIndicators = listOf(
                EmuDetectorMapper.createIndicator(
                    category = DetectionCategory.KERNEL_DRIVERS,
                    name = "QEMU_CPU",
                    matchedValue = "goldfish",
                    isTriggered = true
                ),
                EmuDetectorMapper.createIndicator(
                    category = DetectionCategory.INPUT_DEVICES,
                    name = "InputDevices",
                    matchedValue = "virtualbox",
                    isTriggered = true
                )
            ),
            detectedTypes = listOf(EmulatorType.QEMU_PIPES, EmulatorType.VIRTUALBOX),
            evaluatedAtMs = 123456789L
        )

        val summary = EmuDetectorMapper.formatDiagnosticsSummary(diagnostics)
        assertTrue(summary.contains("EMULATOR_DETECTED"))
        assertTrue(summary.contains("QEMU_PIPES"))
        assertTrue(summary.contains("VIRTUALBOX"))
        assertTrue(summary.contains("weight: 3"))

        val shortVerdict = EmuDetectorMapper.formatShortVerdict(diagnostics)
        assertTrue(shortVerdict.contains("Обнаружен эмулятор"))
    }

    @Test
    fun testRepositoryCacheAndForceRefresh() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        var detectionCount = 0

        val repo = LegacyEmuDetectorRepository(
            ioDispatcher = testDispatcher,
            customDetector = { config ->
                detectionCount++
                EmulatorDiagnostics(
                    verdict = EnvironmentVerdict.EMULATOR_DETECTED,
                    isEmulator = true,
                    confidenceScore = 5,
                    triggeredIndicators = listOf(
                        DetectionIndicator(DetectionCategory.BUILD_PROPERTIES, "SDK", isTriggered = true, confidenceWeight = 5)
                    ),
                    detectedTypes = listOf(EmulatorType.GENERIC_SDK),
                    evaluatedAtMs = 1000L
                )
            }
        )

        val firstResult = repo.detectEnvironment(forceRefresh = false)
        assertEquals(1, detectionCount)
        assertTrue(firstResult.isEmulator)
        assertEquals(EnvironmentVerdict.EMULATOR_DETECTED, firstResult.verdict)

        // Cached retrieval
        val cached = repo.getCachedDiagnostics()
        assertNotNull(cached)
        assertEquals(1, detectionCount)

        val secondResult = repo.detectEnvironment(forceRefresh = false)
        assertEquals(1, detectionCount)
        assertEquals(firstResult, secondResult)

        // Force refresh
        val thirdResult = repo.detectEnvironment(forceRefresh = true)
        assertEquals(2, detectionCount)

        // Clear cache
        repo.clearCache()
        assertNull(repo.getCachedDiagnostics())
        assertFalse(repo.observeIsEmulator().value)
    }

    @Test
    fun testConfigUpdateAndCustomPackages() {
        val repo = LegacyEmuDetectorRepository()
        assertEquals(2, repo.getConfig().confidenceThreshold)
        assertTrue(repo.getConfig().checkTelephony)

        val newConfig = EmulatorDetectorConfig(
            checkTelephony = false,
            checkPackages = true,
            checkInputDevices = false,
            confidenceThreshold = 4
        )
        repo.updateConfig(newConfig)
        assertEquals(4, repo.getConfig().confidenceThreshold)
        assertFalse(repo.getConfig().checkTelephony)

        repo.addCustomPackage("com.mumu.launcher")
        assertTrue(repo.getConfig().customPackageNames.contains("com.mumu.launcher"))
    }

    @Test
    fun testEmuDetectorViewModelMviFlow() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)

        val repo = LegacyEmuDetectorRepository(
            ioDispatcher = testDispatcher,
            customDetector = { config ->
                EmulatorDiagnostics(
                    verdict = EnvironmentVerdict.PHYSICAL_DEVICE,
                    isEmulator = false,
                    confidenceScore = 0,
                    triggeredIndicators = emptyList(),
                    detectedTypes = emptyList(),
                    evaluatedAtMs = 2000L
                )
            }
        )

        val viewModel = EmuDetectorViewModel(
            detectEnvironmentUseCase = DetectEnvironmentUseCase(repo),
            observeDiagnosticsUseCase = ObserveDiagnosticsUseCase(repo),
            getDetectorConfigUseCase = GetDetectorConfigUseCase(repo),
            updateDetectorConfigUseCase = UpdateDetectorConfigUseCase(repo),
            addCustomPackageNameUseCase = AddCustomPackageNameUseCase(repo),
            clearDetectorCacheUseCase = ClearDetectorCacheUseCase(repo),
            scope = scope
        )

        advanceUntilIdle()

        assertNull(viewModel.uiState.value.diagnostics)
        assertFalse(viewModel.uiState.value.isEmulator)

        viewModel.onEvent(EmuDetectorEvent.RunDetection(forceRefresh = false))
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.diagnostics)
        assertFalse(viewModel.uiState.value.isEmulator)
        assertTrue(viewModel.uiState.value.isPhysicalDevice)
        assertEquals(EnvironmentVerdict.PHYSICAL_DEVICE, viewModel.uiState.value.verdict)

        viewModel.onEvent(EmuDetectorEvent.AddCustomPackage("com.bluestacks.home"))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.config.customPackageNames.contains("com.bluestacks.home"))

        viewModel.onEvent(EmuDetectorEvent.ClearCache)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.diagnostics)

        viewModel.onCleared()
    }
}
