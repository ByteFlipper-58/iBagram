package org.telegram.messenger.feature.social.profile

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.profile.data.datasource.ProfileLocalDataSource
import org.telegram.messenger.feature.social.profile.data.datasource.ProfileRemoteDataSource
import org.telegram.messenger.feature.social.profile.data.repository.ProfileRepositoryImpl
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        AccountFeatureContainer.resetAll()
    }

    @After
    fun tearDown() {
        AccountFeatureContainer.resetAll()
        Dispatchers.resetMain()
    }

    private class FakeProfileRemoteDataSource(account: Int) : ProfileRemoteDataSource(account) {
        var lastLoadedUserId: Long? = null
        var lastLoadedChatId: Long? = null
        var lastBlockedPeerId: Long? = null
        var lastUnblockedPeerId: Long? = null

        override suspend fun loadFullUser(user: TLRPC.User): Result<Unit> {
            lastLoadedUserId = user.id
            return Result.success(Unit)
        }

        override suspend fun loadFullChat(chatId: Long): Result<Unit> {
            lastLoadedChatId = chatId
            return Result.success(Unit)
        }

        override suspend fun blockPeer(peerId: Long): Result<Unit> {
            lastBlockedPeerId = peerId
            return Result.success(Unit)
        }

        override suspend fun unblockPeer(peerId: Long): Result<Unit> {
            lastUnblockedPeerId = peerId
            return Result.success(Unit)
        }
    }

    @Test
    fun testGetProfileReturnsUser() = runTest {
        val local = ProfileLocalDataSource(0)
        val remote = FakeProfileRemoteDataSource(0)
        val repo = ProfileRepositoryImpl(0, local, remote, testDispatcher)

        val user = TLRPC.TL_user().apply {
            id = 42L
            first_name = "Alice"
            last_name = "Liddell"
            username = "alice"
            phone = "+1234567"
            verified = true
            premium = true
        }
        val userFull = TLRPC.TL_userFull().apply {
            about = "Curiouser and curiouser!"
        }
        local.putUser(user)
        local.putUserFull(42L, userFull)

        val result = repo.getProfile(42L)
        assertTrue(result.isSuccess)
        val profile = result.getOrNull()
        assertNotNull(profile)
        assertEquals(42L, profile?.id)
        assertEquals("Alice Liddell", profile?.title)
        assertEquals("alice", profile?.username)
        assertEquals("+1234567", profile?.phone)
        assertEquals("Curiouser and curiouser!", profile?.bio)
        assertTrue(profile?.isVerified == true)
        assertTrue(profile?.isPremium == true)
        assertFalse(profile?.isBlocked == true)
    }

    @Test
    fun testGetProfileReturnsChat() = runTest {
        val local = ProfileLocalDataSource(0)
        val remote = FakeProfileRemoteDataSource(0)
        val repo = ProfileRepositoryImpl(0, local, remote, testDispatcher)

        val chat = TLRPC.TL_channel().apply {
            id = 100L
            title = "Wonderland Channel"
            username = "wonderland"
            broadcast = true
            participants_count = 500
        }
        val chatFull = TLRPC.TL_channelFull().apply {
            about = "Official news from Wonderland"
            participants_count = 500
        }
        local.putChat(chat)
        local.putChatFull(100L, chatFull)

        val result = repo.getProfile(-100L)
        assertTrue(result.isSuccess)
        val profile = result.getOrNull()
        assertNotNull(profile)
        assertEquals(-100L, profile?.id)
        assertEquals("Wonderland Channel", profile?.title)
        assertEquals("wonderland", profile?.username)
        assertEquals("Official news from Wonderland", profile?.bio)
        assertTrue(profile?.isChannel == true)
        assertFalse(profile?.isGroup == true)
        assertEquals(500, profile?.membersCount)
    }

    @Test
    fun testObserveProfileEmitsUpdates() = runTest {
        val local = ProfileLocalDataSource(0)
        val remote = FakeProfileRemoteDataSource(0)
        val repo = ProfileRepositoryImpl(0, local, remote, testDispatcher)

        local.putUser(TLRPC.TL_user().apply {
            id = 55L
            first_name = "Mad"
            last_name = "Hatter"
        })

        val profile = repo.observeProfile(55L).first()
        assertNotNull(profile)
        assertEquals(55L, profile?.id)
        assertEquals("Mad Hatter", profile?.title)
    }

    @Test
    fun testLoadFullProfileDelegatesToRemote() = runTest {
        val local = ProfileLocalDataSource(0)
        val remote = FakeProfileRemoteDataSource(0)
        val repo = ProfileRepositoryImpl(0, local, remote, testDispatcher)

        val user = TLRPC.TL_user().apply {
            id = 77L
            first_name = "Cheshire"
        }
        local.putUser(user)

        val res = repo.loadFullProfile(77L)
        assertTrue(res.isSuccess)
        assertEquals(77L, remote.lastLoadedUserId)
    }

    @Test
    fun testBlockAndUnblockPeer() = runTest {
        val local = ProfileLocalDataSource(0)
        val remote = FakeProfileRemoteDataSource(0)
        val repo = ProfileRepositoryImpl(0, local, remote, testDispatcher)

        assertFalse(local.isBlocked(99L))

        val blockRes = repo.blockPeer(99L)
        assertTrue(blockRes.isSuccess)
        assertTrue(local.isBlocked(99L))
        assertEquals(99L, remote.lastBlockedPeerId)

        val unblockRes = repo.unblockPeer(99L)
        assertTrue(unblockRes.isSuccess)
        assertFalse(local.isBlocked(99L))
        assertEquals(99L, remote.lastUnblockedPeerId)
    }

    @Test
    fun testContainerWiringDefaultsToImpl() {
        val container = AccountFeatureContainer.get(0)
        val repo = container.profileRepository
        assertTrue(repo is ProfileRepositoryImpl)

        val created = container.social.createProfileRepository()
        assertTrue(created is ProfileRepositoryImpl)
    }
}
