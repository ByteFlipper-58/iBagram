package org.telegram.messenger.feature.system.emudetector.domain.model

/**
 * Тип эмулятора или виртуализированной среды.
 */
enum class EmulatorType {
    GENYMOTION,
    ANDY,
    NOX,
    BLUESTACKS,
    QEMU_PIPES,
    X86_VM,
    GENERIC_SDK,
    MEMU,
    VIRTUALBOX,
    UNKNOWN
}

/**
 * Категория эвристического индикатора обнаружения эмулятора.
 */
enum class DetectionCategory {
    BUILD_PROPERTIES,
    FILESYSTEM_ARTIFACTS,
    KERNEL_DRIVERS,
    INPUT_DEVICES,
    TELEPHONY,
    PACKAGES,
    NETWORK
}

/**
 * Отдельный индикатор обнаружения эмулятора.
 */
data class DetectionIndicator(
    val category: DetectionCategory,
    val name: String,
    val matchedValue: String? = null,
    val isTriggered: Boolean = false,
    val confidenceWeight: Int = 1
)

/**
 * Итоговый вердикт среды выполнения приложения.
 */
enum class EnvironmentVerdict {
    PHYSICAL_DEVICE,
    SUSPICIOUS_ENVIRONMENT,
    EMULATOR_DETECTED
}

/**
 * Полный снимок диагностики среды выполнения.
 */
data class EmulatorDiagnostics(
    val verdict: EnvironmentVerdict = EnvironmentVerdict.PHYSICAL_DEVICE,
    val isEmulator: Boolean = false,
    val confidenceScore: Int = 0,
    val triggeredIndicators: List<DetectionIndicator> = emptyList(),
    val detectedTypes: List<EmulatorType> = emptyList(),
    val evaluatedAtMs: Long = 0L
)

/**
 * Конфигурация детектора эмулятора.
 */
data class EmulatorDetectorConfig(
    val checkTelephony: Boolean = true,
    val checkPackages: Boolean = true,
    val checkInputDevices: Boolean = true,
    val customPackageNames: List<String> = emptyList(),
    val confidenceThreshold: Int = 2
)

/**
 * Чистая доменная математика и эвристика оценки среды.
 */
object EmulatorHeuristics {

    fun calculateConfidenceScore(indicators: List<DetectionIndicator>): Int {
        return indicators.filter { it.isTriggered }.sumOf { it.confidenceWeight }
    }

    fun evaluateVerdict(
        confidenceScore: Int,
        threshold: Int,
        hasCriticalIndicator: Boolean
    ): EnvironmentVerdict {
        return when {
            hasCriticalIndicator || confidenceScore >= threshold -> EnvironmentVerdict.EMULATOR_DETECTED
            confidenceScore > 0 -> EnvironmentVerdict.SUSPICIOUS_ENVIRONMENT
            else -> EnvironmentVerdict.PHYSICAL_DEVICE
        }
    }

    fun resolveEmulatorTypes(indicators: List<DetectionIndicator>): List<EmulatorType> {
        val triggered = indicators.filter { it.isTriggered }
        val types = mutableSetOf<EmulatorType>()

        for (indicator in triggered) {
            val nameLower = indicator.name.lowercase()
            val valLower = indicator.matchedValue?.lowercase().orEmpty()
            val combined = "$nameLower $valLower"

            when {
                combined.contains("genymotion") -> types.add(EmulatorType.GENYMOTION)
                combined.contains("nox") -> types.add(EmulatorType.NOX)
                combined.contains("bluestacks") -> types.add(EmulatorType.BLUESTACKS)
                combined.contains("andy") -> types.add(EmulatorType.ANDY)
                combined.contains("qemu") || combined.contains("goldfish") || combined.contains("ranchu") -> types.add(EmulatorType.QEMU_PIPES)
                combined.contains("x86") || combined.contains("vbox") -> types.add(EmulatorType.X86_VM)
                combined.contains("memu") -> types.add(EmulatorType.MEMU)
                combined.contains("virtualbox") -> types.add(EmulatorType.VIRTUALBOX)
                combined.contains("google_sdk") || combined.contains("generic") -> types.add(EmulatorType.GENERIC_SDK)
                else -> types.add(EmulatorType.UNKNOWN)
            }
        }

        return types.toList()
    }
}
