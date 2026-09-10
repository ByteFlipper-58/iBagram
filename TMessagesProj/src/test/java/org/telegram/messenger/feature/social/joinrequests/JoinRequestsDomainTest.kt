package org.telegram.messenger.feature.social.joinrequests

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
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.joinrequests.data.mapper.JoinRequestMapper
import org.telegram.messenger.feature.social.joinrequests.domain.model.ChatPendingRequestsModel
import org.telegram.messenger.feature.social.joinrequests.domain.model.JoinRequestModel
import org.telegram.messenger.feature.social.joinrequests.domain.model.JoinRequestUserModel
import org.telegram.messenger.feature.social.joinrequests.domain.model.JoinRequestsListModel
import org.telegram.messenger.feature.social.joinrequests.domain.repository.JoinRequestsRepository
import org.telegram.messenger.feature.social.joinrequests.domain.usecase.ApproveAllJoinRequestsUseCase
import org.telegram.messenger.feature.social.joinrequests.domain.usecase.ApproveJoinRequestUseCase
import org.telegram.messenger.feature.social.joinrequests.domain.usecase.DismissAllJoinRequestsUseCase
import org.telegram.messenger.feature.social.joinrequests.domain.usecase.DismissJoinRequestUseCase
import org.telegram.messenger.feature.social.joinrequests.domain.usecase.GetCachedJoinRequestsUseCase
import org.telegram.messenger.feature.social.joinrequests.domain.usecase.GetPendingRequestsCountUseCase
import org.telegram.messenger.feature.social.joinrequests.domain.usecase.LoadJoinRequestsUseCase
import org.telegram.messenger.feature.social.joinrequests.domain.usecase.ObservePendingRequestsUseCase
import org.telegram.messenger.feature.social.joinrequests.presentation.JoinRequestsEvent
import org.telegram.messenger.feature.social.joinrequests.presentation.JoinRequestsViewModel
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class JoinRequestsDomainTest {

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
    fun testJoinRequestUserModelDisplayName() {
        val userWithBothNames = JoinRequestUserModel(
            id = 100L,
            firstName = "John",
            lastName = "Doe",
            username = "johndoe"
        )
        assertEquals("John Doe", userWithBothNames.displayName)

        val userWithFirstNameOnly = JoinRequestUserModel(
            id = 101L,
            firstName = "Alice",
            lastName = ""
        )
        assertEquals("Alice", userWithFirstNameOnly.displayName)

        val userWithLastNameOnly = JoinRequestUserModel(
            id = 102L,
            firstName = "",
            lastName = "Smith"
        )
        assertEquals("Smith", userWithLastNameOnly.displayName)

        val userWithUsernameOnly = JoinRequestUserModel(
            id = 103L,
            firstName = "",
            lastName = "",
            username = "crypto_fan"
        )
        assertEquals("crypto_fan", userWithUsernameOnly.displayName)

        val userWithIdOnly = JoinRequestUserModel(
            id = 104L,
            firstName = "",
            lastName = ""
        )
        assertEquals("104", userWithIdOnly.displayName)
    }

    @Test
    fun testJoinRequestMapper() {
        val tlUser = TLRPC.TL_user().apply {
            id = 12345L
            first_name = "Bob"
            last_name = "Builder"
            username = "bob_the_builder"
            phone = "+123456789"
            bot = false
            verified = true
            premium = true
        }

        val mappedUser = JoinRequestMapper.mapUser(tlUser)
        assertNotNull(mappedUser)
        assertEquals(12345L, mappedUser?.id)
        assertEquals("Bob", mappedUser?.firstName)
        assertEquals("Builder", mappedUser?.lastName)
        assertEquals("bob_the_builder", mappedUser?.username)
        assertEquals("+123456789", mappedUser?.phone)
        assertFalse(mappedUser!!.isBot)
        assertTrue(mappedUser.isVerified)
        assertTrue(mappedUser.isPremium)
        assertEquals("Bob Builder", mappedUser.displayName)

        assertNull(JoinRequestMapper.mapUser(null))

        val importer = TLRPC.TL_chatInviteImporter().apply {
            user_id = 12345L
            date = 1600000000
            about = "Hello! Please let me in."
            requested = true
            via_chatlist = false
        }

        val mappedImporter = JoinRequestMapper.mapImporter(
            chatId = 777L,
            importer = importer,
            userMap = mapOf(12345L to tlUser)
        )
        assertEquals(12345L, mappedImporter.userId)
        assertEquals(777L, mappedImporter.chatId)
        assertEquals(1600000000, mappedImporter.date)
        assertEquals("Hello! Please let me in.", mappedImporter.about)
        assertTrue(mappedImporter.isRequested)
        assertFalse(mappedImporter.viaChatlist)
        assertEquals("Bob Builder", mappedImporter.user?.displayName)

        val tlImporters = TLRPC.TL_messages_chatInviteImporters().apply {
            count = 1
            importers.add(importer)
            users.add(tlUser)
        }

        val mappedList = JoinRequestMapper.mapImportersList(777L, tlImporters)
        assertEquals(1, mappedList.totalCount)
        assertEquals(1, mappedList.requests.size)
        assertFalse(mappedList.hasMore)

        val emptyMappedList = JoinRequestMapper.mapImportersList(777L, null)
        assertEquals(0, emptyMappedList.totalCount)
        assertTrue(emptyMappedList.requests.isEmpty())
        assertFalse(emptyMappedList.hasMore)

        val tlChatFull = TLRPC.TL_channelFull().apply {
            id = 777L
            requests_pending = 5
            recent_requesters.add(12345L)
            recent_requesters.add(67890L)
        }

        val mappedPending = JoinRequestMapper.mapChatPendingRequests(777L, tlChatFull)
        assertEquals(777L, mappedPending.chatId)
        assertEquals(5, mappedPending.pendingCount)
        assertEquals(listOf(12345L, 67890L), mappedPending.recentRequestersUserIds)

        val nullChatPending = JoinRequestMapper.mapChatPendingRequests(777L, null)
        assertEquals(777L, nullChatPending.chatId)
        assertEquals(0, nullChatPending.pendingCount)
        assertTrue(nullChatPending.recentRequestersUserIds.isEmpty())
    }

    @Test
    fun testJoinRequestsUseCases() = runTest(testDispatcher) {
        val fakeRepo = FakeJoinRequestsRepository()

        val observePendingRequestsUseCase = ObservePendingRequestsUseCase(fakeRepo)
        val getPendingRequestsCountUseCase = GetPendingRequestsCountUseCase(fakeRepo)
        val getCachedJoinRequestsUseCase = GetCachedJoinRequestsUseCase(fakeRepo)
        val loadJoinRequestsUseCase = LoadJoinRequestsUseCase(fakeRepo)
        val approveJoinRequestUseCase = ApproveJoinRequestUseCase(fakeRepo)
        val dismissJoinRequestUseCase = DismissJoinRequestUseCase(fakeRepo)
        val approveAllJoinRequestsUseCase = ApproveAllJoinRequestsUseCase(fakeRepo)
        val dismissAllJoinRequestsUseCase = DismissAllJoinRequestsUseCase(fakeRepo)

        val cached = getCachedJoinRequestsUseCase(777L)
        assertNotNull(cached)
        assertEquals(1, cached?.size)

        val countResult = getPendingRequestsCountUseCase(777L)
        assertTrue(countResult is Result.Success)
        assertEquals(1, (countResult as Result.Success).data.pendingCount)

        val loaded = loadJoinRequestsUseCase(777L)
        assertTrue(loaded is Result.Success)
        assertEquals(1, (loaded as Result.Success).data.requests.size)

        val approveResult = approveJoinRequestUseCase(777L, 100L)
        assertTrue(approveResult is Result.Success)

        val dismissResult = dismissJoinRequestUseCase(777L, 100L)
        assertTrue(dismissResult is Result.Success)

        val approveAllResult = approveAllJoinRequestsUseCase(777L)
        assertTrue(approveAllResult is Result.Success)

        val dismissAllResult = dismissAllJoinRequestsUseCase(777L)
        assertTrue(dismissAllResult is Result.Success)

        val observed = observePendingRequestsUseCase(777L).first()
        assertEquals(777L, observed.chatId)
        assertEquals(1, observed.pendingCount)
    }

    @Test
    fun testJoinRequestsViewModel() = runTest(testDispatcher) {
        val fakeRepo = FakeJoinRequestsRepository()
        val viewModel = JoinRequestsViewModel(
            observePendingRequestsUseCase = ObservePendingRequestsUseCase(fakeRepo),
            getCachedJoinRequestsUseCase = GetCachedJoinRequestsUseCase(fakeRepo),
            loadJoinRequestsUseCase = LoadJoinRequestsUseCase(fakeRepo),
            approveJoinRequestUseCase = ApproveJoinRequestUseCase(fakeRepo),
            dismissJoinRequestUseCase = DismissJoinRequestUseCase(fakeRepo),
            approveAllJoinRequestsUseCase = ApproveAllJoinRequestsUseCase(fakeRepo),
            dismissAllJoinRequestsUseCase = DismissAllJoinRequestsUseCase(fakeRepo)
        )

        viewModel.onEvent(JoinRequestsEvent.Load(777L))
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(777L, state.chatId)
        assertEquals(1, state.pendingCount)
        assertEquals(1, state.requests.size)
        assertFalse(state.isLoading)

        viewModel.onEvent(JoinRequestsEvent.Search("query"))
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals("query", state.searchQuery)

        viewModel.onEvent(JoinRequestsEvent.Approve(100L))
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(0, state.requests.size)
        assertEquals(0, state.pendingCount)
        assertEquals("Request approved", state.actionSuccessMessage)

        viewModel.onEvent(JoinRequestsEvent.ClearMessages)
        state = viewModel.uiState.value
        assertNull(state.actionSuccessMessage)

        viewModel.onEvent(JoinRequestsEvent.Dismiss(100L))
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals("Request dismissed", state.actionSuccessMessage)

        viewModel.onEvent(JoinRequestsEvent.ApproveAll())
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals("All requests approved", state.actionSuccessMessage)

        viewModel.onEvent(JoinRequestsEvent.DismissAll())
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals("All requests dismissed", state.actionSuccessMessage)
    }

    private class FakeJoinRequestsRepository : JoinRequestsRepository {
        val pendingFlow = MutableSharedFlow<ChatPendingRequestsModel>(replay = 1).apply {
            tryEmit(ChatPendingRequestsModel(chatId = 777L, pendingCount = 1, recentRequestersUserIds = listOf(100L)))
        }

        val fakeUser = JoinRequestUserModel(
            id = 100L,
            firstName = "Test",
            lastName = "User"
        )
        val fakeRequest = JoinRequestModel(
            userId = 100L,
            chatId = 777L,
            date = 1600000000,
            about = "Please accept me",
            user = fakeUser
        )

        override fun observePendingRequests(chatId: Long): Flow<ChatPendingRequestsModel> = pendingFlow

        override suspend fun getPendingRequestsCount(chatId: Long): Result<ChatPendingRequestsModel> =
            Result.Success(ChatPendingRequestsModel(chatId, 1, listOf(100L)))

        override suspend fun getCachedRequests(chatId: Long): List<JoinRequestModel>? =
            listOf(fakeRequest)

        override suspend fun loadRequests(
            chatId: Long,
            query: String?,
            offsetUserId: Long?,
            offsetDate: Int?,
            limit: Int
        ): Result<JoinRequestsListModel> =
            Result.Success(JoinRequestsListModel(totalCount = 1, requests = listOf(fakeRequest), hasMore = false))

        override suspend fun approveRequest(chatId: Long, userId: Long): Result<Unit> =
            Result.Success(Unit)

        override suspend fun dismissRequest(chatId: Long, userId: Long): Result<Unit> =
            Result.Success(Unit)

        override suspend fun approveAllRequests(chatId: Long, inviteLink: String?): Result<Unit> =
            Result.Success(Unit)

        override suspend fun dismissAllRequests(chatId: Long, inviteLink: String?): Result<Unit> =
            Result.Success(Unit)
    }
}
