package org.telegram.messenger.feature.fpscontent.presentation

import org.telegram.messenger.feature.fpscontent.domain.model.FpsContentStats
import org.telegram.messenger.feature.fpscontent.domain.model.FrameCallbackSubscription
import org.telegram.messenger.feature.fpscontent.domain.model.FrameTick

/**
 * UI State for FPS arbitration dashboard and debug inspector.
 */
data class FpsContentUiState(
    val stats: FpsContentStats = FpsContentStats(),
    val subscriptions: List<FrameCallbackSubscription> = emptyList(),
    val recentTicks: List<FrameTick> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val isRunning: Boolean get() = stats.isRunning
    val totalDispatchedFrames: Long get() = stats.totalDispatchedFrames
    val activeSubscriptionsCount: Int get() = subscriptions.size
    val hasActiveAnimations: Boolean get() = activeSubscriptionsCount > 0 || stats.pendingViewsCount > 0 || stats.pendingDrawablesCount > 0
}
