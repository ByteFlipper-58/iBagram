package org.telegram.messenger.feature.system.launchericon

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.launchericon.data.mapper.LauncherIconMapper
import org.telegram.messenger.feature.system.launchericon.domain.model.LauncherIconModel
import org.telegram.messenger.feature.system.launchericon.domain.model.LauncherIconType
import org.telegram.messenger.feature.system.launchericon.domain.model.LauncherIconsStateModel
import org.telegram.messenger.feature.system.launchericon.domain.repository.LauncherIconRepository
import org.telegram.messenger.feature.system.launchericon.domain.usecase.FixLauncherIconIfNeededUseCase
import org.telegram.messenger.feature.system.launchericon.domain.usecase.GetActiveLauncherIconUseCase
import org.telegram.messenger.feature.system.launchericon.domain.usecase.GetLauncherIconsUseCase
import org.telegram.messenger.feature.system.launchericon.domain.usecase.IsLauncherIconEnabledUseCase
import org.telegram.messenger.feature.system.launchericon.domain.usecase.ObserveLauncherIconsUseCase
import org.telegram.messenger.feature.system.launchericon.domain.usecase.SetLauncherIconUseCase
import org.telegram.messenger.feature.system.launchericon.presentation.LauncherIconEvent
import org.telegram.messenger.feature.system.launchericon.presentation.LauncherIconViewModel
import org.telegram.ui.LauncherIconController

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherIconDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeLauncherIconRepository : LauncherIconRepository {
        var activeType: LauncherIconType = LauncherIconType.DEFAULT
        var shouldFailSet = false
        var shouldFailFix = false

        val iconsList = mutableListOf(
            LauncherIconModel(LauncherIconType.DEFAULT, "DefaultIcon", 1, 10, 100, isPremium = false, isEnabled = true),
            LauncherIconModel(LauncherIconType.PREMIUM, "PremiumIcon", 2, 20, 200, isPremium = true, isEnabled = false),
            LauncherIconModel(LauncherIconType.TURBO, "TurboIcon", 3, 30, 300, isPremium = true, isEnabled = false)
        )

        private val stateFlow = MutableStateFlow(
            LauncherIconsStateModel(
                icons = iconsList.toList(),
                activeIcon = iconsList.first()
            )
        )

        fun updateActive(type: LauncherIconType) {
            activeType = type
            val updated = iconsList.map { it.copy(isEnabled = it.type == type) }
            iconsList.clear()
            iconsList.addAll(updated)
            stateFlow.value = LauncherIconsStateModel(
                icons = iconsList.toList(),
                activeIcon = iconsList.firstOrNull { it.isEnabled }
            )
        }

        override fun observeLauncherIcons(): Flow<LauncherIconsStateModel> = stateFlow

        override fun getLauncherIcons(): List<LauncherIconModel> = iconsList.toList()

        override fun getActiveIcon(): LauncherIconModel? = iconsList.firstOrNull { it.isEnabled }

        override fun isIconEnabled(type: LauncherIconType): Boolean = type == activeType

        override suspend fun setIcon(type: LauncherIconType): Result<Boolean> {
            return if (shouldFailSet) {
                Result.failure("Failed to enable icon component")
            } else {
                updateActive(type)
                Result.Success(true)
            }
        }

        override suspend fun fixLauncherIconIfNeeded(): Result<Boolean> {
            return if (shouldFailFix) {
                Result.failure("Failed to fix launcher icon")
            } else {
                if (iconsList.none { it.isEnabled }) {
                    updateActive(LauncherIconType.DEFAULT)
                }
                Result.Success(true)
            }
        }
    }

    @Test
    fun testLauncherIconTypeFromKey() {
        assertEquals(LauncherIconType.DEFAULT, LauncherIconType.fromKey("DefaultIcon"))
        assertEquals(LauncherIconType.PREMIUM, LauncherIconType.fromKey("PremiumIcon"))
        assertEquals(LauncherIconType.TURBO, LauncherIconType.fromKey("TurboIcon"))
        assertEquals(LauncherIconType.NOX, LauncherIconType.fromKey("NoxIcon"))
        assertEquals(LauncherIconType.AQUA, LauncherIconType.fromKey("AquaIcon"))
        assertEquals(LauncherIconType.VINTAGE, LauncherIconType.fromKey("VintageIcon"))
        assertEquals(LauncherIconType.DEFAULT, LauncherIconType.fromKey("unknown_key"))

        assertTrue(LauncherIconType.PREMIUM.isPremium)
        assertTrue(LauncherIconType.TURBO.isPremium)
        assertTrue(LauncherIconType.NOX.isPremium)
        assertFalse(LauncherIconType.DEFAULT.isPremium)
        assertFalse(LauncherIconType.VINTAGE.isPremium)
        assertFalse(LauncherIconType.AQUA.isPremium)
    }

    @Test
    fun testLauncherIconMapper() {
        assertEquals(LauncherIconType.DEFAULT, LauncherIconMapper.mapType(LauncherIconController.LauncherIcon.DEFAULT))
        assertEquals(LauncherIconType.PREMIUM, LauncherIconMapper.mapType(LauncherIconController.LauncherIcon.PREMIUM))
        assertEquals(LauncherIconType.TURBO, LauncherIconMapper.mapType(LauncherIconController.LauncherIcon.TURBO))
        assertEquals(LauncherIconType.NOX, LauncherIconMapper.mapType(LauncherIconController.LauncherIcon.NOX))
        assertEquals(LauncherIconType.AQUA, LauncherIconMapper.mapType(LauncherIconController.LauncherIcon.AQUA))
        assertEquals(LauncherIconType.VINTAGE, LauncherIconMapper.mapType(LauncherIconController.LauncherIcon.VINTAGE))

        assertEquals(LauncherIconController.LauncherIcon.DEFAULT, LauncherIconMapper.toLegacyIcon(LauncherIconType.DEFAULT))
        assertEquals(LauncherIconController.LauncherIcon.PREMIUM, LauncherIconMapper.toLegacyIcon(LauncherIconType.PREMIUM))

        val model = LauncherIconMapper.mapModel(LauncherIconController.LauncherIcon.PREMIUM, true)
        assertEquals(LauncherIconType.PREMIUM, model.type)
        assertEquals("PremiumIcon", model.key)
        assertTrue(model.isPremium)
        assertTrue(model.isEnabled)
    }

    @Test
    fun testLauncherIconUseCases() = runTest {
        val repo = FakeLauncherIconRepository()
        val getIconsUseCase = GetLauncherIconsUseCase(repo)
        val getActiveIconUseCase = GetActiveLauncherIconUseCase(repo)
        val isEnabledUseCase = IsLauncherIconEnabledUseCase(repo)
        val setIconUseCase = SetLauncherIconUseCase(repo)
        val fixIconUseCase = FixLauncherIconIfNeededUseCase(repo)

        assertEquals(3, getIconsUseCase().size)
        assertEquals(LauncherIconType.DEFAULT, getActiveIconUseCase()?.type)
        assertTrue(isEnabledUseCase(LauncherIconType.DEFAULT))
        assertFalse(isEnabledUseCase(LauncherIconType.PREMIUM))

        val setResult = setIconUseCase(LauncherIconType.PREMIUM)
        assertTrue(setResult is Result.Success && setResult.data)
        assertEquals(LauncherIconType.PREMIUM, getActiveIconUseCase()?.type)
        assertTrue(isEnabledUseCase(LauncherIconType.PREMIUM))
        assertFalse(isEnabledUseCase(LauncherIconType.DEFAULT))

        val fixResult = fixIconUseCase()
        assertTrue(fixResult is Result.Success && fixResult.data)

        // Test failures
        repo.shouldFailSet = true
        val failedSet = setIconUseCase(LauncherIconType.TURBO)
        assertTrue(failedSet is Result.Failure)

        repo.shouldFailFix = true
        val failedFix = fixIconUseCase()
        assertTrue(failedFix is Result.Failure)
    }

    @Test
    fun testLauncherIconViewModel() = runTest {
        val repo = FakeLauncherIconRepository()
        val vm = LauncherIconViewModel(
            observeLauncherIconsUseCase = ObserveLauncherIconsUseCase(repo),
            getLauncherIconsUseCase = GetLauncherIconsUseCase(repo),
            getActiveLauncherIconUseCase = GetActiveLauncherIconUseCase(repo),
            setLauncherIconUseCase = SetLauncherIconUseCase(repo),
            fixLauncherIconIfNeededUseCase = FixLauncherIconIfNeededUseCase(repo)
        )

        advanceUntilIdle()
        assertEquals(3, vm.uiState.value.icons.size)
        assertEquals(LauncherIconType.DEFAULT, vm.uiState.value.activeIcon?.type)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorMessage)

        // Select icon
        vm.onEvent(LauncherIconEvent.SelectIcon(LauncherIconType.PREMIUM))
        advanceUntilIdle()
        assertEquals(LauncherIconType.PREMIUM, vm.uiState.value.activeIcon?.type)

        // Failure handling
        repo.shouldFailSet = true
        vm.onEvent(LauncherIconEvent.SelectIcon(LauncherIconType.TURBO))
        advanceUntilIdle()
        assertEquals("Failed to enable icon component", vm.uiState.value.errorMessage)

        // Clear error
        vm.onEvent(LauncherIconEvent.ClearError)
        assertNull(vm.uiState.value.errorMessage)

        // Fix icon failure
        repo.shouldFailFix = true
        vm.onEvent(LauncherIconEvent.FixIconIfNeeded)
        advanceUntilIdle()
        assertEquals("Failed to fix launcher icon", vm.uiState.value.errorMessage)
    }

    @Test
    fun testAccountFeatureContainerIntegration() = runTest {
        val container = AccountFeatureContainer.get(0)
        assertNotNull(container.launcherIconRepository)
        assertNotNull(container.observeLauncherIconsUseCase)
        assertNotNull(container.getLauncherIconsUseCase)
        assertNotNull(container.getActiveLauncherIconUseCase)
        assertNotNull(container.isLauncherIconEnabledUseCase)
        assertNotNull(container.setLauncherIconUseCase)
        assertNotNull(container.fixLauncherIconIfNeededUseCase)

        val fakeRepo = FakeLauncherIconRepository()
        container.launcherIconRepository = fakeRepo
        assertEquals(fakeRepo, container.launcherIconRepository)
        assertEquals(fakeRepo.getActiveIcon(), container.getActiveLauncherIconUseCase())

        val vm = container.createLauncherIconViewModel()
        assertNotNull(vm)
        assertEquals(3, vm.uiState.value.icons.size)
    }
}
