package org.telegram.messenger.feature.social.birthdays

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
import org.telegram.messenger.BirthdayController
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.birthdays.data.datasource.BirthdayLocalDataSource
import org.telegram.messenger.feature.social.birthdays.data.datasource.BirthdayRemoteDataSource
import org.telegram.messenger.feature.social.birthdays.data.repository.BirthdaysRepositoryImpl
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account

@OptIn(ExperimentalCoroutinesApi::class)
class BirthdaysRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeLocalDataSource: FakeBirthdayLocalDataSource
    private lateinit var fakeRemoteDataSource: FakeBirthdayRemoteDataSource
    private lateinit var repository: BirthdaysRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeLocalDataSource = FakeBirthdayLocalDataSource()
        fakeRemoteDataSource = FakeBirthdayRemoteDataSource()
        repository = BirthdaysRepositoryImpl(
            currentAccount = 0,
            localDataSource = fakeLocalDataSource,
            remoteDataSource = fakeRemoteDataSource,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGetBirthdaysState() = runTest {
        val user = TLRPC.TL_user().apply {
            id = 42L
            first_name = "John"
            last_name = "Doe"
        }
        val contact = TL_account.TL_contactBirthday().apply {
            contact_id = 42L
            birthday = TL_account.TL_birthday().apply {
                day = 1
                month = 1
                year = 2000
            }
        }
        val tlResponse = TL_account.contactBirthdays().apply {
            users.add(user)
            contacts.add(contact)
        }
        fakeLocalDataSource.cachedState = BirthdayController.BirthdayState.from(tlResponse)

        val state = repository.getBirthdaysState()
        assertNotNull(state)
        assertNotNull(state?.todayKey)
    }

    @Test
    fun testCheckBirthdaysCachedWhenNotNeeded() = runTest {
        fakeLocalDataSource.shouldCheck = false
        val result = repository.checkBirthdays(force = false)

        assertTrue(result is Result.Success)
        assertEquals(0, fakeRemoteDataSource.getBirthdaysCallCount)
    }

    @Test
    fun testCheckBirthdaysSuccessWhenNeeded() = runTest {
        fakeLocalDataSource.shouldCheck = true
        val user = TLRPC.TL_user().apply {
            id = 77L
            first_name = "Alice"
        }
        fakeRemoteDataSource.response = TL_account.contactBirthdays().apply {
            users.add(user)
        }

        val result = repository.checkBirthdays(force = false)
        assertTrue(result is Result.Success)
        assertEquals(1, fakeRemoteDataSource.getBirthdaysCallCount)
        assertTrue(fakeLocalDataSource.savedResponses.isNotEmpty())
    }

    @Test
    fun testCheckBirthdaysFailure() = runTest {
        fakeLocalDataSource.shouldCheck = true
        fakeRemoteDataSource.shouldFail = true

        val result = repository.checkBirthdays(force = true)
        assertTrue(result is Result.Failure)
        assertEquals("Network timeout", (result as Result.Failure).error.message)
    }

    @Test
    fun testHideTodayBirthdays() = runTest {
        val result = repository.hideTodayBirthdays()
        assertTrue(result is Result.Success)
        assertTrue(fakeLocalDataSource.hideCalled)
    }

    @Test
    fun testIsBirthdayTodayAndHasBirthdaysToday() = runTest {
        fakeLocalDataSource.isTodayUser = 123L
        fakeLocalDataSource.hasToday = true

        assertTrue(repository.isBirthdayToday(123L))
        assertFalse(repository.isBirthdayToday(456L))
        assertTrue(repository.hasBirthdaysToday())

        fakeLocalDataSource.hasToday = false
        assertFalse(repository.hasBirthdaysToday())
    }

    @Test
    fun testStranglerHook() {
        val repo = BirthdayController.getBirthdaysRepository(0)
        assertNotNull(repo)
    }

    private class FakeBirthdayLocalDataSource : BirthdayLocalDataSource(0) {
        var cachedState: BirthdayController.BirthdayState? = null
        var shouldCheck = true
        var hideCalled = false
        var isTodayUser = -1L
        var hasToday = false
        val savedResponses = mutableListOf<TL_account.contactBirthdays>()

        override fun getBirthdaysState(): BirthdayController.BirthdayState? = cachedState

        override fun shouldCheckBirthdays(force: Boolean): Boolean {
            if (force) return true
            return shouldCheck
        }

        override suspend fun saveBirthdays(response: TL_account.contactBirthdays) {
            savedResponses.add(response)
            cachedState = BirthdayController.BirthdayState.from(response)
        }

        override suspend fun hideTodayBirthdays() {
            hideCalled = true
        }

        override fun isToday(userId: Long): Boolean = userId == isTodayUser

        override fun hasBirthdaysToday(): Boolean = hasToday
    }

    private class FakeBirthdayRemoteDataSource : BirthdayRemoteDataSource(0) {
        var shouldFail = false
        var getBirthdaysCallCount = 0
        var response = TL_account.contactBirthdays()

        override suspend fun getBirthdays(): Result<TL_account.contactBirthdays> {
            getBirthdaysCallCount++
            if (shouldFail) {
                return Result.failure("Network timeout")
            }
            return Result.Success(response)
        }
    }
}
