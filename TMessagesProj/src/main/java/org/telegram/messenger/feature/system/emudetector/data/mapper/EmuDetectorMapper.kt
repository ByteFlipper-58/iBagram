package org.telegram.messenger.feature.system.emudetector.data.mapper

import org.telegram.messenger.feature.system.emudetector.domain.model.DetectionCategory
import org.telegram.messenger.feature.system.emudetector.domain.model.DetectionIndicator
import org.telegram.messenger.feature.system.emudetector.domain.model.EmulatorDiagnostics
import org.telegram.messenger.feature.system.emudetector.domain.model.EnvironmentVerdict

object EmuDetectorMapper {

    /**
     * Формирует подробный отчет о среде выполнения для системного лога или отладочного экрана.
     */
    fun formatDiagnosticsSummary(diagnostics: EmulatorDiagnostics): String {
        return buildString {
            append("Environment Verdict: ${diagnostics.verdict}")
            append(" (IsEmulator: ${diagnostics.isEmulator}, Score: ${diagnostics.confidenceScore})\n")
            append("Detected Types: ${diagnostics.detectedTypes.joinToString { it.name }}\n")
            append("Evaluated At: ${diagnostics.evaluatedAtMs} ms\n")
            append("Triggered Indicators (${diagnostics.triggeredIndicators.size}):\n")

            if (diagnostics.triggeredIndicators.isEmpty()) {
                append("  None (clean environment)\n")
            } else {
                for (indicator in diagnostics.triggeredIndicators) {
                    append("  - [${indicator.category}] ${indicator.name}")
                    if (!indicator.matchedValue.isNullOrBlank()) {
                        append(" (matched: \"${indicator.matchedValue}\")")
                    }
                    append(" [weight: ${indicator.confidenceWeight}]\n")
                }
            }
        }
    }

    /**
     * Создает индикатор с авто-определением веса по категории.
     */
    fun createIndicator(
        category: DetectionCategory,
        name: String,
        matchedValue: String? = null,
        isTriggered: Boolean = true
    ): DetectionIndicator {
        val weight = when (category) {
            DetectionCategory.KERNEL_DRIVERS,
            DetectionCategory.FILESYSTEM_ARTIFACTS,
            DetectionCategory.INPUT_DEVICES -> 3
            DetectionCategory.BUILD_PROPERTIES -> 2
            DetectionCategory.TELEPHONY -> 2
            DetectionCategory.PACKAGES -> 1
            DetectionCategory.NETWORK -> 1
        }
        return DetectionIndicator(
            category = category,
            name = name,
            matchedValue = matchedValue,
            isTriggered = isTriggered,
            confidenceWeight = weight
        )
    }

    /**
     * Краткое строковое резюме для заголовков и тултипов.
     */
    fun formatShortVerdict(diagnostics: EmulatorDiagnostics): String {
        return when (diagnostics.verdict) {
            EnvironmentVerdict.PHYSICAL_DEVICE -> "Физическое устройство"
            EnvironmentVerdict.SUSPICIOUS_ENVIRONMENT -> "Подозрительное окружение (score ${diagnostics.confidenceScore})"
            EnvironmentVerdict.EMULATOR_DETECTED -> "Обнаружен эмулятор (${diagnostics.detectedTypes.firstOrNull()?.name ?: "UNKNOWN"})"
        }
    }
}
