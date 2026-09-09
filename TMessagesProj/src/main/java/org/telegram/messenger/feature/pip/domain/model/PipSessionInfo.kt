package org.telegram.messenger.feature.pip.domain.model

/**
 * Domain model describing the current active PiP session state and source arbitration.
 */
data class PipSessionInfo(
    val activeSource: PipSourceModel? = null,
    val registeredSources: List<PipSourceModel> = emptyList(),
    val pipState: PipState = PipState.IDLE,
    val isMediaSessionActive: Boolean = false,
    val lastAction: Pair<String, Int>? = null
) {
    val hasContentForPip: Boolean
        get() = activeSource != null

    val isInPip: Boolean
        get() = pipState == PipState.IN_PIP || pipState == PipState.STASHED
}
