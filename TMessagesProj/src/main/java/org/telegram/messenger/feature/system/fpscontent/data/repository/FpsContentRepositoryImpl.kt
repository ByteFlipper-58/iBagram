package org.telegram.messenger.feature.system.fpscontent.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.system.fpscontent.data.datasource.FpsContentLocalDataSource
import org.telegram.messenger.feature.system.fpscontent.data.datasource.FpsContentRemoteDataSource
import org.telegram.messenger.feature.system.fpscontent.domain.model.FpsContentStats
import org.telegram.messenger.feature.system.fpscontent.domain.model.FrameCallbackSubscription
import org.telegram.messenger.feature.system.fpscontent.domain.model.FrameTick
import org.telegram.messenger.feature.system.fpscontent.domain.repository.FpsContentRepository

/**
 * Implementation of [FpsContentRepository] coordinating local frame arbitration and remote policy.
 */
class FpsContentRepositoryImpl(
    private val localDataSource: FpsContentLocalDataSource,
    private val remoteDataSource: FpsContentRemoteDataSource
) : FpsContentRepository {

    override fun addFrameCallback(
        fps: Int,
        isOneShot: Boolean,
        onFrame: (Long) -> Unit
    ): String {
        return localDataSource.addFrameCallback(fps, isOneShot, onFrame)
    }

    override fun addRunnableCallback(
        fps: Int,
        isOneShot: Boolean,
        action: () -> Unit
    ): String {
        return localDataSource.addRunnableCallback(fps, isOneShot, action)
    }

    override fun removeCallback(subscriptionId: String): Boolean {
        return localDataSource.removeCallback(subscriptionId)
    }

    override fun postInvalidateView(viewId: String): Boolean {
        return localDataSource.postInvalidateView(viewId)
    }

    override fun postInvalidateDrawable(drawableId: String, fps: Int): Boolean {
        return localDataSource.postInvalidateDrawable(drawableId, fps)
    }

    override fun dispatchVsync(frameTimeNanos: Long): List<FrameTick> {
        return localDataSource.dispatchVsync(frameTimeNanos)
    }

    override fun getStats(): FpsContentStats {
        return localDataSource.getStats()
    }

    override fun getSubscriptions(): List<FrameCallbackSubscription> {
        return localDataSource.getSubscriptions()
    }

    override fun reset() {
        localDataSource.reset()
    }

    override fun observeStats(): StateFlow<FpsContentStats> {
        return localDataSource.observeStats()
    }

    override fun observeTicks(): Flow<FrameTick> {
        return localDataSource.observeTicks()
    }
}
