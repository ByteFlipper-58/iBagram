package org.telegram.messenger.feature.security.privacy

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.privacy.data.mapper.PrivacyMapper
import org.telegram.messenger.feature.security.privacy.domain.model.BlockedPeerModel
import org.telegram.messenger.feature.security.privacy.domain.model.PasscodeSettingsModel
import org.telegram.messenger.feature.security.privacy.domain.model.PrivacyRuleMode
import org.telegram.messenger.feature.security.privacy.domain.model.PrivacyRuleModel
import org.telegram.messenger.feature.security.privacy.domain.model.PrivacyRuleType
import org.telegram.messenger.feature.security.privacy.domain.model.TwoStepVerificationModel
import org.telegram.messenger.feature.security.privacy.domain.repository.PrivacyRepository
import org.telegram.messenger.feature.security.privacy.domain.usecase.BlockPrivacyPeerUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.CheckPasscodeUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.ClearPasscodeUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.GetBlockedPeersUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.GetPasscodeSettingsUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.GetPrivacyRulesUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.LoadPrivacyRulesUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.LoadTwoStepVerificationUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.ObserveBlockedPeersUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.ObservePrivacyRulesUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.ObserveTwoStepVerificationUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.SetPasscodeUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.SetPrivacyRuleUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.UnblockPrivacyPeerUseCase
import org.telegram.messenger.feature.security.privacy.presentation.PrivacyEvent
import org.telegram.messenger.feature.security.privacy.presentation.PrivacyViewModel
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account
import java.util.ArrayList

@OptIn(ExperimentalCoroutinesApi::class)
class PrivacyDomainTest {

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
    fun testDomainModels() {
        val rule = PrivacyRuleModel(
            type = PrivacyRuleType.PHONE,
            mode = PrivacyRuleMode.ALLOW_CONTACTS,
            allowedUserIds = listOf(101L, 102L),
            disallowedUserIds = listOf(201L)
        )
        assertEquals(PrivacyRuleType.PHONE, rule.type)
        assertEquals(PrivacyRuleMode.ALLOW_CONTACTS, rule.mode)
        assertEquals(2, rule.allowedUserIds.size)
        assertEquals(1, rule.disallowedUserIds.size)

        val blocked = BlockedPeerModel(peerId = 12345L, isUser = true, date = 1000)
        assertEquals(12345L, blocked.peerId)
        assertTrue(blocked.isUser)

        val passcode = PasscodeSettingsModel(
            isPasscodeSet = true,
            passcodeType = 1,
            isAppLocked = false,
            autoLockInSeconds = 300,
            useFingerprint = true,
            allowScreenCapture = false
        )
        assertTrue(passcode.isPasscodeSet)
        assertEquals(1, passcode.passcodeType)
        assertEquals(300, passcode.autoLockInSeconds)

        val twoStep = TwoStepVerificationModel(
            hasPassword = true,
            hasRecoveryEmail = true,
            emailPattern = "t***@gmail.com"
        )
        assertTrue(twoStep.hasPassword)
        assertTrue(twoStep.hasRecoveryEmail)
        assertEquals("t***@gmail.com", twoStep.emailPattern)

        assertEquals(PrivacyRuleType.LAST_SEEN, PrivacyRuleType.fromLegacy(0))
        assertEquals(PrivacyRuleType.PHOTO, PrivacyRuleType.fromLegacy(4))
        assertEquals(PrivacyRuleType.LAST_SEEN, PrivacyRuleType.fromLegacy(999))
    }

    @Test
    fun testPrivacyMapper() {
        val legacyRules = ArrayList<TLRPC.PrivacyRule>()
        legacyRules.add(TLRPC.TL_privacyValueAllowContacts())
        val allowUsers = TLRPC.TL_privacyValueAllowUsers()
        allowUsers.users.add(111L)
        legacyRules.add(allowUsers)

        val mapped = PrivacyMapper.mapPrivacyRules(PrivacyRuleType.LAST_SEEN, legacyRules)
        assertNotNull(mapped)
        assertEquals(PrivacyRuleType.LAST_SEEN, mapped!!.type)
        assertEquals(PrivacyRuleMode.ALLOW_CONTACTS, mapped.mode)
        assertEquals(listOf(111L), mapped.allowedUserIds)

        val toLegacy = PrivacyMapper.mapToLegacyRules(mapped)
        assertTrue(toLegacy.any { it is TLRPC.TL_privacyValueAllowContacts })
        assertTrue(toLegacy.any { it is TLRPC.TL_privacyValueAllowUsers && it.users.contains(111L) })

        val password = TL_account.Password().apply {
            has_password = true
            has_recovery = true
            login_email_pattern = "a***@mail.ru"
        }
        val twoStepModel = PrivacyMapper.mapTwoStepVerification(password)
        assertTrue(twoStepModel.hasPassword)
        assertTrue(twoStepModel.hasRecoveryEmail)
        assertEquals("a***@mail.ru", twoStepModel.emailPattern)

        val emptyTwoStep = PrivacyMapper.mapTwoStepVerification(null)
        assertFalse(emptyTwoStep.hasPassword)
        assertFalse(emptyTwoStep.hasRecoveryEmail)
        assertNull(emptyTwoStep.emailPattern)
    }

    @Test
    fun testUseCasesAndRepository() = runTest(testDispatcher) {
        val fakeRepo = FakePrivacyRepository()

        val observeRulesUc = ObservePrivacyRulesUseCase(fakeRepo)
        val getRulesUc = GetPrivacyRulesUseCase(fakeRepo)
        val setRuleUc = SetPrivacyRuleUseCase(fakeRepo)
        val loadRulesUc = LoadPrivacyRulesUseCase(fakeRepo)

        val observeBlockedUc = ObserveBlockedPeersUseCase(fakeRepo)
        val getBlockedUc = GetBlockedPeersUseCase(fakeRepo)
        val blockUc = BlockPrivacyPeerUseCase(fakeRepo)
        val unblockUc = UnblockPrivacyPeerUseCase(fakeRepo)

        val getPasscodeUc = GetPasscodeSettingsUseCase(fakeRepo)
        val setPasscodeUc = SetPasscodeUseCase(fakeRepo)
        val checkPasscodeUc = CheckPasscodeUseCase(fakeRepo)
        val clearPasscodeUc = ClearPasscodeUseCase(fakeRepo)

        val observeTwoStepUc = ObserveTwoStepVerificationUseCase(fakeRepo)
        val loadTwoStepUc = LoadTwoStepVerificationUseCase(fakeRepo)

        // Privacy rules
        val rule = PrivacyRuleModel(PrivacyRuleType.CALLS, PrivacyRuleMode.ALLOW_ALL)
        setRuleUc(PrivacyRuleType.CALLS, rule)
        assertEquals(rule, getRulesUc(PrivacyRuleType.CALLS))
        assertEquals(rule, observeRulesUc(PrivacyRuleType.CALLS).first())

        // Blocked
        blockUc(999L)
        assertTrue(getBlockedUc().contains(999L))
        assertEquals(listOf(999L), observeBlockedUc().first())
        unblockUc(999L)
        assertFalse(getBlockedUc().contains(999L))

        // Passcode
        assertFalse(getPasscodeUc().isPasscodeSet)
        setPasscodeUc("1234", 0)
        assertTrue(getPasscodeUc().isPasscodeSet)
        assertTrue(checkPasscodeUc("1234"))
        assertFalse(checkPasscodeUc("0000"))
        clearPasscodeUc()
        assertFalse(getPasscodeUc().isPasscodeSet)

        // 2FA
        val twoStep = loadTwoStepUc().getOrNull()
        assertNotNull(twoStep)
        assertEquals(twoStep, observeTwoStepUc().first())
    }

    @Test
    fun testViewModelStateAndEvents() = runTest(testDispatcher) {
        val fakeRepo = FakePrivacyRepository()
        val viewModel = PrivacyViewModel(
            privacyRepository = fakeRepo,
            observePrivacyRulesUseCase = ObservePrivacyRulesUseCase(fakeRepo),
            getPrivacyRulesUseCase = GetPrivacyRulesUseCase(fakeRepo),
            setPrivacyRuleUseCase = SetPrivacyRuleUseCase(fakeRepo),
            loadPrivacyRulesUseCase = LoadPrivacyRulesUseCase(fakeRepo),
            observeBlockedPeersUseCase = ObserveBlockedPeersUseCase(fakeRepo),
            getBlockedPeersUseCase = GetBlockedPeersUseCase(fakeRepo),
            blockPrivacyPeerUseCase = BlockPrivacyPeerUseCase(fakeRepo),
            unblockPrivacyPeerUseCase = UnblockPrivacyPeerUseCase(fakeRepo),
            getPasscodeSettingsUseCase = GetPasscodeSettingsUseCase(fakeRepo),
            setPasscodeUseCase = SetPasscodeUseCase(fakeRepo),
            checkPasscodeUseCase = CheckPasscodeUseCase(fakeRepo),
            clearPasscodeUseCase = ClearPasscodeUseCase(fakeRepo),
            observeTwoStepVerificationUseCase = ObserveTwoStepVerificationUseCase(fakeRepo),
            loadTwoStepVerificationUseCase = LoadTwoStepVerificationUseCase(fakeRepo)
        )

        testDispatcher.scheduler.advanceUntilIdle()

        // Test rule update event
        val newRule = PrivacyRuleModel(PrivacyRuleType.BIO, PrivacyRuleMode.ALLOW_ALL)
        viewModel.onEvent(PrivacyEvent.SetRule(PrivacyRuleType.BIO, newRule))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(newRule, viewModel.uiState.value.privacyRules[PrivacyRuleType.BIO])

        // Test block/unblock event
        viewModel.onEvent(PrivacyEvent.BlockPeer(555L))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.blockedPeers.contains(555L))
        assertEquals(1, viewModel.uiState.value.blockedCount)

        viewModel.onEvent(PrivacyEvent.UnblockPeer(555L))
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.blockedPeers.contains(555L))
        assertEquals(0, viewModel.uiState.value.blockedCount)

        // Test passcode event
        viewModel.onEvent(PrivacyEvent.SetPasscode("4321", 0))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.passcodeSettings.isPasscodeSet)

        viewModel.onEvent(PrivacyEvent.ClearPasscode)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.passcodeSettings.isPasscodeSet)

        // Test fingerprint & autolock
        viewModel.onEvent(PrivacyEvent.ToggleFingerprint(true))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.passcodeSettings.useFingerprint)

        viewModel.onEvent(PrivacyEvent.SetAutoLock(600))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(600, viewModel.uiState.value.passcodeSettings.autoLockInSeconds)

        viewModel.onEvent(PrivacyEvent.ToggleScreenCapture(true))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.passcodeSettings.allowScreenCapture)

        // Test dismiss error
        viewModel.onEvent(PrivacyEvent.DismissError)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    private class FakePrivacyRepository : PrivacyRepository {
        private val rulesMap = mutableMapOf<PrivacyRuleType, PrivacyRuleModel>()
        private val blockedList = mutableListOf<Long>()
        private var passcodeSettings = PasscodeSettingsModel()
        private var currentPasscode = ""
        private var twoStepModel = TwoStepVerificationModel(hasPassword = true, emailPattern = "test@example.com")

        private val blockedFlow = MutableStateFlow<List<Long>>(emptyList())
        private val twoStepFlow = MutableStateFlow(twoStepModel)

        override fun observePrivacyRules(type: PrivacyRuleType): Flow<PrivacyRuleModel?> =
            MutableStateFlow(rulesMap[type])

        override suspend fun getPrivacyRules(type: PrivacyRuleType): PrivacyRuleModel? = rulesMap[type]

        override suspend fun setPrivacyRules(type: PrivacyRuleType, rule: PrivacyRuleModel): Result<Unit> {
            rulesMap[type] = rule
            return Result.Success(Unit)
        }

        override suspend fun loadPrivacyRules(): Result<Unit> = Result.Success(Unit)

        override fun observeBlockedPeers(): Flow<List<Long>> = blockedFlow

        override suspend fun getBlockedPeers(): List<Long> = blockedList.toList()

        override suspend fun getBlockedCount(): Int = blockedList.size

        override suspend fun blockPeer(peerId: Long): Result<Unit> {
            if (!blockedList.contains(peerId)) {
                blockedList.add(peerId)
                blockedFlow.value = blockedList.toList()
            }
            return Result.Success(Unit)
        }

        override suspend fun unblockPeer(peerId: Long): Result<Unit> {
            blockedList.remove(peerId)
            blockedFlow.value = blockedList.toList()
            return Result.Success(Unit)
        }

        override fun getPasscodeSettings(): PasscodeSettingsModel = passcodeSettings

        override suspend fun setPasscode(passcode: String, type: Int): Result<Unit> {
            currentPasscode = passcode
            passcodeSettings = passcodeSettings.copy(isPasscodeSet = true, passcodeType = type)
            return Result.Success(Unit)
        }

        override fun checkPasscode(passcode: String): Boolean = currentPasscode == passcode

        override suspend fun clearPasscode(): Result<Unit> {
            currentPasscode = ""
            passcodeSettings = passcodeSettings.copy(isPasscodeSet = false)
            return Result.Success(Unit)
        }

        override fun setAppLocked(locked: Boolean) {
            passcodeSettings = passcodeSettings.copy(isAppLocked = locked)
        }

        override suspend fun setAutoLockIn(seconds: Int): Result<Unit> {
            passcodeSettings = passcodeSettings.copy(autoLockInSeconds = seconds)
            return Result.Success(Unit)
        }

        override suspend fun setUseFingerprint(use: Boolean): Result<Unit> {
            passcodeSettings = passcodeSettings.copy(useFingerprint = use)
            return Result.Success(Unit)
        }

        override suspend fun setAllowScreenCapture(allow: Boolean): Result<Unit> {
            passcodeSettings = passcodeSettings.copy(allowScreenCapture = allow)
            return Result.Success(Unit)
        }

        override fun observeTwoStepVerification(): Flow<TwoStepVerificationModel> = twoStepFlow

        override suspend fun loadTwoStepVerification(): Result<TwoStepVerificationModel> =
            Result.Success(twoStepModel)
    }
}
