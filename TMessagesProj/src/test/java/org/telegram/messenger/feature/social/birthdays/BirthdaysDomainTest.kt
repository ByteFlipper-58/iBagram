package org.telegram.messenger.feature.social.birthdays

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
import org.telegram.messenger.BirthdayController
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.birthdays.data.mapper.BirthdayMapper
import org.telegram.messenger.feature.social.birthdays.domain.model.BirthdayDateModel
import org.telegram.messenger.feature.social.birthdays.domain.model.BirthdayStateModel
import org.telegram.messenger.feature.social.birthdays.domain.model.BirthdayUserModel
import org.telegram.messenger.feature.social.birthdays.domain.repository.BirthdaysRepository
import org.telegram.messenger.feature.social.birthdays.domain.usecase.CheckBirthdaysUseCase
import org.telegram.messenger.feature.social.birthdays.domain.usecase.GetBirthdaysStateUseCase
import org.telegram.messenger.feature.social.birthdays.domain.usecase.HasBirthdaysTodayUseCase
import org.telegram.messenger.feature.social.birthdays.domain.usecase.HideTodayBirthdaysUseCase
import org.telegram.messenger.feature.social.birthdays.domain.usecase.IsBirthdayTodayUseCase
import org.telegram.messenger.feature.social.birthdays.domain.usecase.ObserveBirthdaysUseCase
import org.telegram.messenger.feature.social.birthdays.presentation.BirthdaysEvent
import org.telegram.messenger.feature.social.birthdays.presentation.BirthdaysViewModel
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account

@OptIn(ExperimentalCoroutinesApi::class)
class BirthdaysDomainTest {

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
    fun testBirthdayUserModelDisplayName() {
        val userWithFullNames = BirthdayUserModel(
            id = 100L,
            firstName = "Pavel",
            lastName = "Durov",
            username = "durov"
        )
        assertEquals("Pavel Durov", userWithFullNames.displayName)

        val userFirstNameOnly = BirthdayUserModel(
            id = 101L,
            firstName = "Nikolai",
            lastName = ""
        )
        assertEquals("Nikolai", userFirstNameOnly.displayName)

        val userLastNameOnly = BirthdayUserModel(
            id = 102L,
            firstName = "",
            lastName = "Durov"
        )
        assertEquals("Durov", userLastNameOnly.displayName)

        val userUsernameOnly = BirthdayUserModel(
            id = 103L,
            firstName = "",
            lastName = "",
            username = "telegram_dev"
        )
        assertEquals("telegram_dev", userUsernameOnly.displayName)

        val userIdOnly = BirthdayUserModel(
            id = 104L,
            firstName = "",
            lastName = ""
        )
        assertEquals("104", userIdOnly.displayName)
    }

    @Test
    fun testBirthdayStateModel() {
        val alice = BirthdayUserModel(1L, "Alice", "")
        val bob = BirthdayUserModel(2L, "Bob", "")
        val charlie = BirthdayUserModel(3L, "Charlie", "")

        val state = BirthdayStateModel(
            yesterdayKey = "10_10_2024",
            todayKey = "11_10_2024",
            tomorrowKey = "12_10_2024",
            yesterday = listOf(alice),
            today = listOf(bob),
            tomorrow = listOf(charlie)
        )

        assertFalse(state.isTodayEmpty)
        assertTrue(state.contains(1L))
        assertTrue(state.contains(2L))
        assertTrue(state.contains(3L))
        assertFalse(state.contains(999L))

        val emptyState = BirthdayStateModel(
            yesterdayKey = "10_10_2024",
            todayKey = "11_10_2024",
            tomorrowKey = "12_10_2024",
            today = emptyList()
        )
        assertTrue(emptyState.isTodayEmpty)
    }

    @Test
    fun testBirthdayMapper() {
        val tlUser = TLRPC.TL_user().apply {
            id = 555L
            first_name = "Alex"
            last_name = "Johnson"
            username = "alex_j"
        }

        val mappedUser = BirthdayMapper.mapUser(tlUser)
        assertNotNull(mappedUser)
        assertEquals(555L, mappedUser?.id)
        assertEquals("Alex", mappedUser?.firstName)
        assertEquals("Johnson", mappedUser?.lastName)
        assertEquals("alex_j", mappedUser?.username)
        assertEquals("Alex Johnson", mappedUser?.displayName)

        assertNull(BirthdayMapper.mapUser(null))

        val tlBirthday = TL_account.TL_birthday().apply {
            day = 15
            month = 7
            year = 1995
        }

        val mappedBirthday = BirthdayMapper.mapBirthday(tlBirthday)
        assertNotNull(mappedBirthday)
        assertEquals(15, mappedBirthday?.day)
        assertEquals(7, mappedBirthday?.month)
        assertEquals(1995, mappedBirthday?.year)

        assertNull(BirthdayMapper.mapBirthday(null))

        val tlContact = TL_account.TL_contactBirthday().apply {
            contact_id = 555L
            birthday = tlBirthday
        }

        val mappedContact = BirthdayMapper.mapContact(tlContact, tlUser)
        assertNotNull(mappedContact)
        assertEquals(555L, mappedContact?.contactId)
        assertEquals(15, mappedContact?.birthday?.day)
        assertEquals(555L, mappedContact?.user?.id)

        assertNull(BirthdayMapper.mapContact(null, null))

        val tlAccountBirthdays = TL_account.contactBirthdays().apply {
            contacts.add(tlContact)
            users.add(tlUser)
        }

        val legacyState = BirthdayController.BirthdayState.from(tlAccountBirthdays)
        val mappedState = BirthdayMapper.mapState(legacyState)
        assertNotNull(mappedState)
        assertNotNull(mappedState?.todayKey)
        assertNotNull(mappedState?.yesterdayKey)
        assertNotNull(mappedState?.tomorrowKey)

        assertNull(BirthdayMapper.mapState(null))
    }

    @Test
    fun testBirthdaysUseCases() = runTest(testDispatcher) {
        val fakeRepo = FakeBirthdaysRepository()

        val observeBirthdaysUseCase = ObserveBirthdaysUseCase(fakeRepo)
        val getBirthdaysStateUseCase = GetBirthdaysStateUseCase(fakeRepo)
        val checkBirthdaysUseCase = CheckBirthdaysUseCase(fakeRepo)
        val hideTodayBirthdaysUseCase = HideTodayBirthdaysUseCase(fakeRepo)
        val isBirthdayTodayUseCase = IsBirthdayTodayUseCase(fakeRepo)
        val hasBirthdaysTodayUseCase = HasBirthdaysTodayUseCase(fakeRepo)

        val state = getBirthdaysStateUseCase()
        assertNotNull(state)
        assertEquals(1, state?.today?.size)

        val hasToday = hasBirthdaysTodayUseCase()
        assertTrue(hasToday)

        val isToday = isBirthdayTodayUseCase(2L)
        assertTrue(isToday)
        assertFalse(isBirthdayTodayUseCase(999L))

        val checkResult = checkBirthdaysUseCase()
        assertTrue(checkResult is Result.Success)
        assertNotNull((checkResult as Result.Success).data)

        val hideResult = hideTodayBirthdaysUseCase()
        assertTrue(hideResult is Result.Success)

        val observed = observeBirthdaysUseCase().first()
        assertNotNull(observed)
        assertEquals(1, observed?.today?.size)
    }

    @Test
    fun testBirthdaysViewModel() = runTest(testDispatcher) {
        val fakeRepo = FakeBirthdaysRepository()
        val viewModel = BirthdaysViewModel(
            observeBirthdaysUseCase = ObserveBirthdaysUseCase(fakeRepo),
            getBirthdaysStateUseCase = GetBirthdaysStateUseCase(fakeRepo),
            checkBirthdaysUseCase = CheckBirthdaysUseCase(fakeRepo),
            hideTodayBirthdaysUseCase = HideTodayBirthdaysUseCase(fakeRepo),
            isBirthdayTodayUseCase = IsBirthdayTodayUseCase(fakeRepo),
            hasBirthdaysTodayUseCase = HasBirthdaysTodayUseCase(fakeRepo)
        )
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertNotNull(state.state)
        assertTrue(state.hasTodayBirthdays)
        assertTrue(state.isBannerVisible)
        assertEquals(1, state.todayBirthdaysCount)

        viewModel.onEvent(BirthdaysEvent.DismissTodayBanner)
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertFalse(state.isBannerVisible)

        viewModel.onEvent(BirthdaysEvent.CheckBirthdays(force = true))
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)

        assertTrue(viewModel.isBirthdayToday(2L))
        assertTrue(viewModel.hasBirthdaysToday())
    }

    private class FakeBirthdaysRepository : BirthdaysRepository {
        val user = BirthdayUserModel(2L, "Birthday", "Person")
        val sampleState = BirthdayStateModel(
            yesterdayKey = "yesterday",
            todayKey = "today",
            tomorrowKey = "tomorrow",
            today = listOf(user)
        )

        val stateFlow = MutableSharedFlow<BirthdayStateModel?>(replay = 1).apply {
            tryEmit(sampleState)
        }

        override fun observeBirthdays(): Flow<BirthdayStateModel?> = stateFlow

        override suspend fun getBirthdaysState(): BirthdayStateModel? = sampleState

        override suspend fun checkBirthdays(force: Boolean): Result<BirthdayStateModel?> =
            Result.Success(sampleState)

        override suspend fun hideTodayBirthdays(): Result<Unit> = Result.Success(Unit)

        override suspend fun isBirthdayToday(userId: Long): Boolean = userId == 2L

        override suspend fun hasBirthdaysToday(): Boolean = true
    }
}
