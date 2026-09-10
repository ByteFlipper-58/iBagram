package org.telegram.messenger.feature.media.camera

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.telegram.messenger.feature.media.camera.data.repository.LegacyCameraRepository
import org.telegram.messenger.feature.media.camera.domain.model.CameraDeviceModel
import org.telegram.messenger.feature.media.camera.domain.model.CameraFacing
import org.telegram.messenger.feature.media.camera.domain.model.CameraFlashMode
import org.telegram.messenger.feature.media.camera.domain.model.CameraRecordingState
import org.telegram.messenger.feature.media.camera.domain.model.CameraResolutionModel
import org.telegram.messenger.feature.media.camera.domain.usecase.ChooseOptimalResolutionUseCase
import org.telegram.messenger.feature.media.camera.domain.usecase.GetCameraStateUseCase
import org.telegram.messenger.feature.media.camera.domain.usecase.InitCamerasUseCase
import org.telegram.messenger.feature.media.camera.domain.usecase.NotifyCameraRecordingUseCase
import org.telegram.messenger.feature.media.camera.domain.usecase.ObserveCameraStateUseCase
import org.telegram.messenger.feature.media.camera.domain.usecase.SelectCameraUseCase
import org.telegram.messenger.feature.media.camera.domain.usecase.SetCameraFlashModeUseCase
import org.telegram.messenger.feature.media.camera.domain.usecase.SwitchCameraUseCase
import org.telegram.messenger.feature.media.camera.domain.usecase.ToggleMirrorFrontCameraUseCase
import org.telegram.messenger.feature.media.camera.presentation.CameraEvent
import org.telegram.messenger.feature.media.camera.presentation.CameraViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class CameraDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: LegacyCameraRepository
    private lateinit var observeCameraStateUseCase: ObserveCameraStateUseCase
    private lateinit var getCameraStateUseCase: GetCameraStateUseCase
    private lateinit var initCamerasUseCase: InitCamerasUseCase
    private lateinit var selectCameraUseCase: SelectCameraUseCase
    private lateinit var switchCameraUseCase: SwitchCameraUseCase
    private lateinit var setCameraFlashModeUseCase: SetCameraFlashModeUseCase
    private lateinit var toggleMirrorFrontCameraUseCase: ToggleMirrorFrontCameraUseCase
    private lateinit var chooseOptimalResolutionUseCase: ChooseOptimalResolutionUseCase
    private lateinit var notifyCameraRecordingUseCase: NotifyCameraRecordingUseCase
    private lateinit var viewModel: CameraViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository = LegacyCameraRepository()
        observeCameraStateUseCase = ObserveCameraStateUseCase(repository)
        getCameraStateUseCase = GetCameraStateUseCase(repository)
        initCamerasUseCase = InitCamerasUseCase(repository)
        selectCameraUseCase = SelectCameraUseCase(repository)
        switchCameraUseCase = SwitchCameraUseCase(repository)
        setCameraFlashModeUseCase = SetCameraFlashModeUseCase(repository)
        toggleMirrorFrontCameraUseCase = ToggleMirrorFrontCameraUseCase(repository)
        chooseOptimalResolutionUseCase = ChooseOptimalResolutionUseCase(repository)
        notifyCameraRecordingUseCase = NotifyCameraRecordingUseCase(repository)

        viewModel = CameraViewModel(
            observeCameraStateUseCase = observeCameraStateUseCase,
            getCameraStateUseCase = getCameraStateUseCase,
            initCamerasUseCase = initCamerasUseCase,
            selectCameraUseCase = selectCameraUseCase,
            switchCameraUseCase = switchCameraUseCase,
            setCameraFlashModeUseCase = setCameraFlashModeUseCase,
            toggleMirrorFrontCameraUseCase = toggleMirrorFrontCameraUseCase,
            chooseOptimalResolutionUseCase = chooseOptimalResolutionUseCase,
            notifyCameraRecordingUseCase = notifyCameraRecordingUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testCameraResolutionModelComputations() {
        val res16x9 = CameraResolutionModel(width = 1920, height = 1080)
        assertEquals(2073600L, res16x9.area)
        assertTrue(res16x9.hasSameAspectRatio(16, 9))
        assertFalse(res16x9.hasSameAspectRatio(4, 3))

        val res4x3 = CameraResolutionModel(width = 640, height = 480)
        assertTrue(res4x3.hasSameAspectRatio(4, 3))
        assertFalse(res4x3.hasSameAspectRatio(16, 9))
    }

    @Test
    fun testCameraDeviceModel() {
        val device = CameraDeviceModel(
            id = 1,
            facing = CameraFacing.FRONT,
            previewResolutions = listOf(
                CameraResolutionModel(1280, 720),
                CameraResolutionModel(640, 480)
            ),
            pictureResolutions = listOf(
                CameraResolutionModel(1920, 1080),
                CameraResolutionModel(1280, 720)
            )
        )

        assertTrue(device.isFront)
        assertEquals(CameraResolutionModel(1920, 1080), device.maxPictureResolution)
        assertEquals(CameraResolutionModel(1280, 720), device.maxPreviewResolution)
    }

    @Test
    fun testCameraSelectionAndSwitching() {
        initCamerasUseCase()
        val state = getCameraStateUseCase()

        assertTrue(state.isInitialized)
        assertEquals(2, state.availableCameras.size)
        assertEquals(0, state.selectedCameraId)
        assertEquals(CameraFacing.BACK, state.selectedCamera?.facing)

        // Switch to front camera
        switchCameraUseCase()
        assertEquals(1, getCameraStateUseCase().selectedCameraId)
        assertEquals(CameraFacing.FRONT, getCameraStateUseCase().selectedCamera?.facing)

        // Switch back
        switchCameraUseCase()
        assertEquals(0, getCameraStateUseCase().selectedCameraId)

        // Direct select
        selectCameraUseCase(1)
        assertEquals(1, getCameraStateUseCase().selectedCameraId)
    }

    @Test
    fun testFlashAndMirrorModes() {
        initCamerasUseCase()

        assertEquals(CameraFlashMode.OFF, getCameraStateUseCase().flashMode)
        assertFalse(getCameraStateUseCase().isMirrorFrontCamera)

        setCameraFlashModeUseCase(CameraFlashMode.TORCH)
        assertEquals(CameraFlashMode.TORCH, getCameraStateUseCase().flashMode)

        toggleMirrorFrontCameraUseCase(true)
        assertTrue(getCameraStateUseCase().isMirrorFrontCamera)
    }

    @Test
    fun testChooseOptimalResolution() {
        val choices = listOf(
            CameraResolutionModel(3840, 2160),
            CameraResolutionModel(1920, 1080),
            CameraResolutionModel(1280, 720),
            CameraResolutionModel(640, 480)
        )

        // Request 1280x720 with 16:9 aspect ratio -> should pick 1280x720
        val optimal = chooseOptimalResolutionUseCase(
            resolutions = choices,
            targetWidth = 1280,
            targetHeight = 720,
            targetAspectWidth = 16,
            targetAspectHeight = 9
        )
        assertNotNull(optimal)
        assertEquals(1280, optimal!!.width)
        assertEquals(720, optimal.height)

        // Request not bigger than 1280x720
        val notBigger = chooseOptimalResolutionUseCase(
            resolutions = choices,
            targetWidth = 1000,
            targetHeight = 600,
            targetAspectWidth = 16,
            targetAspectHeight = 9,
            notBigger = true
        )
        assertNotNull(notBigger)
        assertTrue(notBigger!!.width <= 1000 || notBigger.height <= 600)
    }

    @Test
    fun testRecordingLifecycle() {
        assertEquals(CameraRecordingState.IDLE, getCameraStateUseCase().recordingState)
        assertFalse(getCameraStateUseCase().isRecording)

        notifyCameraRecordingUseCase.started("/tmp/video.mp4")
        assertEquals(CameraRecordingState.RECORDING, getCameraStateUseCase().recordingState)
        assertTrue(getCameraStateUseCase().isRecording)
        assertEquals("/tmp/video.mp4", getCameraStateUseCase().lastRecordedFilePath)

        notifyCameraRecordingUseCase.finished("/tmp/video.mp4", 5500L)
        assertEquals(CameraRecordingState.FINISHED, getCameraStateUseCase().recordingState)
        assertFalse(getCameraStateUseCase().isRecording)
        assertEquals(5500L, getCameraStateUseCase().lastRecordedDurationMs)

        notifyCameraRecordingUseCase.failed("Camera device error")
        assertEquals(CameraRecordingState.FAILED, getCameraStateUseCase().recordingState)
        assertEquals("Camera device error", getCameraStateUseCase().errorMessage)
    }

    @Test
    fun testViewModelStateAndEvents() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isInitialized)
        assertEquals(2, viewModel.uiState.value.availableCameras.size)
        assertTrue(viewModel.uiState.value.canSwitchCamera)

        // Switch camera via ViewModel
        viewModel.onEvent(CameraEvent.SwitchCamera)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.selectedCamera?.id)

        // Compute optimal resolution
        viewModel.onEvent(
            CameraEvent.ComputeOptimalResolutions(
                targetWidth = 1280,
                targetHeight = 720,
                aspectWidth = 16,
                aspectHeight = 9
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.optimalPreviewResolution)
        assertEquals(1280, viewModel.uiState.value.optimalPreviewResolution!!.width)

        // Recording flow via ViewModel
        viewModel.onEvent(CameraEvent.RecordStarted("/tmp/vm_test.mp4"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isRecording)
        assertFalse(viewModel.uiState.value.canSwitchCamera)

        viewModel.onEvent(CameraEvent.RecordFinished("/tmp/vm_test.mp4", 3200L))
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isRecording)
        assertNotNull(viewModel.uiState.value.infoMessage)
        assertTrue(viewModel.uiState.value.infoMessage!!.contains("3200 ms"))

        // Dismiss info
        viewModel.onEvent(CameraEvent.DismissInfo)
        assertNull(viewModel.uiState.value.infoMessage)
    }
}
