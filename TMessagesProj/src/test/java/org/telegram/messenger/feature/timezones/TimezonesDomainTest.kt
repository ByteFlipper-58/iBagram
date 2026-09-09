package org.telegram.messenger.feature.timezones

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
import org.telegram.messenger.feature.timezones.data.mapper.TimezoneMapper
import org.telegram.messenger.feature.timezones.domain.model.TimezoneModel
import org.telegram.messenger.feature.timezones.domain.model.TimezonesStateModel
import org.telegram.messenger.feature.timezones.domain.repository.TimezonesRepository
import org.telegram.messenger.feature.timezones.domain.usecase.FindTimezoneUseCase
import org.telegram.messenger.feature.timezones.domain.usecase.GetSystemTimezoneIdUseCase
import org.telegram.messenger.feature.timezones.domain.usecase.GetTimezoneNameUseCase
import org.telegram.messenger.feature.timezones.domain.usecase.GetTimezonesUseCase
import org.telegram.messenger.feature.timezones.domain.usecase.LoadTimezonesUseCase
import org.telegram.messenger.feature.timezones.domain.usecase.ObserveTimezonesUseCase
import org.telegram.messenger.feature.timezones.presentation.TimezonesEvent
import org.telegram.messenger.feature.timezones.presentation.TimezonesViewModel
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class TimezonesDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeTimezonesRepository : TimezonesRepository {
        val timezonesList = mutableListOf<TimezoneModel>()
        var systemTzId = "Europe/London"
        var shouldFail = false
        var lastLoadedForce = false

        private val flow = MutableStateFlow<List<TimezoneModel>>(emptyList())

        fun emitTimezones() {
            flow.value = timezonesList.toList()
        }

        override fun observeTimezones(): Flow<List<TimezoneModel>> = flow

        override suspend fun getTimezones(): List<TimezoneModel> = timezonesList.toList()

        override suspend fun loadTimezones(forceReload: Boolean): Result<List<TimezoneModel>> {
            lastLoadedForce = forceReload
            return if (shouldFail) {
                Result.failure("Failed to load timezones")
            } else {
                emitTimezones()
                Result.Success(timezonesList.toList())
            }
        }

        override fun findTimezone(id: String): TimezoneModel? {
            return timezonesList.firstOrNull { it.id == id }
        }

        override fun getSystemTimezoneId(): String = systemTzId

        override fun getTimezoneName(id: String, withOffset: Boolean): String {
            val tz = findTimezone(id)
            return if (tz != null) {
                if (withOffset) tz.displayName else tz.name
            } else {
                id
            }
        }
    }

    @Test
    fun testTimezoneModelOffsetFormatting() {
        val gmt = TimezoneModel("UTC", "Coordinated Universal Time", 0)
        assertEquals("GMT", gmt.formattedOffset)
        assertEquals("Coordinated Universal Time, GMT", gmt.displayName)

        val msk = TimezoneModel("Europe/Moscow", "Moscow", 3 * 3600)
        assertEquals("GMT+03:00", msk.formattedOffset)
        assertEquals("Moscow, GMT+03:00", msk.displayName)

        val ny = TimezoneModel("America/New_York", "New York", -5 * 3600)
        assertEquals("GMT-05:00", ny.formattedOffset)
        assertEquals("New York, GMT-05:00", ny.displayName)

        val kathmandu = TimezoneModel("Asia/Kathmandu", "Kathmandu", 5 * 3600 + 45 * 60)
        assertEquals("GMT+05:45", kathmandu.formattedOffset)
        assertEquals("Kathmandu, GMT+05:45", kathmandu.displayName)
    }

    @Test
    fun testTimezonesStateModel() {
        val emptyState = TimezonesStateModel()
        assertEquals(0, emptyState.count)
        assertFalse(emptyState.isLoading)
        assertNull(emptyState.systemTimezoneId)

        val populatedState = TimezonesStateModel(
            timezones = listOf(
                TimezoneModel("UTC", "Universal", 0),
                TimezoneModel("Europe/Paris", "Paris", 3600)
            ),
            systemTimezoneId = "Europe/Paris",
            isLoading = true
        )
        assertEquals(2, populatedState.count)
        assertTrue(populatedState.isLoading)
        assertEquals("Europe/Paris", populatedState.systemTimezoneId)
    }

    @Test
    fun testTimezoneMapper() {
        val tl = TLRPC.TL_timezone().apply {
            id = "Asia/Tokyo"
            name = "Tokyo"
            utc_offset = 9 * 3600
        }

        val domain = TimezoneMapper.toDomain(tl)
        assertNotNull(domain)
        assertEquals("Asia/Tokyo", domain?.id)
        assertEquals("Tokyo", domain?.name)
        assertEquals(9 * 3600, domain?.utcOffsetSeconds)

        val backToTl = TimezoneMapper.toTl(domain!!)
        assertEquals("Asia/Tokyo", backToTl.id)
        assertEquals("Tokyo", backToTl.name)
        assertEquals(9 * 3600, backToTl.utc_offset)

        val domainList = TimezoneMapper.toDomainList(listOf(tl))
        assertEquals(1, domainList.size)
        assertEquals("Asia/Tokyo", domainList[0].id)

        assertNull(TimezoneMapper.toDomain(null))
        assertTrue(TimezoneMapper.toDomainList(null).isEmpty())
    }

    @Test
    fun testUseCases() = runTest {
        val fakeRepo = FakeTimezonesRepository().apply {
            timezonesList.add(TimezoneModel("Europe/London", "London", 0))
            timezonesList.add(TimezoneModel("Europe/Berlin", "Berlin", 3600))
            systemTzId = "Europe/Berlin"
        }

        val observeUseCase = ObserveTimezonesUseCase(fakeRepo)
        val getUseCase = GetTimezonesUseCase(fakeRepo)
        val loadUseCase = LoadTimezonesUseCase(fakeRepo)
        val findUseCase = FindTimezoneUseCase(fakeRepo)
        val getSystemIdUseCase = GetSystemTimezoneIdUseCase(fakeRepo)
        val getNameUseCase = GetTimezoneNameUseCase(fakeRepo)

        assertEquals("Europe/Berlin", getSystemIdUseCase())
        assertEquals(2, getUseCase().size)

        val found = findUseCase("Europe/London")
        assertNotNull(found)
        assertEquals("London", found?.name)
        assertNull(findUseCase("Unknown"))

        assertEquals("Berlin", getNameUseCase("Europe/Berlin", false))
        assertEquals("Berlin, GMT+01:00", getNameUseCase("Europe/Berlin", true))
        assertEquals("NonExistent", getNameUseCase("NonExistent", false))

        val loadResult = loadUseCase(true)
        assertTrue(loadResult is Result.Success)
        assertTrue(fakeRepo.lastLoadedForce)

        fakeRepo.shouldFail = true
        val failResult = loadUseCase(false)
        assertTrue(failResult is Result.Failure)
    }

    @Test
    fun testViewModelSearchAndSelection() = runTest {
        val fakeRepo = FakeTimezonesRepository().apply {
            timezonesList.add(TimezoneModel("Europe/London", "London", 0))
            timezonesList.add(TimezoneModel("Europe/Moscow", "Moscow", 3 * 3600))
            timezonesList.add(TimezoneModel("America/New_York", "New York", -5 * 3600))
            systemTzId = "Europe/London"
        }

        val viewModel = TimezonesViewModel(
            observeTimezonesUseCase = ObserveTimezonesUseCase(fakeRepo),
            getTimezonesUseCase = GetTimezonesUseCase(fakeRepo),
            loadTimezonesUseCase = LoadTimezonesUseCase(fakeRepo),
            findTimezoneUseCase = FindTimezoneUseCase(fakeRepo),
            getSystemTimezoneIdUseCase = GetSystemTimezoneIdUseCase(fakeRepo),
            getTimezoneNameUseCase = GetTimezoneNameUseCase(fakeRepo)
        )

        assertEquals("Europe/London", viewModel.uiState.value.systemTimezoneId)

        fakeRepo.emitTimezones()
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.timezones.size)
        assertEquals(3, viewModel.uiState.value.displayedTimezones.size)

        // Search by name
        viewModel.onEvent(TimezonesEvent.Search("mosc"))
        assertEquals("mosc", viewModel.uiState.value.query)
        assertEquals(1, viewModel.uiState.value.displayedTimezones.size)
        assertEquals("Moscow", viewModel.uiState.value.displayedTimezones[0].name)

        // Search by offset
        viewModel.onEvent(TimezonesEvent.Search("+03:00"))
        assertEquals(1, viewModel.uiState.value.displayedTimezones.size)
        assertEquals("Europe/Moscow", viewModel.uiState.value.displayedTimezones[0].id)

        // Clear search
        viewModel.onEvent(TimezonesEvent.Search(""))
        assertEquals(3, viewModel.uiState.value.displayedTimezones.size)

        // Select timezone
        viewModel.onEvent(TimezonesEvent.SelectTimezone("America/New_York"))
        assertEquals("America/New_York", viewModel.uiState.value.selectedTimezoneId)
        assertEquals("New York", viewModel.uiState.value.selectedTimezone?.name)

        // Find timezone helper
        val found = viewModel.findTimezone("Europe/London")
        assertNotNull(found)
        assertEquals("London", found?.name)

        // Timezone name helper
        assertEquals("London, GMT", viewModel.getTimezoneName("Europe/London", true))
    }

    @Test
    fun testViewModelErrorHandling() = runTest {
        val fakeRepo = FakeTimezonesRepository().apply {
            shouldFail = true
        }

        val viewModel = TimezonesViewModel(
            observeTimezonesUseCase = ObserveTimezonesUseCase(fakeRepo),
            getTimezonesUseCase = GetTimezonesUseCase(fakeRepo),
            loadTimezonesUseCase = LoadTimezonesUseCase(fakeRepo),
            findTimezoneUseCase = FindTimezoneUseCase(fakeRepo),
            getSystemTimezoneIdUseCase = GetSystemTimezoneIdUseCase(fakeRepo),
            getTimezoneNameUseCase = GetTimezoneNameUseCase(fakeRepo)
        )

        viewModel.onEvent(TimezonesEvent.Load(forceReload = true))
        advanceUntilIdle()

        assertEquals("Failed to load timezones", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.onEvent(TimezonesEvent.ClearError)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testAccountFeatureContainerIntegration() = runTest {
        val container = AccountFeatureContainer.get(0)
        val fakeRepo = FakeTimezonesRepository().apply {
            timezonesList.add(TimezoneModel("Asia/Dubai", "Dubai", 4 * 3600))
            systemTzId = "Asia/Dubai"
        }
        container.timezonesRepository = fakeRepo

        assertEquals(fakeRepo, container.timezonesRepository)
        assertEquals("Asia/Dubai", container.getSystemTimezoneIdUseCase())
        assertEquals(1, container.getTimezonesUseCase().size)

        val vm = container.createTimezonesViewModel()
        assertNotNull(vm)
        assertEquals("Asia/Dubai", vm.uiState.value.systemTimezoneId)
    }
}
