package org.telegram.messenger.feature.recyclerscroll.presentation

import org.telegram.messenger.feature.recyclerscroll.domain.model.ScrollAnimationSpec
import org.telegram.messenger.feature.recyclerscroll.domain.model.ScrollDirection

/**
 * MVI events for Recycler scroll animation interactions.
 */
sealed interface RecyclerScrollEvent {
    data class PrepareAndStartScroll(
        val position: Int,
        val offset: Int,
        val spec: ScrollAnimationSpec
    ) : RecyclerScrollEvent

    data class StartScroll(
        val position: Int,
        val offset: Int,
        val direction: ScrollDirection,
        val durationMs: Long
    ) : RecyclerScrollEvent

    data class UpdateProgress(val progress: Float) : RecyclerScrollEvent

    data object FinishScroll : RecyclerScrollEvent

    data object CancelScroll : RecyclerScrollEvent

    data object Reset : RecyclerScrollEvent
}
