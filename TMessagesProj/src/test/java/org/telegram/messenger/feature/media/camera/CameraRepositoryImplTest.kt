package org.telegram.messenger.feature.media.camera

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
import org.telegram.messenger.feature.media.camera.data.datasource.CameraLocalDataSource
import org.telegram.messenger.feature.media.camera.data.datasource.CameraRemoteDataSource
import org.telegram.messenger.feature.media.camera.data.repository.CameraRepositoryImpl
import org.telegram.messenger.feature.media.camera.domain.model.CameraFlashMode
import org.telegram.messenger.feature.media.camera.domain.model.CameraRecordingState
import org.telegram.messenger.feature.media.camera.domain.model.CameraResolutionModel

@OptIn(ExperimentalCoroutinesApi::class)
class CameraRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var localDataSource: CameraLocalDataSource
    private lateinit var remoteDataSource: FakeCameraRemoteDataSource
    private lateinit var repository: CameraRepositoryImpl

    private class FakeCameraRemoteDataSource(currentAccount: Int) : CameraRemoteDataSource(currentAccount) {
        var initialized: Boolean = false

        override fun isCameraInitialized(): Boolean = initialized

        override fun initCamera(onInit: Runnable) {
            initialized = true
            onInit.run()
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        localDataSource = CameraLocalDataSource(0)
        remoteDataSource = FakeCameraRemoteDataSource(0)
        repository = CameraRepositoryImpl(
            currentAccount = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        val state = repository.getCameraState()
        assertFalse(state.isInitialized)
        assertFalse(state.isLoading)
        assertTrue(state.availableCameras.isEmpty())
        assertNull(state.selectedCameraId)
        assertEquals(CameraFlashMode.OFF, state.flashMode)
        assertEquals(CameraRecordingState.IDLE, state.recordingState)
    }

    @Test
    fun testInitCamerasHeadlessFallback() {
        localDataSource.setupHeadlessFallback()
        val state = repository.getCameraState()
        assertTrue(state.isInitialized)
        assertEquals(2, state.availableCameras.size)
        assertEquals(0, state.selectedCameraId)
    }

    @Test
    fun testSelectAndSwitchCamera() {
        localDataSource.setupHeadlessFallback()
        assertEquals(0, repository.getCameraState().selectedCameraId)

        repository.selectCamera(1)
        assertEquals(1, repository.getCameraState().selectedCameraId)

        repository.switchCamera()
        assertEquals(0, repository.getCameraState().selectedCameraId)
    }

    @Test
    fun testSetFlashModeAndMirror() {
        repository.setFlashMode(CameraFlashMode.AUTO)
        assertEquals(CameraFlashMode.AUTO, repository.getCameraState().flashMode)

        repository.toggleMirrorFrontCamera(true)
        assertTrue(repository.getCameraState().isMirrorFrontCamera)

        repository.toggleMirrorFrontCamera(false)
        assertFalse(repository.getCameraState().isMirrorFrontCamera)
    }

    @Test
    fun testChooseOptimalResolution() {
        val resolutions = listOf(
            CameraResolutionModel(1920, 1080),
            CameraResolutionModel(1280, 720),
            CameraResolutionModel(640, 480)
        )

        val optimal = repository.chooseOptimalResolution(
            resolutions = resolutions,
            targetWidth = 1280,
            targetHeight = 720,
            targetAspectWidth = 16,
            targetAspectHeight = 9,
            notBigger = false
        )

        assertNotNull(optimal)
        assertEquals(1280, optimal?.width)
        assertEquals(720, optimal?.height)
    }

    @Test
    fun testRecordingLifecycle() {
        repository.notifyRecordingStarted("/tmp/test_video.mp4")
        var state = repository.getCameraState()
        assertEquals(CameraRecordingState.RECORDING, state.recordingState)
        assertEquals("/tmp/test_video.mp4", state.lastRecordedFilePath)

        repository.notifyRecordingFinished("/tmp/test_video.mp4", 5000L)
        state = repository.getCameraState()
        assertEquals(CameraRecordingState.FINISHED, state.recordingState)
        assertEquals(5000L, state.lastRecordedDurationMs)

        repository.notifyRecordingFailed("Disk full")
        state = repository.getCameraState()
        assertEquals(CameraRecordingState.FAILED, state.recordingState)
        assertEquals("Disk full", state.errorMessage)
    }

    @Test
    fun testObserveCameraState() = runTest {
        val initial = repository.observeCameraState().first()
        assertNotNull(initial)
        assertFalse(initial.isInitialized)
    }
}
