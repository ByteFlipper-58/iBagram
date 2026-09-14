package org.telegram.messenger.feature.system.datastorage

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.datastorage.data.datasource.DataStorageLocalDataSource
import org.telegram.messenger.feature.system.datastorage.data.datasource.DataStorageRemoteDataSource
import org.telegram.messenger.feature.system.datastorage.data.repository.DataStorageRepositoryImpl
import org.telegram.messenger.feature.system.datastorage.domain.model.AutoDownloadNetworkType
import org.telegram.messenger.feature.system.datastorage.domain.model.AutoDownloadPresetModel
import org.telegram.messenger.feature.system.datastorage.domain.model.NetworkUsageType
import org.telegram.messenger.feature.system.datastorage.domain.model.StorageUsageModel

class DataStorageRepositoryImplTest {

    private lateinit var localDataSource: DataStorageLocalDataSource
    private lateinit var remoteDataSource: DataStorageRemoteDataSource
    private lateinit var repository: DataStorageRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = DataStorageLocalDataSource(currentAccount = 0, testMode = true)
        remoteDataSource = DataStorageRemoteDataSource(currentAccount = 0)
        repository = DataStorageRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun testNetworkUsageFlowAndGet() = runTest {
        val initial = repository.getNetworkUsage(NetworkUsageType.MOBILE)
        assertTrue(initial is Result.Success)
        assertEquals(NetworkUsageType.MOBILE, (initial as Result.Success).data.networkType)

        val flowItem = repository.observeNetworkUsage(NetworkUsageType.MOBILE).first()
        assertEquals(NetworkUsageType.MOBILE, flowItem.networkType)

        val resetResult = repository.resetNetworkUsage(NetworkUsageType.MOBILE)
        assertTrue(resetResult is Result.Success)
    }

    @Test
    fun testStorageUsageAndClearing() = runTest {
        localDataSource.updateStorageUsageState(
            StorageUsageModel(
                photosBytes = 300L,
                videosBytes = 400L,
                documentsBytes = 300L,
                databaseBytes = 500L
            )
        )

        val usage = repository.getStorageUsage()
        assertTrue(usage is Result.Success)
        assertEquals(1000L, (usage as Result.Success).data.totalCacheBytes)
        assertEquals(300L, usage.data.photosBytes)

        val clearRes = repository.clearCache(clearPhotos = true, clearVideos = true, clearDocuments = false)
        assertTrue(clearRes is Result.Success)

        val postClear = repository.getStorageUsage()
        assertTrue(postClear is Result.Success)
        assertEquals(0L, (postClear as Result.Success).data.photosBytes)
        assertEquals(0L, postClear.data.videosBytes)
        assertEquals(300L, postClear.data.documentsBytes)

        val clearDbRes = repository.clearDatabase()
        assertTrue(clearDbRes is Result.Success)
        val postDbClear = repository.getStorageUsage()
        assertTrue(postDbClear is Result.Success)
        assertEquals(0L, (postDbClear as Result.Success).data.databaseBytes)

        val refreshRes = repository.refreshStorageUsage()
        assertTrue(refreshRes is Result.Success)
    }

    @Test
    fun testAutoDownloadPresets() = runTest {
        val preset = repository.getAutoDownloadPreset(AutoDownloadNetworkType.WIFI)
        assertTrue(preset is Result.Success)
        assertEquals(AutoDownloadNetworkType.WIFI, (preset as Result.Success).data.networkType)

        val updated = preset.data.copy(downloadPhotos = false, downloadVideos = true)
        val updateRes = repository.updateAutoDownloadPreset(updated)
        assertTrue(updateRes is Result.Success)

        val observed = repository.observeAutoDownloadPreset(AutoDownloadNetworkType.WIFI).first()
        assertEquals(false, observed.downloadPhotos)
        assertEquals(true, observed.downloadVideos)
    }

    @Test
    fun testKeepMediaSettings() = runTest {
        val initial = repository.getKeepMediaSettings()
        assertTrue(initial is Result.Success)

        val updateRes = repository.updateKeepMedia(chatType = 0, keepMediaDuration = 30)
        assertTrue(updateRes is Result.Success)

        val observed = repository.observeKeepMediaSettings().first()
        assertEquals(30, observed.keepMediaUser)
    }

    @Test
    fun testRemoteDataSource() = runTest {
        val res = remoteDataSource.fetchStoragePolicy()
        assertTrue(res is Result.Success)
        assertTrue((res as Result.Success).data)
    }
}
