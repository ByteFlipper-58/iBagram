package org.telegram.messenger.feature.media.chromecast

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
import org.telegram.messenger.feature.media.chromecast.data.datasource.ChromecastLocalDataSource
import org.telegram.messenger.feature.media.chromecast.data.datasource.ChromecastRemoteDataSource
import org.telegram.messenger.feature.media.chromecast.data.repository.ChromecastRepositoryImpl
import org.telegram.messenger.feature.media.chromecast.domain.model.ChromecastMediaModel
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ChromecastRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var localDataSource: ChromecastLocalDataSource
    private lateinit var remoteDataSource: FakeChromecastRemoteDataSource
    private lateinit var repository: ChromecastRepositoryImpl

    private class FakeChromecastRemoteDataSource(currentAccount: Int) : ChromecastRemoteDataSource(currentAccount) {
        var mockIsCasting: Boolean = false

        override fun isCasting(): Boolean = mockIsCasting

        override suspend fun stopCasting(): Result<Unit> {
            mockIsCasting = false
            return Result.success(Unit)
        }

        override suspend fun setCoverFile(file: File?): Result<String?> {
            return Result.success(file?.absolutePath)
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        localDataSource = ChromecastLocalDataSource(0)
        remoteDataSource = FakeChromecastRemoteDataSource(0)
        repository = ChromecastRepositoryImpl(
            currentAccount = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            mainDispatcher = Dispatchers.Unconfined
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        assertFalse(repository.isCasting())
        val state = repository.getChromecastState()
        assertFalse(state.isCasting)
        assertFalse(state.isConnected)
        assertNull(state.currentMedia)
    }

    @Test
    fun testCastMediaAndIsPlaying() = runTest {
        val media = ChromecastMediaModel(
            mimeType = "video/mp4",
            title = "Test Video",
            subtitle = "Video Subtitle",
            externalPath = "https://example.com/video.mp4",
            width = 1920,
            height = 1080
        )

        remoteDataSource.mockIsCasting = true
        val castResult = repository.castMedia(media)
        assertTrue(castResult.isSuccess)

        assertTrue(repository.isCasting())
        assertTrue(repository.isPlaying(media))

        val otherMedia = ChromecastMediaModel(
            mimeType = "audio/mp3",
            title = "Different Media"
        )
        assertFalse(repository.isPlaying(otherMedia))
    }

    @Test
    fun testStopCasting() = runTest {
        val media = ChromecastMediaModel(
            mimeType = "video/mp4",
            title = "Test Video"
        )

        remoteDataSource.mockIsCasting = true
        repository.castMedia(media)
        assertTrue(repository.isCasting())

        val stopResult = repository.stopCasting()
        assertTrue(stopResult.isSuccess)
        assertFalse(repository.isCasting())
        assertFalse(repository.isPlaying(media))
    }

    @Test
    fun testSetCoverFile() = runTest {
        val file = File("test_cover.jpg")
        val result = repository.setCoverFile(file)
        assertTrue(result.isSuccess)
        assertEquals(file.absolutePath, result.getOrNull())
        assertEquals(file.absolutePath, localDataSource.getCoverPath())
    }

    @Test
    fun testObserveChromecastState() = runTest {
        val state = repository.observeChromecastState().first()
        assertNotNull(state)
        assertFalse(state.isCasting)
    }
}
