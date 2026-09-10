package org.telegram.messenger.feature.recyclerscroll.domain.model

/**
 * Scroll direction for recycler list smooth animations.
 */
enum class ScrollDirection(val value: Int) {
    UNSET(-1),
    DOWN(0),
    UP(1);

    companion object {
        fun fromValue(value: Int): ScrollDirection {
            return entries.firstOrNull { it.value == value } ?: UNSET
        }
    }
}

/**
 * Parameters for determining scroll animation duration and interpolation.
 */
data class ScrollAnimationSpec(
    val scrollDirection: ScrollDirection,
    val isDialogs: Boolean,
    val hasSameViews: Boolean,
    val scrollLength: Int,
    val containerHeight: Int
)

/**
 * Calculated animation parameters for recycler scrolling.
 */
data class ScrollAnimationPlan(
    val durationMs: Long,
    val scrollLength: Int,
    val isScrollDown: Boolean
)

/**
 * Dynamic translations for outgoing and incoming views at a given animation progress.
 */
data class ScrollViewTranslation(
    val oldViewsTranslationY: Float,
    val incomingViewsTranslationY: Float
)

/**
 * Pre-condition assessment for animated scrolling vs instant jumping.
 */
data class ScrollEligibility(
    val canAnimate: Boolean,
    val fallbackToInstant: Boolean
)

/**
 * Reactive state of the recycler list scrolling subsystem.
 */
data class RecyclerScrollState(
    val isRunning: Boolean = false,
    val direction: ScrollDirection = ScrollDirection.UNSET,
    val targetPosition: Int = -1,
    val targetOffset: Int = 0,
    val progress: Float = 0f,
    val durationMs: Long = 0L
)
