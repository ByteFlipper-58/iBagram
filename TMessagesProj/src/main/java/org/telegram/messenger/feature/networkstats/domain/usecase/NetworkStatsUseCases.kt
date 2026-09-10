package org.telegram.messenger.feature.networkstats.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.networkstats.domain.model.NetworkStatsSummaryModel
import org.telegram.messenger.feature.networkstats.domain.model.NetworkType
import org.telegram.messenger.feature.networkstats.domain.model.TrafficCategory
import org.telegram.messenger.feature.networkstats.domain.model.TrafficItemModel
import org.telegram.messenger.feature.networkstats.domain.repository.NetworkStatsRepository
import java.util.Locale

class ObserveNetworkStatsUseCase(
    private val repository: NetworkStatsRepository
) {
    operator fun invoke(networkType: NetworkType): Flow<NetworkStatsSummaryModel> {
        return repository.observeStats(networkType)
    }
}

class ObserveAllNetworkStatsUseCase(
    private val repository: NetworkStatsRepository
) {
    operator fun invoke(): Flow<Map<NetworkType, NetworkStatsSummaryModel>> {
        return repository.observeAllStats()
    }
}

class GetNetworkStatsUseCase(
    private val repository: NetworkStatsRepository
) {
    suspend operator fun invoke(networkType: NetworkType): NetworkStatsSummaryModel {
        return repository.getStats(networkType)
    }
}

class GetAllNetworkStatsUseCase(
    private val repository: NetworkStatsRepository
) {
    suspend operator fun invoke(): Map<NetworkType, NetworkStatsSummaryModel> {
        return repository.getAllStats()
    }
}

class IncrementTrafficBytesUseCase(
    private val repository: NetworkStatsRepository
) {
    suspend operator fun invoke(
        networkType: NetworkType,
        category: TrafficCategory,
        sentBytes: Long = 0L,
        receivedBytes: Long = 0L
    ) {
        if (sentBytes > 0L) {
            repository.incrementSentBytes(networkType, category, sentBytes)
        }
        if (receivedBytes > 0L) {
            repository.incrementReceivedBytes(networkType, category, receivedBytes)
        }
    }
}

class IncrementTrafficItemsUseCase(
    private val repository: NetworkStatsRepository
) {
    suspend operator fun invoke(
        networkType: NetworkType,
        category: TrafficCategory,
        sentItems: Int = 0,
        receivedItems: Int = 0
    ) {
        if (sentItems > 0) {
            repository.incrementSentItems(networkType, category, sentItems)
        }
        if (receivedItems > 0) {
            repository.incrementReceivedItems(networkType, category, receivedItems)
        }
    }
}

class IncrementCallsTimeUseCase(
    private val repository: NetworkStatsRepository
) {
    suspend operator fun invoke(networkType: NetworkType, seconds: Int) {
        if (seconds > 0) {
            repository.incrementCallsTotalTime(networkType, seconds)
        }
    }
}

class ResetNetworkStatsUseCase(
    private val repository: NetworkStatsRepository
) {
    suspend operator fun invoke(networkType: NetworkType) {
        repository.resetStats(networkType)
    }
}

class RefreshNetworkStatsUseCase(
    private val repository: NetworkStatsRepository
) {
    suspend operator fun invoke() {
        repository.refreshStats()
    }
}

class CalculateMessagesTrafficUseCase {
    operator fun invoke(rawItems: Map<TrafficCategory, TrafficItemModel>): TrafficItemModel {
        val total = rawItems[TrafficCategory.TOTAL] ?: TrafficItemModel(TrafficCategory.TOTAL)
        val files = rawItems[TrafficCategory.FILES]?.sentBytes ?: 0L
        val audios = rawItems[TrafficCategory.AUDIOS]?.sentBytes ?: 0L
        val videos = rawItems[TrafficCategory.VIDEOS]?.sentBytes ?: 0L
        val photos = rawItems[TrafficCategory.PHOTOS]?.sentBytes ?: 0L
        val music = rawItems[TrafficCategory.MUSIC]?.sentBytes ?: 0L

        val rFiles = rawItems[TrafficCategory.FILES]?.receivedBytes ?: 0L
        val rAudios = rawItems[TrafficCategory.AUDIOS]?.receivedBytes ?: 0L
        val rVideos = rawItems[TrafficCategory.VIDEOS]?.receivedBytes ?: 0L
        val rPhotos = rawItems[TrafficCategory.PHOTOS]?.receivedBytes ?: 0L
        val rMusic = rawItems[TrafficCategory.MUSIC]?.receivedBytes ?: 0L

        val sentMsgBytes = (total.sentBytes - files - audios - videos - photos - music).coerceAtLeast(0L)
        val recMsgBytes = (total.receivedBytes - rFiles - rAudios - rVideos - rPhotos - rMusic).coerceAtLeast(0L)

        val explicitMessages = rawItems[TrafficCategory.MESSAGES]
        return TrafficItemModel(
            category = TrafficCategory.MESSAGES,
            sentBytes = sentMsgBytes,
            receivedBytes = recMsgBytes,
            sentItems = explicitMessages?.sentItems ?: 0,
            receivedItems = explicitMessages?.receivedItems ?: 0
        )
    }
}

class FormatTrafficBytesUseCase {
    operator fun invoke(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        val tb = gb * 1024.0

        val b = bytes.toDouble()
        return when {
            b >= tb -> String.format(Locale.US, "%.1f TB", b / tb)
            b >= gb -> String.format(Locale.US, "%.1f GB", b / gb)
            b >= mb -> String.format(Locale.US, "%.1f MB", b / mb)
            b >= kb -> String.format(Locale.US, "%.1f KB", b / kb)
            else -> "$bytes B"
        }
    }
}

class FormatCallsDurationUseCase {
    operator fun invoke(seconds: Int): String {
        if (seconds <= 0) return "0s"
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m ${secs}s"
            minutes > 0 -> "${minutes}m ${secs}s"
            else -> "${secs}s"
        }
    }
}
