package org.telegram.messenger.feature.system.recyclerscroll.data.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.system.recyclerscroll.data.datasource.RecyclerScrollLocalDataSource
import org.telegram.messenger.feature.system.recyclerscroll.data.datasource.RecyclerScrollRemoteDataSource
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.RecyclerScrollState
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollAnimationPlan
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollAnimationSpec
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollDirection
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollEligibility
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollViewTranslation
import org.telegram.messenger.feature.system.recyclerscroll.domain.repository.RecyclerScrollRepository

class RecyclerScrollRepositoryImpl(
    private val localDataSource: RecyclerScrollLocalDataSource,
    private val remoteDataSource: RecyclerScrollRemoteDataSource
) : RecyclerScrollRepository {

    override fun observeState(): StateFlow<RecyclerScrollState> = localDataSource.observeState()

    override fun getState(): RecyclerScrollState = localDataSource.getState()

    override fun evaluateEligibility(
        fastScrollRunning: Boolean,
        itemAnimatorRunning: Boolean,
        childCount: Int,
        viewAnimationsEnabled: Boolean,
        smooth: Boolean,
        direction: ScrollDirection
    ): ScrollEligibility {
        return localDataSource.evaluateEligibility(
            fastScrollRunning, itemAnimatorRunning, childCount, viewAnimationsEnabled, smooth, direction
        )
    }

    override fun calculatePlan(spec: ScrollAnimationSpec): ScrollAnimationPlan {
        return localDataSource.calculatePlan(spec)
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
        return localDataSource.calculateScrollLength(
            scrollDown, containerHeight, oldViewsCount, scrollDiff, oldTop, oldBottom, incomingTop, incomingBottom
        )
    }

    override fun computeViewTranslations(
        scrollLength: Int,
        isScrollDown: Boolean,
        progress: Float,
        additionalY: Int
    ): ScrollViewTranslation {
        return localDataSource.computeViewTranslations(scrollLength, isScrollDown, progress, additionalY)
    }

    override fun startScroll(position: Int, offset: Int, direction: ScrollDirection, durationMs: Long) {
        localDataSource.startScroll(position, offset, direction, durationMs)
    }

    override fun updateProgress(progress: Float) {
        localDataSource.updateProgress(progress)
    }

    override fun finishScroll() {
        localDataSource.finishScroll()
    }

    override fun cancelScroll() {
        localDataSource.cancelScroll()
    }

    override fun reset() {
        localDataSource.reset()
    }
}
