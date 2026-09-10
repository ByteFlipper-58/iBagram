package org.telegram.messenger.feature.media.chromecast

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
import org.telegram.messenger.feature.media.chromecast.data.mapper.ChromecastMapper
import org.telegram.messenger.feature.media.chromecast.domain.model.ChromecastMediaModel
import org.telegram.messenger.feature.media.chromecast.domain.model.ChromecastStateModel
import org.telegram.messenger.feature.media.chromecast.domain.repository.ChromecastRepository
import org.telegram.messenger.feature.media.chromecast.domain.usecase.CastMediaUseCase
import org.telegram.messenger.feature.media.chromecast.domain.usecase.GetChromecastStateUseCase
import org.telegram.messenger.feature.media.chromecast.domain.usecase.IsCastingUseCase
import org.telegram.messenger.feature.media.chromecast.domain.usecase.IsMediaPlayingOnCastUseCase
import org.telegram.messenger.feature.media.chromecast.domain.usecase.ObserveChromecastStateUseCase
import org.telegram.messenger.feature.media.chromecast.domain.usecase.SetCastCoverFileUseCase
import org.telegram.messenger.feature.media.chromecast.domain.usecase.StopCastingUseCase
import org.telegram.messenger.feature.media.chromecast.presentation.ChromecastEvent
import org.telegram.messenger.feature.media.chromecast.presentation.ChromecastViewModel
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ChromecastDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeChromecastRepository : ChromecastRepository {
        var castingActive: Boolean = false
        var deviceName: String? = "Living Room TV"
        var currentMedia: ChromecastMediaModel? = null
        var shouldFailCast: Boolean = false
        var shouldFailStop: Boolean = false
        var shouldFailCover: Boolean = false

        private val stateFlow = MutableStateFlow(computeState())

        private fun computeState(): ChromecastStateModel = ChromecastStateModel(
            isCasting = castingActive,
            deviceName = if (castingActive) deviceName else null,
            currentMedia = currentMedia,
            isConnected = castingActive
        )

        private fun notifyChange() {
            stateFlow.value = computeState()
        }

        override fun observeChromecastState(): Flow<ChromecastStateModel> = stateFlow.asStateFlow()

        override fun getChromecastState(): ChromecastStateModel = computeState()

        override fun isCasting(): Boolean = castingActive

        override fun isPlaying(media: ChromecastMediaModel): Boolean = castingActive && currentMedia == media

        override suspend fun castMedia(media: ChromecastMediaModel): Result<Unit> {
            return if (shouldFailCast) {
                Result.failure(RuntimeException("Cast connection failed"))
            } else {
                castingActive = true
                currentMedia = media
                notifyChange()
                Result.success(Unit)
            }
        }

        override suspend fun stopCasting(): Result<Unit> {
            return if (shouldFailStop) {
                Result.failure(RuntimeException("Stop casting failed"))
            } else {
                castingActive = false
                currentMedia = null
                notifyChange()
                Result.success(Unit)
            }
        }

        override suspend fun setCoverFile(file: File?): Result<String?> {
            return if (shouldFailCover) {
                Result.failure(RuntimeException("File server error"))
            } else {
                Result.success(file?.let { "/cover_${it.name}" })
            }
        }
    }

    @Test
    fun testChromecastMediaModel() {
        val videoMedia = ChromecastMediaModel(
            mimeType = "video/mp4",
            title = "Test Video",
            subtitle = "HD 1080p",
            externalPath = "/storage/video.mp4",
            width = 1920,
            height = 1080
        )
        assertTrue(videoMedia.isVideo)
        assertFalse(videoMedia.isAudio)
        assertFalse(videoMedia.isImage)
        assertEquals("Test Video", videoMedia.title)
        assertEquals("HD 1080p", videoMedia.subtitle)
        assertEquals(1920, videoMedia.width)

        val audioMedia = ChromecastMediaModel(
            mimeType = "audio/mp3",
            title = "Track 1",
            subtitle = "Artist"
        )
        assertFalse(audioMedia.isVideo)
        assertTrue(audioMedia.isAudio)
        assertFalse(audioMedia.isImage)

        val imageMedia = ChromecastMediaModel(
            mimeType = "image/jpeg",
            title = "Photo"
        )
        assertFalse(imageMedia.isVideo)
        assertFalse(imageMedia.isAudio)
        assertTrue(imageMedia.isImage)
    }

    @Test
    fun testChromecastStateModel() {
        val defaultState = ChromecastStateModel()
        assertFalse(defaultState.isCasting)
        assertNull(defaultState.deviceName)
        assertNull(defaultState.currentMedia)
        assertFalse(defaultState.isConnected)

        val activeMedia = ChromecastMediaModel("video/mp4", title = "Movie")
        val activeState = ChromecastStateModel(
            isCasting = true,
            deviceName = "Bedroom Chromecast",
            currentMedia = activeMedia,
            isConnected = true
        )
        assertTrue(activeState.isCasting)
        assertEquals("Bedroom Chromecast", activeState.deviceName)
        assertEquals("Movie", activeState.currentMedia?.title)
        assertTrue(activeState.isConnected)
    }

    @Test
    fun testChromecastMapper() {
        val media = ChromecastMediaModel("video/mp4", title = "Sample")
        val domainState = ChromecastMapper.toDomainState(
            isCasting = true,
            deviceName = "Google TV",
            currentMedia = media,
            isConnected = true
        )
        assertTrue(domainState.isCasting)
        assertEquals("Google TV", domainState.deviceName)
        assertEquals(media, domainState.currentMedia)
        assertTrue(domainState.isConnected)

        assertNull(ChromecastMapper.toDomainMedia(null))
        assertNull(ChromecastMapper.toDomainMediaVariations(null))
    }

    @Test
    fun testChromecastUseCases() = runTest {
        val repo = FakeChromecastRepository()
        val observeUseCase = ObserveChromecastStateUseCase(repo)
        val getStateUseCase = GetChromecastStateUseCase(repo)
        val isCastingUseCase = IsCastingUseCase(repo)
        val isPlayingUseCase = IsMediaPlayingOnCastUseCase(repo)
        val castMediaUseCase = CastMediaUseCase(repo)
        val stopCastingUseCase = StopCastingUseCase(repo)
        val setCoverUseCase = SetCastCoverFileUseCase(repo)

        assertFalse(isCastingUseCase())
        assertFalse(getStateUseCase().isCasting)

        val sampleMedia = ChromecastMediaModel("video/mp4", title = "Vacation Video")
        assertFalse(isPlayingUseCase(sampleMedia))

        // Cast media
        val castResult = castMediaUseCase(sampleMedia)
        assertTrue(castResult.isSuccess)
        assertTrue(isCastingUseCase())
        assertTrue(isPlayingUseCase(sampleMedia))
        assertEquals(sampleMedia, getStateUseCase().currentMedia)
        assertEquals("Living Room TV", getStateUseCase().deviceName)

        // Set cover
        val coverResult = setCoverUseCase(File("cover.jpg"))
        assertTrue(coverResult.isSuccess)
        assertEquals("/cover_cover.jpg", coverResult.getOrNull())

        // Stop casting
        val stopResult = stopCastingUseCase()
        assertTrue(stopResult.isSuccess)
        assertFalse(isCastingUseCase())
        assertFalse(isPlayingUseCase(sampleMedia))
        assertNull(getStateUseCase().currentMedia)

        // Failures
        repo.shouldFailCast = true
        val failedCast = castMediaUseCase(sampleMedia)
        assertTrue(failedCast.isFailure)

        repo.shouldFailStop = true
        val failedStop = stopCastingUseCase()
        assertTrue(failedStop.isFailure)

        repo.shouldFailCover = true
        val failedCover = setCoverUseCase(File("cover.jpg"))
        assertTrue(failedCover.isFailure)
    }

    @Test
    fun testChromecastViewModel() = runTest {
        val repo = FakeChromecastRepository()
        val vm = ChromecastViewModel(
            observeChromecastStateUseCase = ObserveChromecastStateUseCase(repo),
            getChromecastStateUseCase = GetChromecastStateUseCase(repo),
            isCastingUseCase = IsCastingUseCase(repo),
            isMediaPlayingOnCastUseCase = IsMediaPlayingOnCastUseCase(repo),
            castMediaUseCase = CastMediaUseCase(repo),
            stopCastingUseCase = StopCastingUseCase(repo),
            setCastCoverFileUseCase = SetCastCoverFileUseCase(repo)
        )

        advanceUntilIdle()
        assertFalse(vm.uiState.value.state.isCasting)
        assertNull(vm.uiState.value.errorMessage)
        assertNull(vm.uiState.value.infoMessage)

        val media = ChromecastMediaModel("video/mp4", title = "Summer Clip")

        // Start casting
        vm.onEvent(ChromecastEvent.CastMedia(media))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.state.isCasting)
        assertEquals(media, vm.uiState.value.state.currentMedia)
        assertEquals("Media casting started", vm.uiState.value.infoMessage)

        // Dismiss info
        vm.onEvent(ChromecastEvent.DismissInfo)
        assertNull(vm.uiState.value.infoMessage)

        // Set cover file
        vm.onEvent(ChromecastEvent.SetCoverFile(File("thumb.png")))
        advanceUntilIdle()
        assertEquals("Cover updated: /cover_thumb.png", vm.uiState.value.infoMessage)

        // Failure handling
        repo.shouldFailCast = true
        vm.onEvent(ChromecastEvent.CastMedia(media))
        advanceUntilIdle()
        assertEquals("Cast connection failed", vm.uiState.value.errorMessage)

        // Dismiss error
        vm.onEvent(ChromecastEvent.DismissError)
        assertNull(vm.uiState.value.errorMessage)

        // Stop casting
        repo.shouldFailStop = false
        vm.onEvent(ChromecastEvent.StopCasting)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.state.isCasting)
        assertEquals("Casting stopped", vm.uiState.value.infoMessage)

        // Refresh state
        vm.onEvent(ChromecastEvent.RefreshState)
        assertFalse(vm.uiState.value.state.isCasting)
    }

    @Test
    fun testAccountFeatureContainerIntegration() = runTest {
        val container = AccountFeatureContainer.get(0)
        assertNotNull(container.chromecastRepository)
        assertNotNull(container.observeChromecastStateUseCase)
        assertNotNull(container.getChromecastStateUseCase)
        assertNotNull(container.isCastingUseCase)
        assertNotNull(container.isMediaPlayingOnCastUseCase)
        assertNotNull(container.castMediaUseCase)
        assertNotNull(container.stopCastingUseCase)
        assertNotNull(container.setCastCoverFileUseCase)

        val fakeRepo = FakeChromecastRepository()
        container.chromecastRepository = fakeRepo
        assertEquals(fakeRepo, container.chromecastRepository)

        val vm = container.createChromecastViewModel()
        assertNotNull(vm)
        assertFalse(vm.uiState.value.state.isCasting)
    }
}
