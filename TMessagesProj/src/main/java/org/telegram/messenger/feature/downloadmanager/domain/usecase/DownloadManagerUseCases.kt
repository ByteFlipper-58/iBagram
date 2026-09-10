package org.telegram.messenger.feature.downloadmanager.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.downloadmanager.domain.model.AutoDownloadMediaType
import org.telegram.messenger.feature.downloadmanager.domain.model.AutoDownloadNetwork
import org.telegram.messenger.feature.downloadmanager.domain.model.DownloadItemModel
import org.telegram.messenger.feature.downloadmanager.domain.model.DownloadManagerState
import org.telegram.messenger.feature.downloadmanager.domain.model.DownloadPresetModel
import org.telegram.messenger.feature.downloadmanager.domain.model.PeerTypePreset
import org.telegram.messenger.feature.downloadmanager.domain.repository.DownloadManagerRepository

class EvaluateAutoDownloadEligibilityUseCase(private val repository: DownloadManagerRepository) {
    operator fun invoke(
        mediaType: AutoDownloadMediaType,
        peerType: PeerTypePreset,
        sizeBytes: Long
    ): Boolean {
        return repository.shouldAutoDownload(mediaType, peerType, sizeBytes)
    }
}

class ObserveDownloadManagerStateUseCase(private val repository: DownloadManagerRepository) {
    operator fun invoke(): Flow<DownloadManagerState> = repository.observeState()
}

class GetDownloadManagerStateUseCase(private val repository: DownloadManagerRepository) {
    operator fun invoke(): DownloadManagerState = repository.getState()
}

class EnqueueDownloadUseCase(private val repository: DownloadManagerRepository) {
    operator fun invoke(item: DownloadItemModel): Boolean = repository.enqueueDownload(item)
}

class PauseDownloadUseCase(private val repository: DownloadManagerRepository) {
    operator fun invoke(id: String): Boolean = repository.pauseDownload(id)
}

class ResumeDownloadUseCase(private val repository: DownloadManagerRepository) {
    operator fun invoke(id: String): Boolean = repository.resumeDownload(id)
}

class CancelDownloadUseCase(private val repository: DownloadManagerRepository) {
    operator fun invoke(id: String): Boolean = repository.cancelDownload(id)
}

class RetryDownloadUseCase(private val repository: DownloadManagerRepository) {
    operator fun invoke(id: String): Boolean = repository.retryDownload(id)
}

class ClearRecentDownloadsUseCase(private val repository: DownloadManagerRepository) {
    operator fun invoke() {
        repository.clearRecentDownloads()
    }
}

class MarkDownloadsAsViewedUseCase(private val repository: DownloadManagerRepository) {
    operator fun invoke() {
        repository.markDownloadsAsViewed()
    }
}

class UpdateDownloadProgressUseCase(private val repository: DownloadManagerRepository) {
    operator fun invoke(id: String, downloadedBytes: Long, totalBytes: Long) {
        repository.updateDownloadProgress(id, downloadedBytes, totalBytes)
    }
}

class CalculateDownloadSpeedUseCase {
    operator fun invoke(deltaBytes: Long, durationMs: Long): Long {
        if (durationMs <= 0L || deltaBytes <= 0L) return 0L
        return (deltaBytes * 1000L) / durationMs
    }
}

class SetDownloadNetworkTypeUseCase(private val repository: DownloadManagerRepository) {
    operator fun invoke(network: AutoDownloadNetwork) {
        repository.setNetworkType(network)
    }
}

class UpdateDownloadPresetUseCase(private val repository: DownloadManagerRepository) {
    operator fun invoke(network: AutoDownloadNetwork, preset: DownloadPresetModel) {
        repository.updatePreset(network, preset)
    }
}
