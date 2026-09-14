package org.telegram.messenger.feature.system.recyclerscroll.data.datasource

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

open class RecyclerScrollLocalDataSource(
    initialState: RecyclerScrollState? = null
) {
    private val _state = MutableStateFlow(initialState ?: RecyclerScrollState())
    private val state: StateFlow<RecyclerScrollState> = _state.asStateFlow()

    open fun observeState(): StateFlow<RecyclerScrollState> = state

    open fun getState(): RecyclerScrollState = _state.value

    open fun evaluateEligibility(
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

    open fun calculatePlan(spec: ScrollAnimationSpec): ScrollAnimationPlan {
        return RecyclerScrollMapper.calculatePlan(spec)
    }

    open fun calculateScrollLength(
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

    open fun computeViewTranslations(
        scrollLength: Int,
        isScrollDown: Boolean,
        progress: Float,
        additionalY: Int = 0
    ): ScrollViewTranslation {
        return RecyclerScrollMapper.computeViewTranslations(
            scrollLength = scrollLength,
            isScrollDown = isScrollDown,
            progress = progress,
            additionalY = additionalY
        )
    }

    open fun startScroll(
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

    open fun updateProgress(progress: Float) {
        _state.update {
            it.copy(progress = progress.coerceIn(0f, 1f))
        }
    }

    open fun finishScroll() {
        _state.update {
            it.copy(
                isRunning = false,
                progress = 1f
            )
        }
    }

    open fun cancelScroll() {
        _state.update {
            it.copy(
                isRunning = false,
                progress = 0f
            )
        }
    }

    open fun reset() {
        _state.value = RecyclerScrollState()
    }
}
