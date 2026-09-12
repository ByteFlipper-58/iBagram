package org.telegram.messenger.feature.social.joinrequests

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.MemberRequestsController
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.joinrequests.data.datasource.JoinRequestsLocalDataSource
import org.telegram.messenger.feature.social.joinrequests.data.datasource.JoinRequestsRemoteDataSource
import org.telegram.messenger.feature.social.joinrequests.data.repository.JoinRequestsRepositoryImpl
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class JoinRequestsRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeLocalDataSource: FakeJoinRequestsLocalDataSource
    private lateinit var fakeRemoteDataSource: FakeJoinRequestsRemoteDataSource
    private lateinit var repository: JoinRequestsRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeLocalDataSource = FakeJoinRequestsLocalDataSource()
        fakeRemoteDataSource = FakeJoinRequestsRemoteDataSource()
        repository = JoinRequestsRepositoryImpl(
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
    fun testGetPendingRequestsCount() = runTest {
        val chatFull = TLRPC.TL_channelFull().apply {
            requests_pending = 5
            recent_requesters.add(10L)
            recent_requesters.add(20L)
        }
        fakeLocalDataSource.chatFullToReturn = chatFull

        val result = repository.getPendingRequestsCount(100L)
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(100L, data.chatId)
        assertEquals(5, data.pendingCount)
        assertEquals(2, data.recentRequestersUserIds.size)
    }

    @Test
    fun testGetCachedRequests() = runTest {
        val importer = TLRPC.TL_chatInviteImporter().apply {
            user_id = 99L
            date = 1000
        }
        val user = TLRPC.TL_user().apply {
            id = 99L
            first_name = "Charlie"
        }
        val importers = TLRPC.TL_messages_chatInviteImporters().apply {
            count = 1
            this.importers.add(importer)
            users.add(user)
        }
        fakeLocalDataSource.cachedImportersToReturn = importers

        val cached = repository.getCachedRequests(100L)
        assertNotNull(cached)
        assertEquals(1, cached?.size)
        assertEquals(99L, cached?.get(0)?.user?.id)

        fakeLocalDataSource.cachedImportersToReturn = null
        assertNull(repository.getCachedRequests(100L))
    }

    @Test
    fun testLoadRequestsSuccessAndCache() = runTest {
        val importer = TLRPC.TL_chatInviteImporter().apply {
            user_id = 101L
            date = 2000
        }
        val user = TLRPC.TL_user().apply {
            id = 101L
            first_name = "David"
        }
        val importers = TLRPC.TL_messages_chatInviteImporters().apply {
            count = 1
            this.importers.add(importer)
            users.add(user)
        }
        fakeRemoteDataSource.importersResponse = importers

        val result = repository.loadRequests(chatId = 100L)
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(1, data.totalCount)
        assertEquals(1, data.requests.size)
        assertEquals(100L, data.requests[0].chatId)
        assertEquals(101L, data.requests[0].user?.id)
        assertTrue(fakeLocalDataSource.putCachedCalls.isNotEmpty())
    }

    @Test
    fun testLoadRequestsFailure() = runTest {
        fakeRemoteDataSource.shouldFail = true
        val result = repository.loadRequests(chatId = 100L)
        assertTrue(result is Result.Failure)
        assertEquals("Network timeout", (result as Result.Failure).error.message)
    }

    @Test
    fun testApproveAndDismissRequest() = runTest {
        val updates = TLRPC.TL_updates()
        fakeRemoteDataSource.updatesResponse = updates

        val approveResult = repository.approveRequest(chatId = 100L, userId = 50L)
        assertTrue(approveResult is Result.Success)
        assertTrue(fakeRemoteDataSource.lastApproved == true)
        assertEquals(1, fakeLocalDataSource.processUpdatesCalls)

        val dismissResult = repository.dismissRequest(chatId = 100L, userId = 50L)
        assertTrue(dismissResult is Result.Success)
        assertTrue(fakeRemoteDataSource.lastApproved == false)
        assertEquals(2, fakeLocalDataSource.processUpdatesCalls)
    }

    @Test
    fun testApproveAndDismissAllRequests() = runTest {
        val updates = TLRPC.TL_updates()
        fakeRemoteDataSource.updatesResponse = updates

        val approveAllResult = repository.approveAllRequests(chatId = 100L, inviteLink = "https://t.me/+link")
        assertTrue(approveAllResult is Result.Success)
        assertTrue(fakeRemoteDataSource.lastApproved == true)
        assertEquals("https://t.me/+link", fakeRemoteDataSource.lastInviteLink)

        val dismissAllResult = repository.dismissAllRequests(chatId = 100L, inviteLink = null)
        assertTrue(dismissAllResult is Result.Success)
        assertTrue(fakeRemoteDataSource.lastApproved == false)
        assertNull(fakeRemoteDataSource.lastInviteLink)
    }

    @Test
    fun testStranglerHook() {
        val repo = MemberRequestsController.getJoinRequestsRepository(0)
        assertNotNull(repo)
    }

    private class FakeJoinRequestsLocalDataSource : JoinRequestsLocalDataSource(0) {
        var chatFullToReturn: TLRPC.ChatFull? = null
        var cachedImportersToReturn: TLRPC.TL_messages_chatInviteImporters? = null
        var peerToReturn: TLRPC.InputPeer? = TLRPC.TL_inputPeerChannel().apply { channel_id = 100L }
        var userToReturn: TLRPC.User? = TLRPC.TL_user().apply { id = 50L }
        val putCachedCalls = mutableListOf<TLRPC.TL_messages_chatInviteImporters>()
        var processUpdatesCalls = 0

        override fun getChatFull(chatId: Long): TLRPC.ChatFull? = chatFullToReturn

        override fun getInputPeer(dialogId: Long): TLRPC.InputPeer? = peerToReturn

        override fun getUser(userId: Long): TLRPC.User? = userToReturn

        override fun getInputUser(user: TLRPC.User): TLRPC.InputUser? =
            TLRPC.TL_inputUser().apply { user_id = user.id }

        override fun getCachedImporters(chatId: Long): TLRPC.TL_messages_chatInviteImporters? =
            cachedImportersToReturn

        override fun putCachedImporters(chatId: Long, importers: TLRPC.TL_messages_chatInviteImporters) {
            putCachedCalls.add(importers)
            cachedImportersToReturn = importers
        }

        override suspend fun processUpdates(updates: TLRPC.Updates) {
            processUpdatesCalls++
        }
    }

    private class FakeJoinRequestsRemoteDataSource : JoinRequestsRemoteDataSource(0) {
        var shouldFail = false
        var importersResponse = TLRPC.TL_messages_chatInviteImporters()
        var updatesResponse: TLRPC.Updates = TLRPC.TL_updates()
        var lastApproved: Boolean? = null
        var lastInviteLink: String? = null

        override suspend fun getChatInviteImporters(
            peer: TLRPC.InputPeer,
            requested: Boolean,
            limit: Int,
            query: String?,
            offsetUser: TLRPC.InputUser,
            offsetDate: Int
        ): Result<TLRPC.TL_messages_chatInviteImporters> {
            if (shouldFail) return Result.failure("Network timeout")
            return Result.Success(importersResponse)
        }

        override suspend fun hideChatJoinRequest(
            peer: TLRPC.InputPeer,
            inputUser: TLRPC.InputUser,
            approved: Boolean
        ): Result<TLRPC.Updates> {
            lastApproved = approved
            if (shouldFail) return Result.failure("Network timeout")
            return Result.Success(updatesResponse)
        }

        override suspend fun hideAllChatJoinRequests(
            peer: TLRPC.InputPeer,
            inviteLink: String?,
            approved: Boolean
        ): Result<TLRPC.Updates> {
            lastApproved = approved
            lastInviteLink = inviteLink
            if (shouldFail) return Result.failure("Network timeout")
            return Result.Success(updatesResponse)
        }
    }
}
