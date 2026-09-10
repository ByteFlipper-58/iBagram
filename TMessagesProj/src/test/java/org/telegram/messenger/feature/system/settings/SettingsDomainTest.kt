package org.telegram.messenger.feature.system.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.settings.data.mapper.SettingsMapper
import org.telegram.messenger.feature.system.settings.domain.model.SettingsModel
import org.telegram.messenger.feature.system.settings.domain.repository.SettingsRepository
import org.telegram.messenger.feature.system.settings.domain.usecase.GetSettingsUseCase
import org.telegram.messenger.feature.system.settings.domain.usecase.ObserveSettingsUseCase
import org.telegram.messenger.feature.system.settings.domain.usecase.UpdateBubbleRadiusUseCase
import org.telegram.messenger.feature.system.settings.domain.usecase.UpdateFontSizeUseCase
import org.telegram.messenger.feature.system.settings.domain.usecase.UpdateSaveToGalleryUseCase
import org.telegram.messenger.feature.system.settings.domain.usecase.UpdateStreamMediaUseCase
import org.telegram.messenger.feature.system.settings.domain.usecase.UpdateSyncContactsUseCase
import org.telegram.messenger.feature.system.settings.presentation.SettingsEvent
import org.telegram.messenger.feature.system.settings.presentation.SettingsUiState
import org.telegram.messenger.feature.system.settings.presentation.SettingsViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeSettingsRepository : SettingsRepository {
        val settingsFlow = MutableSharedFlow<SettingsModel>(replay = 1)
        var currentSettings: SettingsModel = SettingsModel()
            set(value) {
                field = value
                settingsFlow.tryEmit(value)
            }
        var updateSuccess = true

        init {
            settingsFlow.tryEmit(currentSettings)
        }

        override fun observeSettings(): Flow<SettingsModel> = settingsFlow.asSharedFlow()

        override suspend fun getSettings(): SettingsModel = currentSettings

        override suspend fun updateFontSize(size: Int): Result<Unit> {
            return if (updateSuccess) {
                currentSettings = currentSettings.copy(fontSize = size)
                settingsFlow.emit(currentSettings)
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Failed to update font size"))
            }
        }

        override suspend fun updateBubbleRadius(radius: Int): Result<Unit> {
            return if (updateSuccess) {
                currentSettings = currentSettings.copy(bubbleRadius = radius)
                settingsFlow.emit(currentSettings)
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Failed to update bubble radius"))
            }
        }

        override suspend fun updateSaveToGallery(enabled: Boolean): Result<Unit> {
            return if (updateSuccess) {
                currentSettings = currentSettings.copy(saveToGallery = enabled)
                settingsFlow.emit(currentSettings)
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Failed to update save to gallery"))
            }
        }

        override suspend fun updateStreamMedia(enabled: Boolean): Result<Unit> {
            return if (updateSuccess) {
                currentSettings = currentSettings.copy(streamMedia = enabled)
                settingsFlow.emit(currentSettings)
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Failed to update stream media"))
            }
        }

        override suspend fun updateSuggestStickers(enabled: Boolean): Result<Unit> {
            return if (updateSuccess) {
                currentSettings = currentSettings.copy(suggestStickers = enabled)
                settingsFlow.emit(currentSettings)
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Failed to update suggest stickers"))
            }
        }

        override suspend fun updateInappCamera(enabled: Boolean): Result<Unit> {
            return if (updateSuccess) {
                currentSettings = currentSettings.copy(inappCamera = enabled)
                settingsFlow.emit(currentSettings)
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Failed to update inapp camera"))
            }
        }

        override suspend fun updateDistanceSystemType(type: Int): Result<Unit> {
            return if (updateSuccess) {
                currentSettings = currentSettings.copy(distanceSystemType = type)
                settingsFlow.emit(currentSettings)
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Failed to update distance system type"))
            }
        }

        override suspend fun updateSyncContacts(enabled: Boolean): Result<Unit> {
            return if (updateSuccess) {
                currentSettings = currentSettings.copy(syncContacts = enabled)
                settingsFlow.emit(currentSettings)
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Failed to update sync contacts"))
            }
        }

        override suspend fun updateSuggestContacts(enabled: Boolean): Result<Unit> {
            return if (updateSuccess) {
                currentSettings = currentSettings.copy(suggestContacts = enabled)
                settingsFlow.emit(currentSettings)
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Failed to update suggest contacts"))
            }
        }

        override suspend fun updateShowCallsTab(enabled: Boolean): Result<Unit> {
            return if (updateSuccess) {
                currentSettings = currentSettings.copy(showCallsTab = enabled)
                settingsFlow.emit(currentSettings)
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Failed to update show calls tab"))
            }
        }
    }

    @Test
    fun `default SettingsModel contains standard Telegram values`() {
        val model = SettingsModel()
        assertEquals(16, model.fontSize)
        assertEquals(17, model.bubbleRadius)
        assertFalse(model.saveToGallery)
        assertTrue(model.streamMedia)
        assertTrue(model.suggestStickers)
        assertTrue(model.inappCamera)
        assertEquals(0, model.distanceSystemType)
        assertTrue(model.syncContacts)
        assertTrue(model.suggestContacts)
        assertFalse(model.showCallsTab)
    }

    @Test
    fun `SettingsMapper mapFromValues creates correct domain model`() {
        val model = SettingsMapper.mapFromValues(
            fontSize = 18,
            bubbleRadius = 12,
            saveToGallery = true,
            streamMedia = false,
            suggestStickers = false,
            inappCamera = false,
            distanceSystemType = 1,
            syncContacts = false,
            suggestContacts = false,
            showCallsTab = true
        )
        assertEquals(18, model.fontSize)
        assertEquals(12, model.bubbleRadius)
        assertTrue(model.saveToGallery)
        assertFalse(model.streamMedia)
        assertFalse(model.suggestStickers)
        assertFalse(model.inappCamera)
        assertEquals(1, model.distanceSystemType)
        assertFalse(model.syncContacts)
        assertFalse(model.suggestContacts)
        assertTrue(model.showCallsTab)
    }

    @Test
    fun `GetSettingsUseCase returns current settings from repository`() = runTest {
        val repo = FakeSettingsRepository()
        repo.currentSettings = SettingsModel(fontSize = 20)
        val useCase = GetSettingsUseCase(repo)

        val result = useCase()
        assertEquals(20, result.fontSize)
    }

    @Test
    fun `ObserveSettingsUseCase emits updates reactively`() = runTest {
        val repo = FakeSettingsRepository()
        val useCase = ObserveSettingsUseCase(repo)

        val initial = useCase().first()
        assertEquals(16, initial.fontSize)

        repo.settingsFlow.emit(SettingsModel(fontSize = 22))
        val updated = useCase().first()
        assertEquals(22, updated.fontSize)
    }

    @Test
    fun `UpdateFontSizeUseCase delegates to repository`() = runTest {
        val repo = FakeSettingsRepository()
        val useCase = UpdateFontSizeUseCase(repo)

        val result = useCase(24)
        assertTrue(result is Result.Success)
        assertEquals(24, repo.currentSettings.fontSize)

        repo.updateSuccess = false
        val failResult = useCase(14)
        assertTrue(failResult is Result.Failure)
    }

    @Test
    fun `UpdateBubbleRadiusUseCase delegates to repository`() = runTest {
        val repo = FakeSettingsRepository()
        val useCase = UpdateBubbleRadiusUseCase(repo)

        val result = useCase(8)
        assertTrue(result is Result.Success)
        assertEquals(8, repo.currentSettings.bubbleRadius)

        repo.updateSuccess = false
        val failResult = useCase(12)
        assertTrue(failResult is Result.Failure)
    }

    @Test
    fun `UpdateSaveToGalleryUseCase delegates to repository`() = runTest {
        val repo = FakeSettingsRepository()
        val useCase = UpdateSaveToGalleryUseCase(repo)

        val result = useCase(true)
        assertTrue(result is Result.Success)
        assertTrue(repo.currentSettings.saveToGallery)

        repo.updateSuccess = false
        val failResult = useCase(false)
        assertTrue(failResult is Result.Failure)
    }

    @Test
    fun `UpdateStreamMediaUseCase delegates to repository`() = runTest {
        val repo = FakeSettingsRepository()
        val useCase = UpdateStreamMediaUseCase(repo)

        val result = useCase(false)
        assertTrue(result is Result.Success)
        assertFalse(repo.currentSettings.streamMedia)

        repo.updateSuccess = false
        val failResult = useCase(true)
        assertTrue(failResult is Result.Failure)
    }

    @Test
    fun `UpdateSyncContactsUseCase delegates to repository`() = runTest {
        val repo = FakeSettingsRepository()
        val useCase = UpdateSyncContactsUseCase(repo)

        val result = useCase(false)
        assertTrue(result is Result.Success)
        assertFalse(repo.currentSettings.syncContacts)

        repo.updateSuccess = false
        val failResult = useCase(true)
        assertTrue(failResult is Result.Failure)
    }

    @Test
    fun `SettingsViewModel initializes with Success state`() = runTest {
        val repo = FakeSettingsRepository()
        repo.currentSettings = SettingsModel(fontSize = 18)

        val viewModel = SettingsViewModel(
            observeSettingsUseCase = ObserveSettingsUseCase(repo),
            getSettingsUseCase = GetSettingsUseCase(repo),
            updateFontSizeUseCase = UpdateFontSizeUseCase(repo),
            updateBubbleRadiusUseCase = UpdateBubbleRadiusUseCase(repo),
            updateSaveToGalleryUseCase = UpdateSaveToGalleryUseCase(repo),
            updateStreamMediaUseCase = UpdateStreamMediaUseCase(repo),
            updateSyncContactsUseCase = UpdateSyncContactsUseCase(repo)
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SettingsUiState.Success)
        assertEquals(18, (state as SettingsUiState.Success).settings.fontSize)
    }

    @Test
    fun `SettingsViewModel updates font size and emits event`() = runTest {
        val repo = FakeSettingsRepository()
        val viewModel = SettingsViewModel(
            observeSettingsUseCase = ObserveSettingsUseCase(repo),
            getSettingsUseCase = GetSettingsUseCase(repo),
            updateFontSizeUseCase = UpdateFontSizeUseCase(repo),
            updateBubbleRadiusUseCase = UpdateBubbleRadiusUseCase(repo),
            updateSaveToGalleryUseCase = UpdateSaveToGalleryUseCase(repo),
            updateStreamMediaUseCase = UpdateStreamMediaUseCase(repo),
            updateSyncContactsUseCase = UpdateSyncContactsUseCase(repo)
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val events = mutableListOf<SettingsEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }

        viewModel.setFontSize(22)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(22, repo.currentSettings.fontSize)
        val successEvent = events.firstOrNull { it is SettingsEvent.SettingChanged }
        assertTrue(successEvent != null)
        assertEquals("fontSize", (successEvent as SettingsEvent.SettingChanged).name)

        job.cancel()
    }

    @Test
    fun `SettingsViewModel handles update error and emits ShowError event`() = runTest {
        val repo = FakeSettingsRepository()
        repo.updateSuccess = false

        val viewModel = SettingsViewModel(
            observeSettingsUseCase = ObserveSettingsUseCase(repo),
            getSettingsUseCase = GetSettingsUseCase(repo),
            updateFontSizeUseCase = UpdateFontSizeUseCase(repo),
            updateBubbleRadiusUseCase = UpdateBubbleRadiusUseCase(repo),
            updateSaveToGalleryUseCase = UpdateSaveToGalleryUseCase(repo),
            updateStreamMediaUseCase = UpdateStreamMediaUseCase(repo),
            updateSyncContactsUseCase = UpdateSyncContactsUseCase(repo)
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val events = mutableListOf<SettingsEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }

        viewModel.setSaveToGallery(true)
        testDispatcher.scheduler.advanceUntilIdle()

        val errorEvent = events.firstOrNull { it is SettingsEvent.ShowError }
        assertTrue(errorEvent != null)
        assertEquals("Failed to update save to gallery", (errorEvent as SettingsEvent.ShowError).message)

        job.cancel()
    }
}
