package org.telegram.messenger.feature.downloadmanager.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.telegram.messenger.feature.downloadmanager.domain.model.AutoDownloadMediaType
import org.telegram.messenger.feature.downloadmanager.domain.model.AutoDownloadNetwork
import org.telegram.messenger.feature.downloadmanager.domain.model.DownloadItemModel
import org.telegram.messenger.feature.downloadmanager.domain.model.DownloadItemStatus
import org.telegram.messenger.feature.downloadmanager.domain.model.DownloadManagerState
import org.telegram.messenger.feature.downloadmanager.domain.model.DownloadManagerStats
import org.telegram.messenger.feature.downloadmanager.domain.model.DownloadPresetModel
import org.telegram.messenger.feature.downloadmanager.domain.model.PeerTypePreset
import org.telegram.messenger.feature.downloadmanager.domain.repository.DownloadManagerRepository
import java.util.Collections
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap

class LegacyDownloadManagerRepository(
    private val currentAccount: Int = 0
) : DownloadManagerRepository {

    private val lock = Any()

    private val downloadingMap = Collections.synchronizedMap(LinkedHashMap<String, DownloadItemModel>())
    private val recentMap = Collections.synchronizedMap(LinkedHashMap<String, DownloadItemModel>())
    private val unviewedMap = Collections.synchronizedMap(LinkedHashMap<String, DownloadItemModel>())

    private val presetsMap = ConcurrentHashMap<AutoDownloadNetwork, DownloadPresetModel>()
    private var currentNetwork: AutoDownloadNetwork = AutoDownloadNetwork.WIFI

    private var lastSpeedCalcTime = 0L
    private var lastBytesCount = 0L
    private var currentSpeed = 0L

    private val _state = MutableStateFlow(DownloadManagerState())
    override fun observeState(): Flow<DownloadManagerState> = _state.asStateFlow()
    override fun getState(): DownloadManagerState = _state.value

    override fun observeDownloadingFiles(): Flow<List<DownloadItemModel>> =
        _state.map { it.downloadingFiles }

    override fun getDownloadingFiles(): List<DownloadItemModel> =
        _state.value.downloadingFiles

    override fun observeRecentFiles(): Flow<List<DownloadItemModel>> =
        _state.map { it.recentDownloadingFiles }

    override fun getRecentFiles(): List<DownloadItemModel> =
        _state.value.recentDownloadingFiles

    init {
        initDefaultPresets()
        publishState()
    }

    private fun initDefaultPresets() {
        val allTypes = setOf(
            AutoDownloadMediaType.PHOTO,
            AutoDownloadMediaType.VIDEO,
            AutoDownloadMediaType.DOCUMENT,
            AutoDownloadMediaType.AUDIO
        )
        val photoOnly = setOf(AutoDownloadMediaType.PHOTO)

        val fullMask = PeerTypePreset.values().associateWith { allTypes }
        val photoMask = PeerTypePreset.values().associateWith { photoOnly }

        val wifiSizes = mapOf(
            AutoDownloadMediaType.PHOTO to (10L * 1024L * 1024L),
            AutoDownloadMediaType.VIDEO to (15L * 1024L * 1024L),
            AutoDownloadMediaType.DOCUMENT to (3L * 1024L * 1024L),
            AutoDownloadMediaType.AUDIO to (10L * 1024L * 1024L)
        )

        val cellularSizes = mapOf(
            AutoDownloadMediaType.PHOTO to (3L * 1024L * 1024L),
            AutoDownloadMediaType.VIDEO to (5L * 1024L * 1024L),
            AutoDownloadMediaType.DOCUMENT to (1L * 1024L * 1024L),
            AutoDownloadMediaType.AUDIO to (1L * 1024L * 1024L)
        )

        val roamingSizes = mapOf(
            AutoDownloadMediaType.PHOTO to (500L * 1024L),
            AutoDownloadMediaType.VIDEO to 0L,
            AutoDownloadMediaType.DOCUMENT to 0L,
            AutoDownloadMediaType.AUDIO to 0L
        )

        presetsMap[AutoDownloadNetwork.WIFI] = DownloadPresetModel(
            mask = fullMask,
            maxSizes = wifiSizes,
            preloadVideo = true,
            preloadMusic = true,
            preloadStories = true,
            lessCallData = false,
            maxVideoBitrate = 1000,
            enabled = true
        )

        presetsMap[AutoDownloadNetwork.CELLULAR] = DownloadPresetModel(
            mask = fullMask,
            maxSizes = cellularSizes,
            preloadVideo = false,
            preloadMusic = true,
            preloadStories = true,
            lessCallData = true,
            maxVideoBitrate = 500,
            enabled = true
        )

        presetsMap[AutoDownloadNetwork.ROAMING] = DownloadPresetModel(
            mask = photoMask,
            maxSizes = roamingSizes,
            preloadVideo = false,
            preloadMusic = false,
            preloadStories = false,
            lessCallData = true,
            maxVideoBitrate = 300,
            enabled = false
        )
    }

    override fun enqueueDownload(item: DownloadItemModel): Boolean {
        synchronized(lock) {
            downloadingMap[item.id] = item
            publishState()
            return true
        }
    }

    override fun pauseDownload(id: String): Boolean {
        synchronized(lock) {
            val item = downloadingMap[id] ?: return false
            if (item.status == DownloadItemStatus.DOWNLOADING || item.status == DownloadItemStatus.QUEUED) {
                downloadingMap[id] = item.copy(status = DownloadItemStatus.PAUSED)
                publishState()
                return true
            }
            return false
        }
    }

    override fun resumeDownload(id: String): Boolean {
        synchronized(lock) {
            val item = downloadingMap[id] ?: return false
            if (item.status == DownloadItemStatus.PAUSED || item.status == DownloadItemStatus.FAILED) {
                downloadingMap[id] = item.copy(status = DownloadItemStatus.DOWNLOADING)
                publishState()
                return true
            }
            return false
        }
    }

    override fun cancelDownload(id: String): Boolean {
        synchronized(lock) {
            val item = downloadingMap.remove(id) ?: return false
            unviewedMap.remove(id)
            publishState()
            return true
        }
    }

    override fun retryDownload(id: String): Boolean {
        synchronized(lock) {
            val item = downloadingMap[id] ?: return false
            downloadingMap[id] = item.copy(status = DownloadItemStatus.DOWNLOADING, progress = 0f, downloadedBytes = 0L)
            publishState()
            return true
        }
    }

    override fun deleteRecentDownload(id: String): Boolean {
        synchronized(lock) {
            val removed = recentMap.remove(id) != null
            if (removed) {
                unviewedMap.remove(id)
                publishState()
            }
            return removed
        }
    }

    override fun clearRecentDownloads() {
        synchronized(lock) {
            recentMap.clear()
            unviewedMap.clear()
            publishState()
        }
    }

    override fun markDownloadsAsViewed() {
        synchronized(lock) {
            for (key in unviewedMap.keys) {
                val item = unviewedMap[key]
                if (item != null) {
                    recentMap[key] = item.copy(isViewed = true)
                }
            }
            unviewedMap.clear()
            publishState()
        }
    }

    override fun updateDownloadProgress(id: String, downloadedBytes: Long, totalBytes: Long) {
        synchronized(lock) {
            val item = downloadingMap[id] ?: return
            val progress = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
            downloadingMap[id] = item.copy(
                status = DownloadItemStatus.DOWNLOADING,
                downloadedBytes = downloadedBytes,
                sizeBytes = if (totalBytes > 0) totalBytes else item.sizeBytes,
                progress = progress
            )

            // Speed estimation
            val now = System.currentTimeMillis()
            if (lastSpeedCalcTime == 0L) {
                lastSpeedCalcTime = now
                lastBytesCount = downloadedBytes
            } else {
                val dt = now - lastSpeedCalcTime
                if (dt >= 500L) {
                    val db = (downloadedBytes - lastBytesCount).coerceAtLeast(0L)
                    currentSpeed = (db * 1000L) / dt
                    lastSpeedCalcTime = now
                    lastBytesCount = downloadedBytes
                }
            }

            publishState()
        }
    }

    override fun completeDownload(id: String, path: String) {
        synchronized(lock) {
            val item = downloadingMap.remove(id) ?: return
            val completedItem = item.copy(
                status = DownloadItemStatus.COMPLETED,
                path = path,
                downloadedBytes = item.sizeBytes,
                progress = 1.0f,
                isViewed = false,
                timestamp = System.currentTimeMillis()
            )
            recentMap[id] = completedItem
            unviewedMap[id] = completedItem
            publishState()
        }
    }

    override fun failDownload(id: String, canceled: Boolean) {
        synchronized(lock) {
            val item = downloadingMap[id] ?: return
            val newStatus = if (canceled) DownloadItemStatus.CANCELLED else DownloadItemStatus.FAILED
            downloadingMap[id] = item.copy(status = newStatus)
            publishState()
        }
    }

    override fun setNetworkType(network: AutoDownloadNetwork) {
        synchronized(lock) {
            currentNetwork = network
            publishState()
        }
    }

    override fun shouldAutoDownload(
        mediaType: AutoDownloadMediaType,
        peerType: PeerTypePreset,
        sizeBytes: Long
    ): Boolean {
        val preset = getPreset(currentNetwork)
        return preset.isMediaAllowed(peerType, mediaType, sizeBytes)
    }

    override fun getPreset(network: AutoDownloadNetwork): DownloadPresetModel {
        return presetsMap[network] ?: DownloadPresetModel()
    }

    override fun updatePreset(network: AutoDownloadNetwork, preset: DownloadPresetModel) {
        presetsMap[network] = preset
        publishState()
    }

    private fun publishState() {
        val downloading = downloadingMap.values.toList()
        val recent = recentMap.values.toList().reversed()
        val unviewed = unviewedMap.values.toList()

        var totalBytesDownloaded = 0L
        for (item in recent) {
            totalBytesDownloaded += item.downloadedBytes
        }
        for (item in downloading) {
            totalBytesDownloaded += item.downloadedBytes
        }

        val stats = DownloadManagerStats(
            totalActiveDownloads = downloading.count { it.status == DownloadItemStatus.DOWNLOADING || it.status == DownloadItemStatus.QUEUED },
            totalCompletedDownloads = recent.size,
            unviewedCount = unviewed.size,
            totalBytesDownloaded = totalBytesDownloaded,
            currentDownloadSpeedBytesPerSec = currentSpeed
        )

        _state.value = DownloadManagerState(
            downloadingFiles = downloading,
            recentDownloadingFiles = recent,
            unviewedDownloads = unviewed,
            currentNetwork = currentNetwork,
            activePreset = getPreset(currentNetwork),
            stats = stats
        )
    }
}
