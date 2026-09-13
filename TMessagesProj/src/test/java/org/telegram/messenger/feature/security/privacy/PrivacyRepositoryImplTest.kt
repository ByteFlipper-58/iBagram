package org.telegram.messenger.feature.security.privacy

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
import org.telegram.messenger.feature.security.privacy.data.datasource.PrivacyLocalDataSource
import org.telegram.messenger.feature.security.privacy.data.datasource.PrivacyRemoteDataSource
import org.telegram.messenger.feature.security.privacy.data.repository.PrivacyRepositoryImpl
import org.telegram.messenger.feature.security.privacy.domain.model.PrivacyRuleMode
import org.telegram.messenger.feature.security.privacy.domain.model.PrivacyRuleModel
import org.telegram.messenger.feature.security.privacy.domain.model.PrivacyRuleType
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account
import java.util.ArrayList

@OptIn(ExperimentalCoroutinesApi::class)
class PrivacyRepositoryImplTest {

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

    private class FakePrivacyRemoteDataSource(account: Int) : PrivacyRemoteDataSource(account) {
        var lastSetKey: TLRPC.InputPrivacyKey? = null
        var lastSetRulesCount: Int = 0

        override suspend fun setPrivacy(
            key: TLRPC.InputPrivacyKey,
            rules: List<TLRPC.InputPrivacyRule>
        ): Result<Unit> {
            lastSetKey = key
            lastSetRulesCount = rules.size
            return Result.success(Unit)
        }

        override suspend fun loadTwoStepVerification(): Result<TL_account.Password> {
            val pass = TL_account.Password().apply {
                has_password = true
                has_recovery = true
                login_email_pattern = "a***@gmail.com"
            }
            return Result.success(pass)
        }
    }

    @Test
    fun testGetAndSetPrivacyRules() = runTest {
        val local = PrivacyLocalDataSource(0)
        val remote = FakePrivacyRemoteDataSource(0)
        val repo = PrivacyRepositoryImpl(0, local, remote, testDispatcher)

        val rule = PrivacyRuleModel(
            type = PrivacyRuleType.PHONE,
            mode = PrivacyRuleMode.ALLOW_CONTACTS
        )

        val setRes = repo.setPrivacyRules(PrivacyRuleType.PHONE, rule)
        assertTrue(setRes.isSuccess)
        assertNotNull(remote.lastSetKey)

        val retrieved = repo.getPrivacyRules(PrivacyRuleType.PHONE)
        assertNotNull(retrieved)
        assertEquals(PrivacyRuleMode.ALLOW_CONTACTS, retrieved?.mode)
    }

    @Test
    fun testBlockedPeersManagement() = runTest {
        val local = PrivacyLocalDataSource(0)
        val remote = FakePrivacyRemoteDataSource(0)
        val repo = PrivacyRepositoryImpl(0, local, remote, testDispatcher)

        assertEquals(0, repo.getBlockedCount())
        assertTrue(repo.getBlockedPeers().isEmpty())

        val blockRes = repo.blockPeer(1001L)
        assertTrue(blockRes.isSuccess)
        assertEquals(1, repo.getBlockedCount())
        assertEquals(listOf(1001L), repo.getBlockedPeers())

        val unblockRes = repo.unblockPeer(1001L)
        assertTrue(unblockRes.isSuccess)
        assertEquals(0, repo.getBlockedCount())
    }

    @Test
    fun testPasscodeManagement() = runTest {
        val local = PrivacyLocalDataSource(0)
        val remote = FakePrivacyRemoteDataSource(0)
        val repo = PrivacyRepositoryImpl(0, local, remote, testDispatcher)

        assertFalse(repo.getPasscodeSettings().isPasscodeSet)

        val setRes = repo.setPasscode("1234", 0)
        assertTrue(setRes.isSuccess)
        assertTrue(repo.checkPasscode("1234"))
        assertFalse(repo.checkPasscode("0000"))

        val settings = repo.getPasscodeSettings()
        assertTrue(settings.isPasscodeSet)

        repo.setAutoLockIn(60)
        repo.setUseFingerprint(true)
        repo.setAllowScreenCapture(true)
        repo.setAppLocked(true)

        val updatedSettings = repo.getPasscodeSettings()
        assertEquals(60, updatedSettings.autoLockInSeconds)
        assertTrue(updatedSettings.useFingerprint)
        assertTrue(updatedSettings.allowScreenCapture)
        assertTrue(updatedSettings.isAppLocked)

        val clearRes = repo.clearPasscode()
        assertTrue(clearRes.isSuccess)
        assertFalse(repo.getPasscodeSettings().isPasscodeSet)
    }

    @Test
    fun testLoadTwoStepVerification() = runTest {
        val local = PrivacyLocalDataSource(0)
        val remote = FakePrivacyRemoteDataSource(0)
        val repo = PrivacyRepositoryImpl(0, local, remote, testDispatcher)

        val result = repo.loadTwoStepVerification()
        assertTrue(result.isSuccess)
        val model = result.getOrNull()
        assertNotNull(model)
        assertTrue(model?.hasPassword == true)
        assertTrue(model?.hasRecoveryEmail == true)
        assertEquals("a***@gmail.com", model?.emailPattern)
    }

    @Test
    fun testContainerWiringDefaultsToImpl() {
        val container = AccountFeatureContainer.get(0)
        val repo = container.privacyRepository
        assertTrue(repo is PrivacyRepositoryImpl)

        val created = container.security.createPrivacyRepository()
        assertTrue(created is PrivacyRepositoryImpl)
    }
}
