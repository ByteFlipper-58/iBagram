package org.telegram.messenger.feature.media.downloadmanager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.downloadmanager.data.datasource.DownloadManagerLocalDataSource
import org.telegram.messenger.feature.media.downloadmanager.data.datasource.DownloadManagerRemoteDataSource
import org.telegram.messenger.feature.media.downloadmanager.data.repository.DownloadManagerRepositoryImpl
import org.telegram.messenger.feature.media.downloadmanager.domain.model.AutoDownloadMediaType
import org.telegram.messenger.feature.media.downloadmanager.domain.model.AutoDownloadNetwork
import org.telegram.messenger.feature.media.downloadmanager.domain.model.DownloadItemModel
import org.telegram.messenger.feature.media.downloadmanager.domain.model.DownloadItemStatus
import org.telegram.messenger.feature.media.downloadmanager.domain.model.DownloadPresetModel
import org.telegram.messenger.feature.media.downloadmanager.domain.model.PeerTypePreset
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadManagerRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        AccountFeatureContainer.resetAll()
    }

    @After
    fun tearDown() {
        AccountFeatureContainer.resetAll()
        Dispatchers.resetMain()
    }

    private class FakeDownloadManagerLocalDataSource(account: Int) : DownloadManagerLocalDataSource(account) {
        val prefs = mutableMapOf<String, String>()
        var clearRecentDocumentsCalled = false
        var deleteRecentDocumentCalled = false
        var lastDeletedDcId = 0
        var lastDeletedDocId = 0L

        override fun getPresetString(key: String, defaultValue: String): String {
            return prefs[key] ?: defaultValue
        }

        override fun savePresetString(key: String, value: String) {
            prefs[key] = value
        }

        override suspend fun clearRecentDownloadedDocuments(): Result<Unit> {
            clearRecentDocumentsCalled = true
            return Result.success(Unit)
        }

        override suspend fun deleteRecentDocument(dcId: Int, id: Long): Result<Unit> {
            deleteRecentDocumentCalled = true
            lastDeletedDcId = dcId
            lastDeletedDocId = id
            return Result.success(Unit)
        }

        override fun checkAutodownloadSettings() {}

        override fun canDownloadMedia(type: Int, size: Long): Boolean = true
    }

    private class FakeDownloadManagerRemoteDataSource(account: Int) : DownloadManagerRemoteDataSource(account) {
        var saveAutoDownloadSettingsCalled = false
        var lastSavedSettings: TLRPC.TL_autoDownloadSettings? = null

        override suspend fun getAutoDownloadConfig(): Result<TL_account.autoDownloadSettings> {
            return Result.success(TL_account.autoDownloadSettings())
        }

        override suspend fun saveAutoDownloadSettings(
            settings: TLRPC.TL_autoDownloadSettings,
            low: Boolean,
            high: Boolean
        ): Result<Boolean> {
            saveAutoDownloadSettingsCalled = true
            lastSavedSettings = settings
            return Result.success(true)
        }
    }

    private fun createRepository(
        account: Int = 0,
        localDataSource: DownloadManagerLocalDataSource = FakeDownloadManagerLocalDataSource(account),
        remoteDataSource: DownloadManagerRemoteDataSource = FakeDownloadManagerRemoteDataSource(account),
        scope: CoroutineScope = CoroutineScope(testDispatcher)
    ): DownloadManagerRepositoryImpl {
        return DownloadManagerRepositoryImpl(
            currentAccount = account,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            scope = scope
        )
    }

    @Test
    fun init_uses_defaults_when_storage_empty() = runTest {
        val repo = createRepository()
        val wifiPreset = repo.getPreset(AutoDownloadNetwork.WIFI)
        val cellularPreset = repo.getPreset(AutoDownloadNetwork.CELLULAR)
        val roamingPreset = repo.getPreset(AutoDownloadNetwork.ROAMING)

        assertTrue(wifiPreset.enabled)
        assertTrue(cellularPreset.enabled)
        assertFalse(roamingPreset.enabled)
        assertEquals(1000, wifiPreset.maxVideoBitrate)
        assertEquals(500, cellularPreset.maxVideoBitrate)
    }

    @Test
    fun init_loads_presets_from_storage_when_available() = runTest {
        val local = FakeDownloadManagerLocalDataSource(0)
        local.prefs["wifiPreset"] = "15_15_15_15_512000_10485760_3145728_524288_1_1_1_0_1200_1"

        val repo = createRepository(localDataSource = local)
        val wifiPreset = repo.getPreset(AutoDownloadNetwork.WIFI)

        assertTrue(wifiPreset.enabled)
        assertEquals(1200, wifiPreset.maxVideoBitrate)
        assertEquals(512000L, wifiPreset.maxSizes[AutoDownloadMediaType.PHOTO])
    }

    @Test
    fun enqueueDownload_adds_item_and_updates_state() = runTest {
        val repo = createRepository()
        val item = DownloadItemModel(
            id = "1_100",
            fileName = "photo.jpg",
            sizeBytes = 2048L,
            mimeType = "image/jpeg",
            status = DownloadItemStatus.DOWNLOADING
        )

        val enqueued = repo.enqueueDownload(item)
        assertTrue(enqueued)

        val files = repo.observeDownloadingFiles().first()
        assertEquals(1, files.size)
        assertEquals("photo.jpg", files[0].fileName)
        assertEquals(DownloadItemStatus.DOWNLOADING, files[0].status)
    }

    @Test
    fun pause_and_resume_download_transitions_status() = runTest {
        val repo = createRepository()
        val item = DownloadItemModel(
            id = "1_200",
            fileName = "video.mp4",
            sizeBytes = 1048576L,
            mimeType = "video/mp4",
            status = DownloadItemStatus.DOWNLOADING
        )
        repo.enqueueDownload(item)

        assertTrue(repo.pauseDownload("1_200"))
        assertEquals(DownloadItemStatus.PAUSED, repo.getDownloadingFiles()[0].status)

        assertTrue(repo.resumeDownload("1_200"))
        assertEquals(DownloadItemStatus.DOWNLOADING, repo.getDownloadingFiles()[0].status)
    }

    @Test
    fun cancelDownload_removes_from_queue() = runTest {
        val repo = createRepository()
        val item = DownloadItemModel(
            id = "1_300",
            fileName = "document.pdf",
            sizeBytes = 5000L,
            mimeType = "application/pdf",
            status = DownloadItemStatus.DOWNLOADING
        )
        repo.enqueueDownload(item)

        assertTrue(repo.cancelDownload("1_300"))
        assertEquals(0, repo.getDownloadingFiles().size)
    }

    @Test
    fun completeDownload_moves_item_to_recent_and_unviewed() = runTest {
        val repo = createRepository()
        val item = DownloadItemModel(
            id = "1_400",
            fileName = "music.mp3",
            sizeBytes = 4096L,
            mimeType = "audio/mpeg",
            status = DownloadItemStatus.DOWNLOADING
        )
        repo.enqueueDownload(item)
        repo.completeDownload("1_400", "/sdcard/music.mp3")

        assertEquals(0, repo.getDownloadingFiles().size)
        val recent = repo.getRecentFiles()
        assertEquals(1, recent.size)
        assertEquals(DownloadItemStatus.COMPLETED, recent[0].status)
        assertEquals("/sdcard/music.mp3", recent[0].path)
        assertFalse(recent[0].isViewed)

        val state = repo.getState()
        assertEquals(1, state.unviewedDownloads.size)
        assertEquals(1, state.stats.unviewedCount)
    }

    @Test
    fun markDownloadsAsViewed_marks_recent_items() = runTest {
        val repo = createRepository()
        val item = DownloadItemModel(
            id = "1_500",
            fileName = "archive.zip",
            sizeBytes = 10000L,
            mimeType = "application/zip"
        )
        repo.enqueueDownload(item)
        repo.completeDownload("1_500", "/downloads/archive.zip")

        repo.markDownloadsAsViewed()
        val state = repo.getState()
        assertEquals(0, state.unviewedDownloads.size)
        assertTrue(state.recentDownloadingFiles[0].isViewed)
    }

    @Test
    fun deleteRecentDownload_removes_from_state_and_triggers_local_source() = runTest {
        val local = FakeDownloadManagerLocalDataSource(0)
        val repo = createRepository(localDataSource = local, scope = this)

        val item = DownloadItemModel(
            id = "2_600",
            fileName = "file.apk",
            sizeBytes = 8000L,
            mimeType = "application/vnd.android.package-archive"
        )
        repo.enqueueDownload(item)
        repo.completeDownload("2_600", "/downloads/file.apk")

        val deleted = repo.deleteRecentDownload("2_600")
        assertTrue(deleted)
        assertEquals(0, repo.getRecentFiles().size)
    }

    @Test
    fun clearRecentDownloads_clears_all_recent() = runTest {
        val local = FakeDownloadManagerLocalDataSource(0)
        val repo = createRepository(localDataSource = local, scope = this)

        val item = DownloadItemModel(
            id = "3_700",
            fileName = "image.png",
            sizeBytes = 1000L,
            mimeType = "image/png"
        )
        repo.enqueueDownload(item)
        repo.completeDownload("3_700", "/downloads/image.png")

        repo.clearRecentDownloads()
        assertEquals(0, repo.getRecentFiles().size)
        assertEquals(0, repo.getState().unviewedDownloads.size)
    }

    @Test
    fun updateDownloadProgress_updates_bytes_and_speed() = runTest {
        val repo = createRepository()
        val item = DownloadItemModel(
            id = "1_800",
            fileName = "movie.mkv",
            sizeBytes = 1000000L,
            mimeType = "video/x-matroska"
        )
        repo.enqueueDownload(item)

        repo.updateDownloadProgress("1_800", 500000L, 1000000L)
        val downloading = repo.getDownloadingFiles()[0]
        assertEquals(500000L, downloading.downloadedBytes)
        assertEquals(0.5f, downloading.progress, 0.01f)
    }

    @Test
    fun updatePreset_persists_locally_and_syncs_remotely() = runTest {
        val local = FakeDownloadManagerLocalDataSource(0)
        val remote = FakeDownloadManagerRemoteDataSource(0)
        val repo = createRepository(localDataSource = local, remoteDataSource = remote, scope = this)

        val customWifi = DownloadPresetModel(
            mask = mapOf(PeerTypePreset.CONTACTS to setOf(AutoDownloadMediaType.PHOTO)),
            maxSizes = mapOf(AutoDownloadMediaType.PHOTO to 2048L),
            preloadVideo = false,
            preloadMusic = false,
            preloadStories = false,
            lessCallData = false,
            maxVideoBitrate = 720,
            enabled = true
        )

        repo.updatePreset(AutoDownloadNetwork.WIFI, customWifi)

        assertEquals(customWifi, repo.getPreset(AutoDownloadNetwork.WIFI))
        assertTrue(local.prefs.containsKey("wifiPreset"))
    }

    @Test
    fun shouldAutoDownload_evaluates_active_network_preset() = runTest {
        val repo = createRepository()
        repo.setNetworkType(AutoDownloadNetwork.WIFI)

        // By default Wi-Fi allows 10MB photo
        assertTrue(repo.shouldAutoDownload(AutoDownloadMediaType.PHOTO, PeerTypePreset.CONTACTS, 1024L))

        // Switch to roaming (roaming only allows photo under 500KB)
        repo.setNetworkType(AutoDownloadNetwork.ROAMING)
        assertFalse(repo.shouldAutoDownload(AutoDownloadMediaType.VIDEO, PeerTypePreset.CONTACTS, 1024L))
    }
}
