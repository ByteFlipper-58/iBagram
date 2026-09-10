package org.telegram.messenger.feature.system.recyclerscroll.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.system.recyclerscroll.data.mapper.RecyclerScrollMapper
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.RecyclerScrollState
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollAnimationPlan
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollAnimationSpec
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollDirection
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollEligibility
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollViewTranslation
import org.telegram.messenger.feature.system.recyclerscroll.domain.repository.RecyclerScrollRepository

/**
 * Thread-safe adapter implementation for Recycler list scroll animations.
 */
class LegacyRecyclerScrollRepository(
    initialState: RecyclerScrollState? = null
) : RecyclerScrollRepository {

    private val _state = MutableStateFlow(initialState ?: RecyclerScrollState())
    private val state: StateFlow<RecyclerScrollState> = _state.asStateFlow()

    override fun observeState(): StateFlow<RecyclerScrollState> = state

    override fun getState(): RecyclerScrollState = _state.value

    override fun evaluateEligibility(
        fastScrollRunning: Boolean,
        itemAnimatorRunning: Boolean,
        childCount: Int,
        viewAnimationsEnabled: Boolean,
        smooth: Boolean,
        direction: ScrollDirection
    ): ScrollEligibility {
        return RecyclerScrollMapper.evaluateEligibility(
            fastScrollRunning = fastScrollRunning,
            itemAnimatorRunning = itemAnimatorRunning,
            childCount = childCount,
            viewAnimationsEnabled = viewAnimationsEnabled,
            smooth = smooth,
            direction = direction
        )
    }

    override fun calculatePlan(spec: ScrollAnimationSpec): ScrollAnimationPlan {
        return RecyclerScrollMapper.calculatePlan(spec)
    }

    override fun calculateScrollLength(
        scrollDown: Boolean,
        containerHeight: Int,
        oldViewsCount: Int,
        scrollDiff: Int,
        oldTop: Int,
        oldBottom: Int,
        incomingTop: Int,
        incomingBottom: Int
    ): Int {
        return RecyclerScrollMapper.calculateScrollLength(
            scrollDown = scrollDown,
            containerHeight = containerHeight,
            oldViewsCount = oldViewsCount,
            scrollDiff = scrollDiff,
            oldTop = oldTop,
            oldBottom = oldBottom,
            incomingTop = incomingTop,
            incomingBottom = incomingBottom
        )
    }

    override fun computeViewTranslations(
        scrollLength: Int,
        isScrollDown: Boolean,
        progress: Float,
        additionalY: Int
    ): ScrollViewTranslation {
        return RecyclerScrollMapper.computeViewTranslations(
            scrollLength = scrollLength,
            isScrollDown = isScrollDown,
            progress = progress,
            additionalY = additionalY
        )
    }

    override fun startScroll(
        position: Int,
        offset: Int,
        direction: ScrollDirection,
        durationMs: Long
    ) {
        _state.update {
            it.copy(
                isRunning = true,
                direction = direction,
                targetPosition = position,
                targetOffset = offset,
                durationMs = durationMs,
                progress = 0f
            )
        }
    }

    override fun updateProgress(progress: Float) {
        _state.update {
            it.copy(progress = progress.coerceIn(0f, 1f))
        }
    }

    override fun finishScroll() {
        _state.update {
            it.copy(
                isRunning = false,
                progress = 1f
            )
        }
    }

    override fun cancelScroll() {
        _state.update {
            it.copy(
                isRunning = false,
                progress = 0f
            )
        }
    }

    override fun reset() {
        _state.value = RecyclerScrollState()
    }
}
