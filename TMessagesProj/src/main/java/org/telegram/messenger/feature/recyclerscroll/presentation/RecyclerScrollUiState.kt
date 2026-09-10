package org.telegram.messenger.feature.recyclerscroll.presentation

import org.telegram.messenger.feature.recyclerscroll.domain.model.RecyclerScrollState
import org.telegram.messenger.feature.recyclerscroll.domain.model.ScrollAnimationPlan
import org.telegram.messenger.feature.recyclerscroll.domain.model.ScrollViewTranslation

/**
 * Reactive UI state for recycler scroll animations.
 */
data class RecyclerScrollUiState(
    val scrollState: RecyclerScrollState = RecyclerScrollState(),
    val currentPlan: ScrollAnimationPlan? = null,
    val currentTranslation: ScrollViewTranslation? = null
)
