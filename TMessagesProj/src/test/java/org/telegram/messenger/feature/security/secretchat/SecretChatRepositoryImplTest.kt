package org.telegram.messenger.feature.security.secretchat

import android.content.Context
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
import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.secretchat.data.datasource.SecretChatLocalDataSource
import org.telegram.messenger.feature.security.secretchat.data.datasource.SecretChatRemoteDataSource
import org.telegram.messenger.feature.security.secretchat.data.repository.SecretChatRepositoryImpl
import org.telegram.messenger.feature.security.secretchat.domain.model.SecretChatState
import org.telegram.tgnet.TLRPC
import java.util.ArrayList

@OptIn(ExperimentalCoroutinesApi::class)
class SecretChatRepositoryImplTest {

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

    private class FakeSecretChatLocalDataSource(account: Int) : SecretChatLocalDataSource(account) {
        val chats = mutableMapOf<Int, TLRPC.EncryptedChat>()
        val users = mutableMapOf<Long, TLRPC.User>()
        val dialogs = mutableListOf<TLRPC.Dialog>()
        var frozen: Boolean = false
        var testClientUserId: Long = 12345L

        var startSecretChatCalled = false
        var acceptSecretChatCalled = false
        var declineSecretChatCalled = false
        var sendTTLMessageCalled = false
        var sendScreenshotMessageCalled = false
        var updateEncryptedChatTTLCalled = false
        var updateEncryptedChatCalled = false

        override fun getEncryptedChat(chatId: Int): TLRPC.EncryptedChat? = chats[chatId]

        override fun putEncryptedChat(chat: TLRPC.EncryptedChat, notify: Boolean) {
            chats[chat.id] = chat
        }

        override fun getUser(userId: Long): TLRPC.User? = users[userId]

        override fun getAllDialogs(): List<TLRPC.Dialog> = ArrayList(dialogs)

        override fun isFrozen(): Boolean = frozen

        override fun getClientUserId(): Long = testClientUserId

        override suspend fun updateEncryptedChatTTL(chat: TLRPC.EncryptedChat): Result<Unit> {
            updateEncryptedChatTTLCalled = true
            chats[chat.id] = chat
            return Result.success(Unit)
        }

        override suspend fun updateEncryptedChat(chat: TLRPC.EncryptedChat): Result<Unit> {
            updateEncryptedChatCalled = true
            chats[chat.id] = chat
            return Result.success(Unit)
        }

        override fun startSecretChat(context: Context?, user: TLRPC.User) {
            startSecretChatCalled = true
        }

        override fun acceptSecretChat(chat: TLRPC.EncryptedChat) {
            acceptSecretChatCalled = true
        }

        override fun declineSecretChat(chatId: Int, revoke: Boolean) {
            declineSecretChatCalled = true
            chats.remove(chatId)
        }

        override fun sendTTLMessage(chat: TLRPC.EncryptedChat, resendMessage: TLRPC.Message?) {
            sendTTLMessageCalled = true
        }

        override fun sendScreenshotMessage(
            chat: TLRPC.EncryptedChat,
            randomIds: ArrayList<Long>?,
            resendMessage: TLRPC.Message?
        ) {
            sendScreenshotMessageCalled = true
        }
    }

    private class FakeSecretChatRemoteDataSource(account: Int) : SecretChatRemoteDataSource(account) {
        var discardEncryptionCalled = false
        var discardChatId: Int = 0
        var discardRevoke: Boolean = false

        override suspend fun discardEncryption(chatId: Int, deleteHistory: Boolean): Result<Boolean> {
            discardEncryptionCalled = true
            discardChatId = chatId
            discardRevoke = deleteHistory
            return Result.success(true)
        }
    }

    private fun createRepository(
        account: Int = 0,
        localDataSource: SecretChatLocalDataSource = FakeSecretChatLocalDataSource(account),
        remoteDataSource: SecretChatRemoteDataSource = FakeSecretChatRemoteDataSource(account)
    ): SecretChatRepositoryImpl {
        return SecretChatRepositoryImpl(
            currentAccount = account,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource
        )
    }

    @Test
    fun getSecretChat_returns_mapped_model_when_found() = runTest {
        val local = FakeSecretChatLocalDataSource(0)
        val remote = FakeSecretChatRemoteDataSource(0)
        val repo = createRepository(localDataSource = local, remoteDataSource = remote)

        val chat = TLRPC.TL_encryptedChat().apply {
            id = 42
            user_id = 999L
            admin_id = 12345L
            date = 1600000000
            ttl = 30
            key_fingerprint = 123456789L
        }
        val user = TLRPC.TL_user().apply {
            id = 999L
            first_name = "Alice"
            last_name = "Smith"
        }
        local.chats[42] = chat
        local.users[999L] = user

        val result = repo.getSecretChat(42)
        assertNotNull(result)
        assertEquals(42, result?.chatId)
        assertEquals(999L, result?.userId)
        assertEquals(30, result?.ttlSeconds)
        assertEquals(SecretChatState.ACTIVE, result?.state)
        assertTrue(result?.isCreator == true)
    }

    @Test
    fun getSecretChat_returns_null_when_not_found() = runTest {
        val repo = createRepository()
        val result = repo.getSecretChat(999)
        assertNull(result)
    }

    @Test
    fun observeSecretChat_emits_initial_value() = runTest {
        val local = FakeSecretChatLocalDataSource(0)
        val repo = createRepository(localDataSource = local)

        val chat = TLRPC.TL_encryptedChat().apply {
            id = 100
            user_id = 500L
            ttl = 10
        }
        local.chats[100] = chat

        val emitted = repo.observeSecretChat(100).first()
        assertNotNull(emitted)
        assertEquals(100, emitted?.chatId)
        assertEquals(10, emitted?.ttlSeconds)
    }

    @Test
    fun observeSecretChats_emits_encrypted_chats_from_dialogs() = runTest {
        val local = FakeSecretChatLocalDataSource(0)
        val repo = createRepository(localDataSource = local)

        val chatId1 = 10
        val chatId2 = 20
        val dialogId1 = SecretChatRepositoryImpl.makeEncryptedDialogId(chatId1.toLong())
        val dialogId2 = SecretChatRepositoryImpl.makeEncryptedDialogId(chatId2.toLong())

        local.dialogs.add(TLRPC.TL_dialog().apply { id = dialogId1 })
        local.dialogs.add(TLRPC.TL_dialog().apply { id = dialogId2 })
        local.dialogs.add(TLRPC.TL_dialog().apply { id = 12345L }) // Non-encrypted dialog

        local.chats[chatId1] = TLRPC.TL_encryptedChat().apply { id = chatId1; user_id = 1L }
        local.chats[chatId2] = TLRPC.TL_encryptedChatWaiting().apply { id = chatId2; user_id = 2L }

        val list = repo.observeSecretChats().first()
        assertEquals(2, list.size)
        assertEquals(10, list[0].chatId)
        assertEquals(20, list[1].chatId)
        assertEquals(SecretChatState.ACTIVE, list[0].state)
        assertEquals(SecretChatState.WAITING, list[1].state)
    }

    @Test
    fun startSecretChat_fails_when_user_not_found() = runTest {
        val repo = createRepository()
        val result = repo.startSecretChat(999L)
        assertTrue(result is Result.Failure)
    }

    @Test
    fun startSecretChat_fails_when_account_frozen() = runTest {
        val local = FakeSecretChatLocalDataSource(0).apply {
            users[123L] = TLRPC.TL_user().apply { id = 123L }
            frozen = true
        }
        val repo = createRepository(localDataSource = local)
        val result = repo.startSecretChat(123L)
        assertTrue(result is Result.Failure)
    }

    @Test
    fun startSecretChat_succeeds_when_valid_user() = runTest {
        val local = FakeSecretChatLocalDataSource(0).apply {
            users[123L] = TLRPC.TL_user().apply { id = 123L }
            frozen = false
        }
        val repo = createRepository(localDataSource = local)
        val result = repo.startSecretChat(123L)
        assertTrue(result is Result.Success)
        assertTrue(local.startSecretChatCalled)
    }

    @Test
    fun acceptSecretChat_fails_when_chat_not_found() = runTest {
        val repo = createRepository()
        val result = repo.acceptSecretChat(404)
        assertTrue(result is Result.Failure)
    }

    @Test
    fun acceptSecretChat_succeeds_when_valid_chat() = runTest {
        val local = FakeSecretChatLocalDataSource(0).apply {
            chats[77] = TLRPC.TL_encryptedChatRequested().apply { id = 77 }
        }
        val repo = createRepository(localDataSource = local)
        val result = repo.acceptSecretChat(77)
        assertTrue(result is Result.Success)
        assertTrue(local.acceptSecretChatCalled)
    }

    @Test
    fun declineSecretChat_delegates_to_local_and_remote() = runTest {
        val local = FakeSecretChatLocalDataSource(0).apply {
            chats[88] = TLRPC.TL_encryptedChat().apply { id = 88 }
        }
        val remote = FakeSecretChatRemoteDataSource(0)
        val repo = createRepository(localDataSource = local, remoteDataSource = remote)

        val result = repo.declineSecretChat(88)
        assertTrue(result is Result.Success)
        assertTrue(local.declineSecretChatCalled)
        assertNull(local.chats[88])
    }

    @Test
    fun setTtl_fails_when_chat_not_found() = runTest {
        val repo = createRepository()
        val result = repo.setTtl(999, 60)
        assertTrue(result is Result.Failure)
    }

    @Test
    fun setTtl_updates_ttl_and_storage_when_valid_chat() = runTest {
        val local = FakeSecretChatLocalDataSource(0).apply {
            chats[55] = TLRPC.TL_encryptedChat().apply { id = 55; ttl = 0 }
        }
        val repo = createRepository(localDataSource = local)

        val result = repo.setTtl(55, 120)
        assertTrue(result is Result.Success)
        assertEquals(120, local.chats[55]?.ttl)
        assertTrue(local.sendTTLMessageCalled)
        assertTrue(local.updateEncryptedChatTTLCalled)
    }

    @Test
    fun sendScreenshotNotification_fails_when_chat_not_found() = runTest {
        val repo = createRepository()
        val result = repo.sendScreenshotNotification(999)
        assertTrue(result is Result.Failure)
    }

    @Test
    fun sendScreenshotNotification_succeeds_when_valid_chat() = runTest {
        val local = FakeSecretChatLocalDataSource(0).apply {
            chats[66] = TLRPC.TL_encryptedChat().apply { id = 66 }
        }
        val repo = createRepository(localDataSource = local)

        val result = repo.sendScreenshotNotification(66)
        assertTrue(result is Result.Success)
        assertTrue(local.sendScreenshotMessageCalled)
    }

    @Test
    fun bitwise_helpers_verify_encrypted_dialog_calculations() {
        val chatId = 12345
        val encryptedDialogId = SecretChatRepositoryImpl.makeEncryptedDialogId(chatId.toLong())

        assertTrue(SecretChatRepositoryImpl.isEncryptedDialog(encryptedDialogId))
        assertFalse(SecretChatRepositoryImpl.isEncryptedDialog(12345L))
        assertFalse(SecretChatRepositoryImpl.isEncryptedDialog(-12345L))

        val extractedChatId = SecretChatRepositoryImpl.getEncryptedChatId(encryptedDialogId)
        assertEquals(chatId, extractedChatId)
    }
}
