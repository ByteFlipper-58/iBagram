package org.telegram.messenger.feature.windowvisibility.domain.model

/**
 * Scope or categorization of window visibility.
 */
enum class WindowVisibilityScope {
    ACTIVITY,
    DIALOG,
    BOTTOM_SHEET,
    OVERLAY,
    CUSTOM
}

/**
 * Pure domain descriptor representing a reason to hide a window.
 */
data class WindowVisibilityReason(
    val tag: String,
    val description: String = "",
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Aggregated state of window visibility arbitration.
 */
data class WindowVisibilityState(
    val isVisible: Boolean = true,
    val reasonsCount: Int = 0,
    val activeReasons: Set<String> = emptySet(),
    val lastChangedReason: String? = null
) {
    val isHidden: Boolean get() = !isVisible
}

/**
 * Result of a visibility state change transition.
 */
data class WindowVisibilityChangeResult(
    val previousState: WindowVisibilityState,
    val newState: WindowVisibilityState,
    val visibilityToggled: Boolean
)

/**
 * Controller contract allowing individual subsystems to acquire and release hide requests.
 */
interface WindowVisibilityController {
    val reasonTag: String
    val isHidden: Boolean
    val isDestroyed: Boolean
    fun setHidden(hidden: Boolean)
    fun destroy()
}
