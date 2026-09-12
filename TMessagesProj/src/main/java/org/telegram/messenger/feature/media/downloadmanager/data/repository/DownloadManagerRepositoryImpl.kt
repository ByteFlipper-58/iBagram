package org.telegram.messenger.feature.media.downloadmanager.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.feature.media.downloadmanager.data.datasource.DownloadManagerLocalDataSource
import org.telegram.messenger.feature.media.downloadmanager.data.datasource.DownloadManagerRemoteDataSource
import org.telegram.messenger.feature.media.downloadmanager.data.mapper.DownloadManagerMapper
import org.telegram.messenger.feature.media.downloadmanager.domain.model.AutoDownloadMediaType
import org.telegram.messenger.feature.media.downloadmanager.domain.model.AutoDownloadNetwork
import org.telegram.messenger.feature.media.downloadmanager.domain.model.DownloadItemModel
import org.telegram.messenger.feature.media.downloadmanager.domain.model.DownloadItemStatus
import org.telegram.messenger.feature.media.downloadmanager.domain.model.DownloadManagerState
import org.telegram.messenger.feature.media.downloadmanager.domain.model.DownloadManagerStats
import org.telegram.messenger.feature.media.downloadmanager.domain.model.DownloadPresetModel
import org.telegram.messenger.feature.media.downloadmanager.domain.model.PeerTypePreset
import org.telegram.messenger.feature.media.downloadmanager.domain.repository.DownloadManagerRepository
import org.telegram.tgnet.TLRPC
import java.util.Collections
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * Clean domain repository implementation coordinating local storage/cache and
 * remote MTProto auto-download settings.
 */
class DownloadManagerRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: DownloadManagerLocalDataSource,
    private val remoteDataSource: DownloadManagerRemoteDataSource,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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
        initPresetsFromStorage()
        setupNotificationCenterListeners()
        publishState()
    }

    private fun initPresetsFromStorage() {
        val mobileStr = localDataSource.getPresetString("mobilePreset")
        val wifiStr = localDataSource.getPresetString("wifiPreset")
        val roamingStr = localDataSource.getPresetString("roamingPreset")

        if (mobileStr.isNotBlank() && mobileStr.split("_").size >= 11) {
            presetsMap[AutoDownloadNetwork.CELLULAR] = DownloadManagerMapper.parsePresetString(mobileStr)
        } else {
            initDefaultCellularPreset()
        }

        if (wifiStr.isNotBlank() && wifiStr.split("_").size >= 11) {
            presetsMap[AutoDownloadNetwork.WIFI] = DownloadManagerMapper.parsePresetString(wifiStr)
        } else {
            initDefaultWifiPreset()
        }

        if (roamingStr.isNotBlank() && roamingStr.split("_").size >= 11) {
            presetsMap[AutoDownloadNetwork.ROAMING] = DownloadManagerMapper.parsePresetString(roamingStr)
        } else {
            initDefaultRoamingPreset()
        }
    }

    private fun initDefaultWifiPreset() {
        val allTypes = setOf(
            AutoDownloadMediaType.PHOTO,
            AutoDownloadMediaType.VIDEO,
            AutoDownloadMediaType.DOCUMENT,
            AutoDownloadMediaType.AUDIO
        )
        val fullMask = PeerTypePreset.values().associateWith { allTypes }
        val wifiSizes = mapOf(
            AutoDownloadMediaType.PHOTO to (10L * 1024L * 1024L),
            AutoDownloadMediaType.VIDEO to (15L * 1024L * 1024L),
            AutoDownloadMediaType.DOCUMENT to (3L * 1024L * 1024L),
            AutoDownloadMediaType.AUDIO to (10L * 1024L * 1024L)
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
    }

    private fun initDefaultCellularPreset() {
        val allTypes = setOf(
            AutoDownloadMediaType.PHOTO,
            AutoDownloadMediaType.VIDEO,
            AutoDownloadMediaType.DOCUMENT,
            AutoDownloadMediaType.AUDIO
        )
        val fullMask = PeerTypePreset.values().associateWith { allTypes }
        val cellularSizes = mapOf(
            AutoDownloadMediaType.PHOTO to (3L * 1024L * 1024L),
            AutoDownloadMediaType.VIDEO to (5L * 1024L * 1024L),
            AutoDownloadMediaType.DOCUMENT to (1L * 1024L * 1024L),
            AutoDownloadMediaType.AUDIO to (1L * 1024L * 1024L)
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
    }

    private fun initDefaultRoamingPreset() {
        val photoOnly = setOf(AutoDownloadMediaType.PHOTO)
        val photoMask = PeerTypePreset.values().associateWith { photoOnly }
        val roamingSizes = mapOf(
            AutoDownloadMediaType.PHOTO to (500L * 1024L),
            AutoDownloadMediaType.VIDEO to 0L,
            AutoDownloadMediaType.DOCUMENT to 0L,
            AutoDownloadMediaType.AUDIO to 0L
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

    private fun setupNotificationCenterListeners() {
        NotificationCenterFlowBridge.runOnMainThread {
            try {
                val nc = NotificationCenter.getInstance(currentAccount) ?: return@runOnMainThread
                val delegate = NotificationCenter.NotificationCenterDelegate { id, _, _ ->
                    when (id) {
                        NotificationCenter.onDownloadingFilesChanged,
                        NotificationCenter.fileLoaded,
                        NotificationCenter.fileLoadFailed -> {
                            publishState()
                        }
                    }
                }
                nc.addObserver(delegate, NotificationCenter.onDownloadingFilesChanged)
                nc.addObserver(delegate, NotificationCenter.fileLoaded)
                nc.addObserver(delegate, NotificationCenter.fileLoadFailed)
            } catch (_: Throwable) {
                // Safely handled in headless or test environments
            }
        }
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
            scope.launch {
                val parts = id.split("_")
                val dcId = parts.getOrNull(0)?.toIntOrNull() ?: 0
                val docId = parts.getOrNull(1)?.toLongOrNull() ?: 0L
                if (dcId != 0 && docId != 0L) {
                    localDataSource.deleteRecentDocument(dcId, docId)
                }
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
        scope.launch {
            localDataSource.clearRecentFiles()
            localDataSource.clearRecentDownloadedDocuments()
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
        val key = when (network) {
            AutoDownloadNetwork.WIFI -> "wifiPreset"
            AutoDownloadNetwork.CELLULAR -> "mobilePreset"
            AutoDownloadNetwork.ROAMING -> "roamingPreset"
        }
        val encodedStr = DownloadManagerMapper.presetToString(preset)
        localDataSource.savePresetString(key, encodedStr)
        localDataSource.checkAutodownloadSettings()

        scope.launch {
            val tlSettings = TLRPC.TL_autoDownloadSettings().apply {
                this.audio_preload_next = preset.preloadMusic
                this.video_preload_large = preset.preloadVideo
                this.phonecalls_less_data = preset.lessCallData
                this.video_upload_maxbitrate = preset.maxVideoBitrate
                this.disabled = !preset.enabled
                this.photo_size_max = preset.maxSizes[AutoDownloadMediaType.PHOTO]?.toInt() ?: 0
                this.video_size_max = preset.maxSizes[AutoDownloadMediaType.VIDEO] ?: 0L
                this.file_size_max = preset.maxSizes[AutoDownloadMediaType.DOCUMENT] ?: 0L
            }
            remoteDataSource.saveAutoDownloadSettings(tlSettings)
        }

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
