package org.telegram.messenger.feature.profile

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.profile.data.mapper.ProfileMapper
import org.telegram.messenger.feature.profile.domain.model.ProfileModel
import org.telegram.messenger.feature.profile.domain.repository.ProfileRepository
import org.telegram.messenger.feature.profile.domain.usecase.BlockPeerUseCase
import org.telegram.messenger.feature.profile.domain.usecase.GetProfileUseCase
import org.telegram.messenger.feature.profile.domain.usecase.LoadFullProfileUseCase
import org.telegram.messenger.feature.profile.domain.usecase.ObserveProfileUseCase
import org.telegram.messenger.feature.profile.domain.usecase.UnblockPeerUseCase
import org.telegram.messenger.feature.profile.presentation.ProfileEvent
import org.telegram.messenger.feature.profile.presentation.ProfileUiState
import org.telegram.messenger.feature.profile.presentation.ProfileViewModel
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeProfileRepository : ProfileRepository {
        val profileFlow = MutableSharedFlow<ProfileModel?>(replay = 1)
        var currentProfile: ProfileModel? = null
        var blockSuccess = true
        var unblockSuccess = true
        var loadFullSuccess = true

        override fun observeProfile(id: Long): Flow<ProfileModel?> = profileFlow.asSharedFlow()

        override suspend fun getProfile(id: Long): Result<ProfileModel> {
            val p = currentProfile
            return if (p != null && p.id == id) {
                Result.success(p)
            } else {
                Result.failure(AppError.NotFound("Not found"))
            }
        }

        override suspend fun loadFullProfile(id: Long): Result<ProfileModel> {
            return if (loadFullSuccess) {
                val updated = currentProfile?.copy(bio = "Full bio loaded") ?: ProfileModel(id = id, title = "Loaded")
                currentProfile = updated
                profileFlow.emit(updated)
                Result.success(updated)
            } else {
                Result.failure(AppError.Generic("Network error"))
            }
        }

        override suspend fun blockPeer(id: Long): Result<Unit> {
            return if (blockSuccess) {
                currentProfile = currentProfile?.copy(isBlocked = true)
                profileFlow.emit(currentProfile)
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Failed to block"))
            }
        }

        override suspend fun unblockPeer(id: Long): Result<Unit> {
            return if (unblockSuccess) {
                currentProfile = currentProfile?.copy(isBlocked = false)
                profileFlow.emit(currentProfile)
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Failed to unblock"))
            }
        }
    }

    @Test
    fun testProfileMapper_user() {
        val user = TLRPC.TL_user().apply {
            id = 123L
            first_name = "Pavel"
            last_name = "Durov"
            username = "durov"
            phone = "+123456789"
            bot = false
            verified = true
            premium = true
        }
        val userFull = TLRPC.TL_userFull().apply {
            about = "Telegram Founder"
            blocked = false
        }

        val profile = ProfileMapper.mapToDomain(user, userFull, isBlocked = false)

        assertEquals(123L, profile.id)
        assertEquals("Pavel Durov", profile.title)
        assertEquals("Pavel", profile.firstName)
        assertEquals("Durov", profile.lastName)
        assertEquals("durov", profile.username)
        assertEquals("+123456789", profile.phone)
        assertEquals("Telegram Founder", profile.bio)
        assertFalse(profile.isBot)
        assertFalse(profile.isChannel)
        assertFalse(profile.isGroup)
        assertTrue(profile.isVerified)
        assertTrue(profile.isPremium)
        assertFalse(profile.isBlocked)
        assertNull(profile.membersCount)
    }

    @Test
    fun testProfileMapper_chat() {
        val chat = TLRPC.TL_channel().apply {
            id = 456L
            title = "Telegram News"
            username = "telegram"
            broadcast = true
            verified = true
            participants_count = 5000000
        }
        val chatFull = TLRPC.TL_channelFull().apply {
            about = "Official news"
            participants_count = 5000000
        }

        val profile = ProfileMapper.mapToDomain(chat, chatFull, isBlocked = false)

        assertEquals(-456L, profile.id)
        assertEquals("Telegram News", profile.title)
        assertNull(profile.firstName)
        assertNull(profile.lastName)
        assertEquals("telegram", profile.username)
        assertNull(profile.phone)
        assertEquals("Official news", profile.bio)
        assertFalse(profile.isBot)
        assertTrue(profile.isChannel)
        assertFalse(profile.isGroup)
        assertTrue(profile.isVerified)
        assertFalse(profile.isPremium)
        assertFalse(profile.isBlocked)
        assertEquals(5000000, profile.membersCount)
    }

    @Test
    fun testObserveProfileUseCase() = runTest(testDispatcher) {
        val repo = FakeProfileRepository()
        val p = ProfileModel(id = 1L, title = "Test User")
        repo.currentProfile = p
        repo.profileFlow.emit(p)

        val useCase = ObserveProfileUseCase(repo)
        val result = useCase(1L).first()

        assertEquals(1L, result?.id)
        assertEquals("Test User", result?.title)
    }

    @Test
    fun testGetProfileUseCase_success() = runTest(testDispatcher) {
        val repo = FakeProfileRepository()
        val p = ProfileModel(id = 2L, title = "Alice")
        repo.currentProfile = p

        val useCase = GetProfileUseCase(repo)
        val result = useCase(2L)

        assertTrue(result.isSuccess)
        assertEquals("Alice", (result as Result.Success).data.title)
    }

    @Test
    fun testGetProfileUseCase_notFound() = runTest(testDispatcher) {
        val repo = FakeProfileRepository()
        val useCase = GetProfileUseCase(repo)
        val result = useCase(999L)

        assertTrue(result.isFailure)
        assertTrue((result as Result.Failure).error is AppError.NotFound)
    }

    @Test
    fun testLoadFullProfileUseCase() = runTest(testDispatcher) {
        val repo = FakeProfileRepository()
        val p = ProfileModel(id = 3L, title = "Bob", bio = null)
        repo.currentProfile = p

        val useCase = LoadFullProfileUseCase(repo)
        val result = useCase(3L)

        assertTrue(result.isSuccess)
        assertEquals("Full bio loaded", (result as Result.Success).data.bio)
    }

    @Test
    fun testBlockAndUnblockPeerUseCases() = runTest(testDispatcher) {
        val repo = FakeProfileRepository()
        val p = ProfileModel(id = 4L, title = "Charlie", isBlocked = false)
        repo.currentProfile = p

        val blockUseCase = BlockPeerUseCase(repo)
        val blockResult = blockUseCase(4L)
        assertTrue(blockResult.isSuccess)
        assertTrue(repo.currentProfile?.isBlocked == true)

        val unblockUseCase = UnblockPeerUseCase(repo)
        val unblockResult = unblockUseCase(4L)
        assertTrue(unblockResult.isSuccess)
        assertFalse(repo.currentProfile?.isBlocked == true)
    }

    @Test
    fun testProfileViewModel_initialStateAndObserve() = runTest(testDispatcher) {
        val repo = FakeProfileRepository()
        val p = ProfileModel(id = 10L, title = "Diana")
        repo.currentProfile = p
        repo.profileFlow.emit(p)

        val viewModel = ProfileViewModel(
            account = 0,
            peerId = 10L,
            observeProfileUseCase = ObserveProfileUseCase(repo),
            loadFullProfileUseCase = LoadFullProfileUseCase(repo),
            blockPeerUseCase = BlockPeerUseCase(repo),
            unblockPeerUseCase = UnblockPeerUseCase(repo)
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ProfileUiState.Success)
        val success = state as ProfileUiState.Success
        assertEquals(10L, success.profile.id)
        assertEquals("Diana", success.profile.title)
        assertFalse(success.isUpdatingBlockState)
    }

    @Test
    fun testProfileViewModel_onToggleBlock_blockAndUnblock() = runTest(testDispatcher) {
        val repo = FakeProfileRepository()
        val p = ProfileModel(id = 20L, title = "Eve", isBlocked = false)
        repo.currentProfile = p
        repo.profileFlow.emit(p)

        val viewModel = ProfileViewModel(
            account = 0,
            peerId = 20L,
            observeProfileUseCase = ObserveProfileUseCase(repo),
            loadFullProfileUseCase = LoadFullProfileUseCase(repo),
            blockPeerUseCase = BlockPeerUseCase(repo),
            unblockPeerUseCase = UnblockPeerUseCase(repo)
        )

        advanceUntilIdle()

        val events = mutableListOf<ProfileEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }

        // Block
        viewModel.onToggleBlock()
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertEquals(ProfileEvent.BlockStateChanged(true), events[0])
        assertTrue((viewModel.uiState.value as ProfileUiState.Success).profile.isBlocked)

        // Unblock
        viewModel.onToggleBlock()
        advanceUntilIdle()

        assertEquals(2, events.size)
        assertEquals(ProfileEvent.BlockStateChanged(false), events[1])
        assertFalse((viewModel.uiState.value as ProfileUiState.Success).profile.isBlocked)

        job.cancel()
    }

    @Test
    fun testProfileViewModel_onToggleBlock_failure() = runTest(testDispatcher) {
        val repo = FakeProfileRepository().apply { blockSuccess = false }
        val p = ProfileModel(id = 30L, title = "Frank", isBlocked = false)
        repo.currentProfile = p
        repo.profileFlow.emit(p)

        val viewModel = ProfileViewModel(
            account = 0,
            peerId = 30L,
            observeProfileUseCase = ObserveProfileUseCase(repo),
            loadFullProfileUseCase = LoadFullProfileUseCase(repo),
            blockPeerUseCase = BlockPeerUseCase(repo),
            unblockPeerUseCase = UnblockPeerUseCase(repo)
        )

        advanceUntilIdle()

        val events = mutableListOf<ProfileEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }

        viewModel.onToggleBlock()
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events[0] is ProfileEvent.ShowError)
        assertEquals("Failed to block", (events[0] as ProfileEvent.ShowError).message)

        job.cancel()
    }
}
