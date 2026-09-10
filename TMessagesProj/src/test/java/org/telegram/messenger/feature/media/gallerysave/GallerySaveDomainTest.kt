package org.telegram.messenger.feature.media.gallerysave

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
import org.telegram.messenger.feature.media.gallerysave.data.mapper.GallerySaveMapper
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySaveConfigModel
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySaveDialogExceptionModel
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySavePeerType
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySaveTargetSettingsModel
import org.telegram.messenger.feature.media.gallerysave.domain.repository.GallerySaveRepository
import org.telegram.messenger.feature.media.gallerysave.domain.usecase.GetGallerySaveConfigUseCase
import org.telegram.messenger.feature.media.gallerysave.domain.usecase.GetGallerySaveExceptionsUseCase
import org.telegram.messenger.feature.media.gallerysave.domain.usecase.GetGallerySaveSettingsUseCase
import org.telegram.messenger.feature.media.gallerysave.domain.usecase.ObserveGallerySaveConfigUseCase
import org.telegram.messenger.feature.media.gallerysave.domain.usecase.RemoveAllGallerySaveExceptionsUseCase
import org.telegram.messenger.feature.media.gallerysave.domain.usecase.RemoveGallerySaveExceptionUseCase
import org.telegram.messenger.feature.media.gallerysave.domain.usecase.SetGallerySaveExceptionUseCase
import org.telegram.messenger.feature.media.gallerysave.domain.usecase.SetGallerySaveVideoLimitUseCase
import org.telegram.messenger.feature.media.gallerysave.domain.usecase.ToggleGallerySavePeerTypeUseCase
import org.telegram.messenger.feature.media.gallerysave.domain.usecase.UpdateGallerySaveSettingsUseCase
import org.telegram.messenger.feature.media.gallerysave.presentation.GallerySaveEvent
import org.telegram.messenger.feature.media.gallerysave.presentation.GallerySaveViewModel
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalCoroutinesApi::class)
class GallerySaveDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- 1. Domain Model Tests ---

    @Test
    fun `GallerySavePeerType flags and lookups match Telegram specifications`() {
        assertEquals(1, GallerySavePeerType.PEER.flag)
        assertEquals(2, GallerySavePeerType.GROUP.flag)
        assertEquals(4, GallerySavePeerType.CHANNEL.flag)

        assertEquals("user", GallerySavePeerType.PEER.prefKey)
        assertEquals("groups", GallerySavePeerType.GROUP.prefKey)
        assertEquals("channels", GallerySavePeerType.CHANNEL.prefKey)

        assertEquals(GallerySavePeerType.PEER, GallerySavePeerType.fromFlag(1))
        assertEquals(GallerySavePeerType.GROUP, GallerySavePeerType.fromPrefKey("groups"))
        assertNull(GallerySavePeerType.fromFlag(99))
    }

    @Test
    fun `GallerySaveTargetSettingsModel and ExceptionModel calculate enabled state correctly`() {
        val disabledSettings = GallerySaveTargetSettingsModel(GallerySavePeerType.PEER)
        assertFalse(disabledSettings.isEnabled)

        val photoOnly = disabledSettings.copy(savePhoto = true)
        assertTrue(photoOnly.isEnabled)

        val exception = GallerySaveDialogExceptionModel(
            dialogId = 12345L,
            savePhoto = false,
            saveVideo = true,
            limitVideoBytes = 50 * 1024 * 1024L
        )
        assertTrue(exception.isEnabled)
        assertEquals(12345L, exception.dialogId)
    }

    // --- 2. Mapper Tests ---

    @Test
    fun `GallerySaveMapper maps peer types and flags accurately`() {
        assertEquals(GallerySavePeerType.CHANNEL, GallerySaveMapper.toDomainPeerType(4))
        assertEquals(4, GallerySaveMapper.toLegacyFlag(GallerySavePeerType.CHANNEL))

        val domainException = GallerySaveDialogExceptionModel(dialogId = 999L, savePhoto = true, saveVideo = false)
        val legacyException = GallerySaveMapper.toLegacyException(domainException)
        assertEquals(999L, legacyException.dialogId)
        assertTrue(legacyException.savePhoto)
        assertFalse(legacyException.saveVideo)

        val remapped = GallerySaveMapper.toDomainException(legacyException)
        assertEquals(domainException, remapped)
    }

    // --- 3. Fake Repository & Use Cases Tests ---

    private class FakeGallerySaveRepository : GallerySaveRepository {
        private val settingsMap = ConcurrentHashMap<GallerySavePeerType, GallerySaveTargetSettingsModel>()
        private val exceptionsMap = ConcurrentHashMap<GallerySavePeerType, MutableList<GallerySaveDialogExceptionModel>>()
        private val flow = MutableStateFlow(buildConfig())

        init {
            for (type in GallerySavePeerType.entries) {
                settingsMap[type] = GallerySaveTargetSettingsModel(type)
                exceptionsMap[type] = mutableListOf()
            }
            flow.value = buildConfig()
        }

        private fun buildConfig(): GallerySaveConfigModel {
            return GallerySaveConfigModel(
                userSettings = settingsMap[GallerySavePeerType.PEER] ?: GallerySaveTargetSettingsModel(GallerySavePeerType.PEER),
                groupSettings = settingsMap[GallerySavePeerType.GROUP] ?: GallerySaveTargetSettingsModel(GallerySavePeerType.GROUP),
                channelSettings = settingsMap[GallerySavePeerType.CHANNEL] ?: GallerySaveTargetSettingsModel(GallerySavePeerType.CHANNEL),
                exceptions = exceptionsMap.mapValues { it.value.toList() }
            )
        }

        override fun observeConfig(): Flow<GallerySaveConfigModel> = flow

        override fun getConfig(): GallerySaveConfigModel = flow.value

        override fun getSettings(peerType: GallerySavePeerType): GallerySaveTargetSettingsModel {
            return settingsMap[peerType] ?: GallerySaveTargetSettingsModel(peerType)
        }

        override fun updateSettings(settings: GallerySaveTargetSettingsModel) {
            settingsMap[settings.peerType] = settings
            flow.value = buildConfig()
        }

        override fun togglePeerType(peerType: GallerySavePeerType) {
            val current = getSettings(peerType)
            val toggled = current.copy(savePhoto = !current.savePhoto, saveVideo = !current.saveVideo)
            updateSettings(toggled)
        }

        override fun setVideoLimit(peerType: GallerySavePeerType, limitBytes: Long) {
            val current = getSettings(peerType)
            updateSettings(current.copy(limitVideoBytes = limitBytes))
        }

        override fun getExceptions(peerType: GallerySavePeerType): List<GallerySaveDialogExceptionModel> {
            return exceptionsMap[peerType] ?: emptyList()
        }

        override fun setException(peerType: GallerySavePeerType, exception: GallerySaveDialogExceptionModel) {
            val list = exceptionsMap.computeIfAbsent(peerType) { mutableListOf() }
            list.removeAll { it.dialogId == exception.dialogId }
            list.add(exception)
            flow.value = buildConfig()
        }

        override fun removeException(peerType: GallerySavePeerType, dialogId: Long) {
            exceptionsMap[peerType]?.removeAll { it.dialogId == dialogId }
            flow.value = buildConfig()
        }

        override fun removeAllExceptions(peerType: GallerySavePeerType) {
            exceptionsMap[peerType]?.clear()
            flow.value = buildConfig()
        }
    }

    @Test
    fun `Use cases update settings, toggle peer types, set limits, and manage exceptions`() = runTest {
        val fakeRepo = FakeGallerySaveRepository()
        val observeConfig = ObserveGallerySaveConfigUseCase(fakeRepo)
        val getConfig = GetGallerySaveConfigUseCase(fakeRepo)
        val getSettings = GetGallerySaveSettingsUseCase(fakeRepo)
        val updateSettings = UpdateGallerySaveSettingsUseCase(fakeRepo)
        val togglePeerType = ToggleGallerySavePeerTypeUseCase(fakeRepo)
        val setVideoLimit = SetGallerySaveVideoLimitUseCase(fakeRepo)
        val getExceptions = GetGallerySaveExceptionsUseCase(fakeRepo)
        val setException = SetGallerySaveExceptionUseCase(fakeRepo)
        val removeException = RemoveGallerySaveExceptionUseCase(fakeRepo)
        val removeAll = RemoveAllGallerySaveExceptionsUseCase(fakeRepo)

        // Initial config
        val initialConfig = getConfig()
        assertFalse(initialConfig.userSettings.isEnabled)

        // Update settings
        updateSettings(GallerySaveTargetSettingsModel(GallerySavePeerType.PEER, savePhoto = true, saveVideo = true))
        assertTrue(getSettings(GallerySavePeerType.PEER).isEnabled)

        // Toggle
        togglePeerType(GallerySavePeerType.PEER)
        assertFalse(getSettings(GallerySavePeerType.PEER).isEnabled)

        // Set video limit
        setVideoLimit(GallerySavePeerType.GROUP, 250 * 1024 * 1024L)
        assertEquals(250 * 1024 * 1024L, getSettings(GallerySavePeerType.GROUP).limitVideoBytes)

        // Exceptions
        val ex1 = GallerySaveDialogExceptionModel(dialogId = 111L, savePhoto = true)
        val ex2 = GallerySaveDialogExceptionModel(dialogId = 222L, saveVideo = true)
        setException(GallerySavePeerType.CHANNEL, ex1)
        setException(GallerySavePeerType.CHANNEL, ex2)
        assertEquals(2, getExceptions(GallerySavePeerType.CHANNEL).size)

        // Remove one exception
        removeException(GallerySavePeerType.CHANNEL, 111L)
        assertEquals(1, getExceptions(GallerySavePeerType.CHANNEL).size)
        assertEquals(222L, getExceptions(GallerySavePeerType.CHANNEL)[0].dialogId)

        // Remove all
        removeAll(GallerySavePeerType.CHANNEL)
        assertTrue(getExceptions(GallerySavePeerType.CHANNEL).isEmpty())

        val flowConfig = observeConfig().first()
        assertTrue(flowConfig.getExceptions(GallerySavePeerType.CHANNEL).isEmpty())
    }

    // --- 4. Presentation ViewModel MVI Tests ---

    @Test
    fun `GallerySaveViewModel updates selection, toggles options and manages exceptions`() = runTest {
        val fakeRepo = FakeGallerySaveRepository()
        val viewModel = GallerySaveViewModel(
            observeGallerySaveConfigUseCase = ObserveGallerySaveConfigUseCase(fakeRepo),
            getGallerySaveConfigUseCase = GetGallerySaveConfigUseCase(fakeRepo),
            getGallerySaveSettingsUseCase = GetGallerySaveSettingsUseCase(fakeRepo),
            updateGallerySaveSettingsUseCase = UpdateGallerySaveSettingsUseCase(fakeRepo),
            toggleGallerySavePeerTypeUseCase = ToggleGallerySavePeerTypeUseCase(fakeRepo),
            setGallerySaveVideoLimitUseCase = SetGallerySaveVideoLimitUseCase(fakeRepo),
            setGallerySaveExceptionUseCase = SetGallerySaveExceptionUseCase(fakeRepo),
            removeGallerySaveExceptionUseCase = RemoveGallerySaveExceptionUseCase(fakeRepo),
            removeAllGallerySaveExceptionsUseCase = RemoveAllGallerySaveExceptionsUseCase(fakeRepo)
        )

        advanceUntilIdle()
        assertEquals(GallerySavePeerType.PEER, viewModel.uiState.value.selectedPeerType)

        // Select peer type
        viewModel.onEvent(GallerySaveEvent.SelectPeerType(GallerySavePeerType.GROUP))
        assertEquals(GallerySavePeerType.GROUP, viewModel.uiState.value.selectedPeerType)

        // Toggle photo
        viewModel.onEvent(GallerySaveEvent.TogglePhoto(GallerySavePeerType.GROUP))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.currentSettings.savePhoto)

        // Toggle video
        viewModel.onEvent(GallerySaveEvent.ToggleVideo(GallerySavePeerType.GROUP))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.currentSettings.saveVideo)

        // Set video limit
        viewModel.onEvent(GallerySaveEvent.SetVideoLimit(GallerySavePeerType.GROUP, 500 * 1024 * 1024L))
        advanceUntilIdle()
        assertEquals(500 * 1024 * 1024L, viewModel.uiState.value.currentSettings.limitVideoBytes)

        // Add exception
        val ex = GallerySaveDialogExceptionModel(dialogId = 999L, savePhoto = true)
        viewModel.onEvent(GallerySaveEvent.SetException(GallerySavePeerType.GROUP, ex))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.currentExceptionsCount)
        assertNotNull(viewModel.uiState.value.infoMessage)

        // Dismiss messages
        viewModel.onEvent(GallerySaveEvent.DismissMessages)
        assertNull(viewModel.uiState.value.infoMessage)

        // Delete exception
        viewModel.onEvent(GallerySaveEvent.RemoveException(GallerySavePeerType.GROUP, 999L))
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.currentExceptionsCount)
    }

    // --- 5. DI Container Test ---

    @Test
    fun `AccountFeatureContainer resolves gallery save dependencies`() {
        val container = AccountFeatureContainer.get(0)
        val fakeRepo = FakeGallerySaveRepository()
        container.gallerySaveRepository = fakeRepo

        assertNotNull(container.gallerySaveRepository)
        assertNotNull(container.observeGallerySaveConfigUseCase)
        assertNotNull(container.getGallerySaveConfigUseCase)
        assertNotNull(container.getGallerySaveSettingsUseCase)
        assertNotNull(container.updateGallerySaveSettingsUseCase)
        assertNotNull(container.toggleGallerySavePeerTypeUseCase)
        assertNotNull(container.setGallerySaveVideoLimitUseCase)
        assertNotNull(container.getGallerySaveExceptionsUseCase)
        assertNotNull(container.setGallerySaveExceptionUseCase)
        assertNotNull(container.removeGallerySaveExceptionUseCase)
        assertNotNull(container.removeAllGallerySaveExceptionsUseCase)

        val vm = container.createGallerySaveViewModel()
        assertNotNull(vm)
    }
}
