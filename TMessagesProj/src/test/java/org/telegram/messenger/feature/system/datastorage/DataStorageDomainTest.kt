package org.telegram.messenger.feature.system.datastorage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.telegram.messenger.DownloadController
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.datastorage.data.mapper.DataStorageMapper
import org.telegram.messenger.feature.system.datastorage.domain.model.AutoDownloadNetworkType
import org.telegram.messenger.feature.system.datastorage.domain.model.AutoDownloadPresetModel
import org.telegram.messenger.feature.system.datastorage.domain.model.KeepMediaSettingsModel
import org.telegram.messenger.feature.system.datastorage.domain.model.NetworkUsageModel
import org.telegram.messenger.feature.system.datastorage.domain.model.NetworkUsageType
import org.telegram.messenger.feature.system.datastorage.domain.model.StorageUsageModel
import org.telegram.messenger.feature.system.datastorage.domain.repository.DataStorageRepository
import org.telegram.messenger.feature.system.datastorage.domain.usecase.ClearCacheUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.ClearDatabaseUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.GetAutoDownloadPresetUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.GetKeepMediaSettingsUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.GetNetworkUsageUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.GetStorageUsageUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.ObserveAutoDownloadPresetUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.ObserveKeepMediaSettingsUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.ObserveNetworkUsageUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.ObserveStorageUsageUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.RefreshStorageUsageUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.ResetNetworkUsageUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.UpdateAutoDownloadPresetUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.UpdateKeepMediaUseCase
import org.telegram.messenger.feature.system.datastorage.presentation.DataStorageEvent
import org.telegram.messenger.feature.system.datastorage.presentation.DataStorageViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class DataStorageDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDomainModels() {
        val netUsage = NetworkUsageModel(
            networkType = NetworkUsageType.WIFI,
            bytesSent = 1000L,
            bytesReceived = 4000L,
            messagesSentBytes = 200L,
            messagesReceivedBytes = 300L,
            photosSentBytes = 500L,
            photosReceivedBytes = 1500L,
            videosSentBytes = 300L,
            videosReceivedBytes = 2200L,
            callsTotalTimeSeconds = 120,
            resetDate = 1700000000L
        )
        assertEquals(NetworkUsageType.WIFI, netUsage.networkType)
        assertEquals(5000L, netUsage.totalBytes)
        assertEquals(1000L, netUsage.bytesSent)
        assertEquals(4000L, netUsage.bytesReceived)
        assertEquals(120, netUsage.callsTotalTimeSeconds)
        assertEquals(1700000000L, netUsage.resetDate)

        val storageUsage = StorageUsageModel(
            photosBytes = 100L,
            videosBytes = 200L,
            documentsBytes = 300L,
            audioBytes = 50L,
            musicBytes = 150L,
            stickersBytes = 40L,
            storiesBytes = 60L,
            otherBytes = 10L,
            cacheTempBytes = 20L,
            databaseBytes = 500L,
            totalDeviceBytes = 10000L,
            totalDeviceFreeBytes = 5000L
        )
        assertEquals(930L, storageUsage.totalCacheBytes)
        assertEquals(1430L, storageUsage.totalTelegramBytes)
        assertEquals(10000L, storageUsage.totalDeviceBytes)
        assertEquals(5000L, storageUsage.totalDeviceFreeBytes)

        val preset = AutoDownloadPresetModel(
            networkType = AutoDownloadNetworkType.MOBILE,
            isEnabled = true,
            downloadPhotos = true,
            downloadVideos = false,
            downloadDocuments = true,
            maxVideoSize = 5000L,
            maxDocumentSize = 2000L,
            preloadVideo = false,
            preloadMusic = true,
            preloadStories = true,
            lessCallData = false
        )
        assertEquals(AutoDownloadNetworkType.MOBILE, preset.networkType)
        assertTrue(preset.isEnabled)
        assertTrue(preset.downloadPhotos)
        assertFalse(preset.downloadVideos)
        assertTrue(preset.downloadDocuments)
        assertEquals(5000L, preset.maxVideoSize)
        assertEquals(2000L, preset.maxDocumentSize)

        val keepMedia = KeepMediaSettingsModel(
            keepMediaUser = 2,
            keepMediaGroup = 1,
            keepMediaChannel = 0,
            keepMediaStories = 6
        )
        assertEquals(2, keepMedia.keepMediaUser)
        assertEquals(1, keepMedia.keepMediaGroup)
        assertEquals(0, keepMedia.keepMediaChannel)
        assertEquals(6, keepMedia.keepMediaStories)
    }

    @Test
    fun testDataStorageMapper() {
        val preset = DownloadController.Preset(
            intArrayOf(
                DownloadController.AUTODOWNLOAD_TYPE_PHOTO or DownloadController.AUTODOWNLOAD_TYPE_VIDEO,
                DownloadController.AUTODOWNLOAD_TYPE_PHOTO or DownloadController.AUTODOWNLOAD_TYPE_VIDEO,
                DownloadController.AUTODOWNLOAD_TYPE_PHOTO or DownloadController.AUTODOWNLOAD_TYPE_VIDEO,
                DownloadController.AUTODOWNLOAD_TYPE_PHOTO or DownloadController.AUTODOWNLOAD_TYPE_VIDEO
            ),
            1048576L,
            10485760L,
            1048576L,
            true,
            true,
            true,
            false,
            100,
            true
        )

        val mapped = DataStorageMapper.mapAutoDownloadPreset(preset, AutoDownloadNetworkType.WIFI)
        assertEquals(AutoDownloadNetworkType.WIFI, mapped.networkType)
        assertTrue(mapped.isEnabled)
        assertTrue(mapped.downloadPhotos)
        assertTrue(mapped.downloadVideos)
        assertFalse(mapped.downloadDocuments)
        assertEquals(10485760L, mapped.maxVideoSize)
        assertEquals(1048576L, mapped.maxDocumentSize)
        assertTrue(mapped.preloadVideo)
        assertTrue(mapped.preloadMusic)
        assertTrue(mapped.preloadStories)

        val newModel = AutoDownloadPresetModel(
            networkType = AutoDownloadNetworkType.WIFI,
            isEnabled = false,
            downloadPhotos = true,
            downloadVideos = true,
            downloadDocuments = true,
            maxVideoSize = 20000000L,
            maxDocumentSize = 5000000L,
            preloadVideo = false,
            preloadMusic = false,
            preloadStories = false,
            lessCallData = true
        )
        DataStorageMapper.applyPresetModel(newModel, preset)
        assertFalse(preset.enabled)
        assertEquals(20000000L, preset.sizes[DownloadController.PRESET_SIZE_NUM_VIDEO])
        assertEquals(5000000L, preset.sizes[DownloadController.PRESET_SIZE_NUM_DOCUMENT])
        assertFalse(preset.preloadVideo)
        assertFalse(preset.preloadMusic)
        assertFalse(preset.preloadStories)
        assertTrue(preset.lessCallData)
        assertTrue((preset.mask[0] and DownloadController.AUTODOWNLOAD_TYPE_DOCUMENT) != 0)
    }

    @Test
    fun testUseCasesAndRepository() = runTest {
        val fakeRepo = FakeDataStorageRepository()

        val observeNetworkUsageUseCase = ObserveNetworkUsageUseCase(fakeRepo)
        val observeStorageUsageUseCase = ObserveStorageUsageUseCase(fakeRepo)
        val observeAutoDownloadPresetUseCase = ObserveAutoDownloadPresetUseCase(fakeRepo)
        val observeKeepMediaSettingsUseCase = ObserveKeepMediaSettingsUseCase(fakeRepo)
        val getNetworkUsageUseCase = GetNetworkUsageUseCase(fakeRepo)
        val resetNetworkUsageUseCase = ResetNetworkUsageUseCase(fakeRepo)
        val getStorageUsageUseCase = GetStorageUsageUseCase(fakeRepo)
        val clearCacheUseCase = ClearCacheUseCase(fakeRepo)
        val clearDatabaseUseCase = ClearDatabaseUseCase(fakeRepo)
        val getAutoDownloadPresetUseCase = GetAutoDownloadPresetUseCase(fakeRepo)
        val updateAutoDownloadPresetUseCase = UpdateAutoDownloadPresetUseCase(fakeRepo)
        val getKeepMediaSettingsUseCase = GetKeepMediaSettingsUseCase(fakeRepo)
        val updateKeepMediaUseCase = UpdateKeepMediaUseCase(fakeRepo)
        val refreshStorageUsageUseCase = RefreshStorageUsageUseCase(fakeRepo)

        val netUsage = observeNetworkUsageUseCase(NetworkUsageType.MOBILE).first()
        assertEquals(NetworkUsageType.MOBILE, netUsage.networkType)
        assertEquals(100L, netUsage.bytesSent)

        val storage = observeStorageUsageUseCase().first()
        assertEquals(500L, storage.photosBytes)

        val preset = observeAutoDownloadPresetUseCase(AutoDownloadNetworkType.WIFI).first()
        assertEquals(AutoDownloadNetworkType.WIFI, preset.networkType)

        val keepMedia = observeKeepMediaSettingsUseCase().first()
        assertEquals(2, keepMedia.keepMediaUser)

        val netRes = getNetworkUsageUseCase(NetworkUsageType.MOBILE)
        assertTrue(netRes is Result.Success)
        assertEquals(100L, (netRes as Result.Success).data.bytesSent)

        val resetRes = resetNetworkUsageUseCase(NetworkUsageType.MOBILE)
        assertTrue(resetRes is Result.Success)
        assertTrue(fakeRepo.networkReset)

        val storageRes = getStorageUsageUseCase()
        assertTrue(storageRes is Result.Success)
        assertEquals(500L, (storageRes as Result.Success).data.photosBytes)

        val clearCacheRes = clearCacheUseCase(clearPhotos = true)
        assertTrue(clearCacheRes is Result.Success)
        assertTrue(fakeRepo.cacheCleared)

        val clearDbRes = clearDatabaseUseCase()
        assertTrue(clearDbRes is Result.Success)
        assertTrue(fakeRepo.databaseCleared)

        val presetRes = getAutoDownloadPresetUseCase(AutoDownloadNetworkType.WIFI)
        assertTrue(presetRes is Result.Success)
        assertEquals(AutoDownloadNetworkType.WIFI, (presetRes as Result.Success).data.networkType)

        val updatePresetRes = updateAutoDownloadPresetUseCase(preset.copy(isEnabled = false))
        assertTrue(updatePresetRes is Result.Success)
        assertTrue(fakeRepo.presetUpdated)

        val kmRes = getKeepMediaSettingsUseCase()
        assertTrue(kmRes is Result.Success)
        assertEquals(2, (kmRes as Result.Success).data.keepMediaUser)

        val updateKmRes = updateKeepMediaUseCase(0, 3)
        assertTrue(updateKmRes is Result.Success)
        assertTrue(fakeRepo.keepMediaUpdated)

        val refreshStorageRes = refreshStorageUsageUseCase()
        assertTrue(refreshStorageRes is Result.Success)
        assertTrue(fakeRepo.storageRefreshed)
    }

    @Test
    fun testViewModelStateAndEvents() = runTest {
        val fakeRepo = FakeDataStorageRepository()
        val viewModel = DataStorageViewModel(
            observeNetworkUsageUseCase = ObserveNetworkUsageUseCase(fakeRepo),
            observeStorageUsageUseCase = ObserveStorageUsageUseCase(fakeRepo),
            observeAutoDownloadPresetUseCase = ObserveAutoDownloadPresetUseCase(fakeRepo),
            observeKeepMediaSettingsUseCase = ObserveKeepMediaSettingsUseCase(fakeRepo),
            getNetworkUsageUseCase = GetNetworkUsageUseCase(fakeRepo),
            resetNetworkUsageUseCase = ResetNetworkUsageUseCase(fakeRepo),
            getStorageUsageUseCase = GetStorageUsageUseCase(fakeRepo),
            clearCacheUseCase = ClearCacheUseCase(fakeRepo),
            clearDatabaseUseCase = ClearDatabaseUseCase(fakeRepo),
            getAutoDownloadPresetUseCase = GetAutoDownloadPresetUseCase(fakeRepo),
            updateAutoDownloadPresetUseCase = UpdateAutoDownloadPresetUseCase(fakeRepo),
            getKeepMediaSettingsUseCase = GetKeepMediaSettingsUseCase(fakeRepo),
            updateKeepMediaUseCase = UpdateKeepMediaUseCase(fakeRepo),
            refreshStorageUsageUseCase = RefreshStorageUsageUseCase(fakeRepo)
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(500L, state.storageUsage.photosBytes)
        assertNotNull(state.mobilePreset)
        assertNotNull(state.wifiPreset)
        assertNotNull(state.roamingPreset)
        assertNotNull(state.keepMediaSettings)

        viewModel.onEvent(DataStorageEvent.RefreshStorage)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fakeRepo.storageRefreshed)

        viewModel.onEvent(DataStorageEvent.ResetNetwork(NetworkUsageType.MOBILE))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fakeRepo.networkReset)

        viewModel.onEvent(DataStorageEvent.ClearCache(clearPhotos = true))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fakeRepo.cacheCleared)

        viewModel.onEvent(DataStorageEvent.ClearDatabase)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fakeRepo.databaseCleared)

        viewModel.onEvent(DataStorageEvent.UpdatePreset(AutoDownloadPresetModel(AutoDownloadNetworkType.WIFI, isEnabled = false)))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fakeRepo.presetUpdated)

        viewModel.onEvent(DataStorageEvent.UpdateKeepMedia(chatType = 0, duration = 3))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fakeRepo.keepMediaUpdated)
    }

    private class FakeDataStorageRepository : DataStorageRepository {
        val sampleNet = NetworkUsageModel(networkType = NetworkUsageType.MOBILE, bytesSent = 100L, bytesReceived = 200L)
        val sampleStorage = StorageUsageModel(photosBytes = 500L, totalDeviceBytes = 10000L, totalDeviceFreeBytes = 4000L)
        val samplePresetMobile = AutoDownloadPresetModel(AutoDownloadNetworkType.MOBILE)
        val samplePresetWifi = AutoDownloadPresetModel(AutoDownloadNetworkType.WIFI)
        val samplePresetRoaming = AutoDownloadPresetModel(AutoDownloadNetworkType.ROAMING)
        val sampleKeepMedia = KeepMediaSettingsModel()

        val netFlow = MutableStateFlow(sampleNet)
        val storageFlow = MutableStateFlow(sampleStorage)
        val mobilePresetFlow = MutableStateFlow(samplePresetMobile)
        val wifiPresetFlow = MutableStateFlow(samplePresetWifi)
        val roamingPresetFlow = MutableStateFlow(samplePresetRoaming)
        val keepMediaFlow = MutableStateFlow(sampleKeepMedia)

        var networkReset = false
        var cacheCleared = false
        var databaseCleared = false
        var presetUpdated = false
        var keepMediaUpdated = false
        var storageRefreshed = false

        override fun observeNetworkUsage(type: NetworkUsageType): Flow<NetworkUsageModel> = netFlow
        override fun observeStorageUsage(): Flow<StorageUsageModel> = storageFlow
        override fun observeAutoDownloadPreset(type: AutoDownloadNetworkType): Flow<AutoDownloadPresetModel> {
            return when (type) {
                AutoDownloadNetworkType.MOBILE -> mobilePresetFlow
                AutoDownloadNetworkType.WIFI -> wifiPresetFlow
                AutoDownloadNetworkType.ROAMING -> roamingPresetFlow
            }
        }
        override fun observeKeepMediaSettings(): Flow<KeepMediaSettingsModel> = keepMediaFlow

        override suspend fun getNetworkUsage(type: NetworkUsageType): Result<NetworkUsageModel> = Result.Success(sampleNet)
        override suspend fun resetNetworkUsage(type: NetworkUsageType): Result<Unit> {
            networkReset = true
            return Result.Success(Unit)
        }
        override suspend fun getStorageUsage(): Result<StorageUsageModel> = Result.Success(sampleStorage)
        override suspend fun clearCache(
            clearPhotos: Boolean,
            clearVideos: Boolean,
            clearDocuments: Boolean,
            clearMusic: Boolean,
            clearAudio: Boolean,
            clearStickers: Boolean,
            clearStories: Boolean,
            clearOther: Boolean
        ): Result<Unit> {
            cacheCleared = true
            return Result.Success(Unit)
        }
        override suspend fun clearDatabase(): Result<Unit> {
            databaseCleared = true
            return Result.Success(Unit)
        }
        override suspend fun getAutoDownloadPreset(type: AutoDownloadNetworkType): Result<AutoDownloadPresetModel> {
            return when (type) {
                AutoDownloadNetworkType.MOBILE -> Result.Success(samplePresetMobile)
                AutoDownloadNetworkType.WIFI -> Result.Success(samplePresetWifi)
                AutoDownloadNetworkType.ROAMING -> Result.Success(samplePresetRoaming)
            }
        }
        override suspend fun updateAutoDownloadPreset(preset: AutoDownloadPresetModel): Result<Unit> {
            presetUpdated = true
            return Result.Success(Unit)
        }
        override suspend fun getKeepMediaSettings(): Result<KeepMediaSettingsModel> = Result.Success(sampleKeepMedia)
        override suspend fun updateKeepMedia(chatType: Int, keepMediaDuration: Int): Result<Unit> {
            keepMediaUpdated = true
            return Result.Success(Unit)
        }
        override suspend fun refreshStorageUsage(): Result<Unit> {
            storageRefreshed = true
            return Result.Success(Unit)
        }
    }
}
