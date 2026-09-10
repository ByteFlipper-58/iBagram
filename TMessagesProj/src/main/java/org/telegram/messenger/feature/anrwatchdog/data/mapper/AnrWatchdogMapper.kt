package org.telegram.messenger.feature.anrwatchdog.data.mapper

import org.telegram.messenger.feature.anrwatchdog.domain.model.AnrIncident
import org.telegram.messenger.feature.anrwatchdog.domain.model.AnrSeverity
import org.telegram.messenger.feature.anrwatchdog.domain.model.AnrWatchdogState

object AnrWatchdogMapper {

    fun createIncident(
        pingId: Long,
        freezeDurationMs: Long,
        generation: Int,
        timestampMs: Long = System.currentTimeMillis()
    ): AnrIncident {
        val severity = if (freezeDurationMs >= 5000L) AnrSeverity.CRITICAL else AnrSeverity.WARNING
        return AnrIncident(
            incidentId = "anr_${pingId}_$timestampMs",
            pingId = pingId,
            freezeDurationMs = freezeDurationMs,
            generation = generation,
            timestampMs = timestampMs,
            severity = severity,
            isRecovered = false,
            threadSummary = "Main UI thread blocked for ${freezeDurationMs}ms (generation: $generation, ping: $pingId)"
        )
    }

    fun formatSummary(state: AnrWatchdogState): String {
        return "AnrWatchdog[running=${state.isRunning}, lifecycle=${state.lifecycleState}, generation=${state.currentGeneration}, pingsSent=${state.lastSentPingId}, pingsAcked=${state.lastAcknowledgedPingId}, frozen=${state.isFrozen}, totalAnrs=${state.reportedAnrsCount}]"
    }
}
