package org.telegram.messenger.feature.business.stargifts

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
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.stargifts.data.mapper.StarGiftMapper
import org.telegram.messenger.feature.business.stargifts.domain.model.ProfileGiftsModel
import org.telegram.messenger.feature.business.stargifts.domain.model.SavedStarGiftModel
import org.telegram.messenger.feature.business.stargifts.domain.model.StarGiftFilter
import org.telegram.messenger.feature.business.stargifts.domain.model.StarGiftModel
import org.telegram.messenger.feature.business.stargifts.domain.model.StarGiftsCatalogModel
import org.telegram.messenger.feature.business.stargifts.domain.repository.StarGiftsRepository
import org.telegram.messenger.feature.business.stargifts.domain.usecase.GetStarGiftByIdUseCase
import org.telegram.messenger.feature.business.stargifts.domain.usecase.GetStarGiftsCatalogUseCase
import org.telegram.messenger.feature.business.stargifts.domain.usecase.LoadProfileGiftsUseCase
import org.telegram.messenger.feature.business.stargifts.domain.usecase.ObserveProfileGiftsUseCase
import org.telegram.messenger.feature.business.stargifts.domain.usecase.ObserveStarGiftsCatalogUseCase
import org.telegram.messenger.feature.business.stargifts.domain.usecase.ToggleHideProfileGiftUseCase
import org.telegram.messenger.feature.business.stargifts.domain.usecase.TogglePinProfileGiftUseCase
import org.telegram.messenger.feature.business.stargifts.presentation.StarGiftsEvent
import org.telegram.messenger.feature.business.stargifts.presentation.StarGiftsUiState
import org.telegram.messenger.feature.business.stargifts.presentation.StarGiftsViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class StarGiftsDomainTest {

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
    fun testStarGiftsModelsAndMappers() {
        val gift1 = StarGiftModel(
            id = 101L,
            stars = 50L,
            slug = "cake",
            title = "Birthday Cake",
            stickerDocumentId = 12345678L,
            isBirthday = true,
            isLimited = false,
            isSoldOut = false
        )
        val gift2 = StarGiftModel(
            id = 202L,
            stars = 500L,
            slug = "diamond",
            title = "Star Diamond",
            availabilityRemains = 5,
            availabilityTotal = 100,
            isLimited = true,
            isSoldOut = false,
            upgradeStars = 250L,
            transferStars = 50L
        )

        assertEquals(101L, gift1.id)
        assertEquals(50L, gift1.stars)
        assertTrue(gift1.isBirthday)
        assertFalse(gift1.isLimited)

        assertEquals(202L, gift2.id)
        assertTrue(gift2.isLimited)
        assertEquals(5, gift2.availabilityRemains)
        assertEquals(250L, gift2.upgradeStars)

        val savedGift = SavedStarGiftModel(
            id = 999L,
            date = 1700000000,
            gift = gift1,
            message = "Happy Birthday!",
            fromPeerId = 777L,
            isPinnedToTop = true,
            isUnsaved = false,
            canUpgrade = true,
            convertStars = 40L
        )

        assertEquals(999L, savedGift.id)
        assertEquals("Happy Birthday!", savedGift.message)
        assertTrue(savedGift.isPinnedToTop)
        assertFalse(savedGift.isUnsaved)
        assertTrue(savedGift.canUpgrade)
        assertEquals(40L, savedGift.convertStars)

        // Filter defaults
        val defaultFilter = StarGiftFilter()
        assertTrue(defaultFilter.sortByDate)
        assertTrue(defaultFilter.includeUnlimited)
        assertTrue(defaultFilter.includeDisplayed)

        // Mapper null checks
        assertNull(StarGiftMapper.mapStarGift(null))
        assertNull(StarGiftMapper.mapSavedStarGift(null))
        assertEquals(emptyList<StarGiftModel>(), StarGiftMapper.mapStarGiftList(null))
        assertEquals(emptyList<SavedStarGiftModel>(), StarGiftMapper.mapSavedStarGiftList(null))
    }

    @Test
    fun testStarGiftsUseCasesWithFakeRepository() = runTest {
        val fakeRepo = FakeStarGiftsRepository()

        val sampleGift1 = StarGiftModel(id = 1L, stars = 10L, title = "Teddy Bear")
        val sampleGift2 = StarGiftModel(id = 2L, stars = 100L, title = "Golden Trophy")
        fakeRepo.setCatalogGifts(listOf(sampleGift1, sampleGift2))

        val getCatalogUseCase = GetStarGiftsCatalogUseCase(fakeRepo)
        val getGiftByIdUseCase = GetStarGiftByIdUseCase(fakeRepo)
        val loadProfileGiftsUseCase = LoadProfileGiftsUseCase(fakeRepo)
        val togglePinUseCase = TogglePinProfileGiftUseCase(fakeRepo)
        val toggleHideUseCase = ToggleHideProfileGiftUseCase(fakeRepo)

        // Catalog
        val catalogRes = getCatalogUseCase()
        assertTrue(catalogRes is Result.Success)
        val catalog = (catalogRes as Result.Success).data
        assertEquals(2, catalog.size)
        assertEquals("Teddy Bear", catalog[0].title)

        // Single Gift
        val singleRes = getGiftByIdUseCase(2L)
        assertTrue(singleRes is Result.Success)
        assertEquals("Golden Trophy", (singleRes as Result.Success).data?.title)

        // Profile Gifts
        val saved1 = SavedStarGiftModel(id = 10L, date = 1000, gift = sampleGift1, isPinnedToTop = false)
        val saved2 = SavedStarGiftModel(id = 20L, date = 2000, gift = sampleGift2, isPinnedToTop = false)
        fakeRepo.setProfileGifts(12345L, listOf(saved1, saved2))

        val profileRes = loadProfileGiftsUseCase(12345L)
        assertTrue(profileRes is Result.Success)
        val profileData = (profileRes as Result.Success).data
        assertEquals(2, profileData.gifts.size)
        assertEquals(2, profileData.totalCount)

        // Toggle Pin
        val pinRes = togglePinUseCase(12345L, 10L, true)
        assertTrue(pinRes is Result.Success && pinRes.data)
        val afterPin = loadProfileGiftsUseCase(12345L)
        assertTrue((afterPin as Result.Success).data.gifts.first { it.id == 10L }.isPinnedToTop)

        // Toggle Hide
        val hideRes = toggleHideUseCase(12345L, 20L, true)
        assertTrue(hideRes is Result.Success && hideRes.data)
        val afterHide = loadProfileGiftsUseCase(12345L)
        assertTrue((afterHide as Result.Success).data.gifts.first { it.id == 20L }.isUnsaved)
    }

    @Test
    fun testStarGiftsViewModelWorkflow() = runTest {
        val fakeRepo = FakeStarGiftsRepository()
        val gift = StarGiftModel(id = 50L, stars = 25L, title = "Magic Wand")
        fakeRepo.setCatalogGifts(listOf(gift))

        val viewModel = StarGiftsViewModel(
            observeStarGiftsCatalogUseCase = ObserveStarGiftsCatalogUseCase(fakeRepo),
            getStarGiftsCatalogUseCase = GetStarGiftsCatalogUseCase(fakeRepo),
            getStarGiftByIdUseCase = GetStarGiftByIdUseCase(fakeRepo),
            observeProfileGiftsUseCase = ObserveProfileGiftsUseCase(fakeRepo),
            loadProfileGiftsUseCase = LoadProfileGiftsUseCase(fakeRepo),
            togglePinProfileGiftUseCase = TogglePinProfileGiftUseCase(fakeRepo),
            toggleHideProfileGiftUseCase = ToggleHideProfileGiftUseCase(fakeRepo)
        )

        advanceUntilIdle()

        // Catalog loaded
        val loadedState = viewModel.uiState.value
        assertTrue(loadedState is StarGiftsUiState.Success)
        val successState = loadedState as StarGiftsUiState.Success
        assertEquals(1, successState.catalog.size)
        assertEquals(50L, successState.catalog[0].id)

        // Select Gift
        viewModel.onEvent(StarGiftsEvent.SelectGift(50L))
        advanceUntilIdle()

        val selectedState = viewModel.uiState.value as StarGiftsUiState.Success
        assertNotNull(selectedState.selectedGift)
        assertEquals("Magic Wand", selectedState.selectedGift?.title)

        // Load profile gifts
        val saved = SavedStarGiftModel(id = 500L, date = 5000, gift = gift)
        fakeRepo.setProfileGifts(99L, listOf(saved))

        viewModel.onEvent(StarGiftsEvent.LoadProfileGifts(99L))
        advanceUntilIdle()

        val profileState = viewModel.uiState.value as StarGiftsUiState.Success
        assertEquals(1, profileState.profileGifts?.gifts?.size)
        assertEquals(500L, profileState.profileGifts?.gifts?.get(0)?.id)

        // Toggle Pin via ViewModel
        viewModel.onEvent(StarGiftsEvent.TogglePin(99L, 500L, true))
        advanceUntilIdle()

        // Clear error
        viewModel.onEvent(StarGiftsEvent.ClearError)
        val finalState = viewModel.uiState.value as StarGiftsUiState.Success
        assertNull(finalState.errorMessage)
    }

    private class FakeStarGiftsRepository : StarGiftsRepository {
        private val catalogList = mutableListOf<StarGiftModel>()
        private val catalogStateFlow = MutableStateFlow(StarGiftsCatalogModel())

        private val profileGiftsMap = mutableMapOf<Long, MutableList<SavedStarGiftModel>>()
        private val profileFlowMap = mutableMapOf<Long, MutableStateFlow<ProfileGiftsModel>>()

        fun setCatalogGifts(gifts: List<StarGiftModel>) {
            catalogList.clear()
            catalogList.addAll(gifts)
            catalogStateFlow.value = StarGiftsCatalogModel(catalogList.toList())
        }

        fun setProfileGifts(dialogId: Long, gifts: List<SavedStarGiftModel>) {
            val list = profileGiftsMap.computeIfAbsent(dialogId) { mutableListOf() }
            list.clear()
            list.addAll(gifts)
            val flow = profileFlowMap.computeIfAbsent(dialogId) {
                MutableStateFlow(ProfileGiftsModel(dialogId = dialogId))
            }
            flow.value = ProfileGiftsModel(
                dialogId = dialogId,
                gifts = list.toList(),
                totalCount = list.size
            )
        }

        override fun observeCatalog(): Flow<StarGiftsCatalogModel> = catalogStateFlow.asStateFlow()

        override suspend fun getCatalog(forceRefresh: Boolean): Result<List<StarGiftModel>> {
            return Result.Success(catalogList.toList())
        }

        override suspend fun getGift(giftId: Long): Result<StarGiftModel?> {
            val found = catalogList.firstOrNull { it.id == giftId }
            return Result.Success(found)
        }

        override fun observeProfileGifts(dialogId: Long): Flow<ProfileGiftsModel> {
            return profileFlowMap.computeIfAbsent(dialogId) {
                MutableStateFlow(ProfileGiftsModel(dialogId = dialogId))
            }.asStateFlow()
        }

        override suspend fun loadProfileGifts(
            dialogId: Long,
            offset: String?,
            limit: Int,
            filter: StarGiftFilter
        ): Result<ProfileGiftsModel> {
            val list = profileGiftsMap[dialogId] ?: emptyList()
            val model = ProfileGiftsModel(
                dialogId = dialogId,
                gifts = list.toList(),
                totalCount = list.size
            )
            return Result.Success(model)
        }

        override suspend fun togglePinGift(
            dialogId: Long,
            giftId: Long,
            pin: Boolean
        ): Result<Boolean> {
            val list = profileGiftsMap[dialogId] ?: return Result.failure("Not found")
            val index = list.indexOfFirst { it.id == giftId }
            if (index >= 0) {
                list[index] = list[index].copy(isPinnedToTop = pin)
                profileFlowMap[dialogId]?.value = ProfileGiftsModel(dialogId, list.toList(), list.size)
                return Result.Success(true)
            }
            return Result.failure("Gift not found")
        }

        override suspend fun toggleHideGift(
            dialogId: Long,
            giftId: Long,
            hide: Boolean
        ): Result<Boolean> {
            val list = profileGiftsMap[dialogId] ?: return Result.failure("Not found")
            val index = list.indexOfFirst { it.id == giftId }
            if (index >= 0) {
                list[index] = list[index].copy(isUnsaved = hide)
                profileFlowMap[dialogId]?.value = ProfileGiftsModel(dialogId, list.toList(), list.size)
                return Result.Success(true)
            }
            return Result.failure("Gift not found")
        }
    }
}
