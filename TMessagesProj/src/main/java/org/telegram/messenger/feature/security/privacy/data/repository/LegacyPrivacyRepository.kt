package org.telegram.messenger.feature.security.privacy.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.ContactsController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.SharedConfig
import org.telegram.messenger.Utilities
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.privacy.data.mapper.PrivacyMapper
import org.telegram.messenger.feature.security.privacy.domain.model.PasscodeSettingsModel
import org.telegram.messenger.feature.security.privacy.domain.model.PrivacyRuleMode
import org.telegram.messenger.feature.security.privacy.domain.model.PrivacyRuleModel
import org.telegram.messenger.feature.security.privacy.domain.model.PrivacyRuleType
import org.telegram.messenger.feature.security.privacy.domain.model.TwoStepVerificationModel
import org.telegram.messenger.feature.security.privacy.domain.repository.PrivacyRepository
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account
import java.nio.charset.StandardCharsets
import kotlin.coroutines.resume

class LegacyPrivacyRepository(
    private val currentAccount: Int,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : PrivacyRepository {

    private val contactsController: ContactsController
        get() = ContactsController.getInstance(currentAccount)

    private val messagesController: MessagesController
        get() = MessagesController.getInstance(currentAccount)

    private val connectionsManager: ConnectionsManager
        get() = ConnectionsManager.getInstance(currentAccount)

    override fun observePrivacyRules(type: PrivacyRuleType): Flow<PrivacyRuleModel?> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.privacyRulesUpdated)
            .map { getPrivacyRules(type) }
            .onStart { emit(getPrivacyRules(type)) }
    }

    override suspend fun getPrivacyRules(type: PrivacyRuleType): PrivacyRuleModel? = withContext(mainDispatcher) {
        val legacy = contactsController.getPrivacyRules(type.legacyType)
        PrivacyMapper.mapPrivacyRules(type, legacy)
    }

    override suspend fun setPrivacyRules(type: PrivacyRuleType, rule: PrivacyRuleModel): Result<Unit> = withContext(mainDispatcher) {
        try {
            val legacyRules = PrivacyMapper.mapToLegacyRules(rule)
            contactsController.setPrivacyRules(legacyRules, type.legacyType)
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.privacyRulesUpdated)

            val req = TL_account.setPrivacy()
            req.key = when (type) {
                PrivacyRuleType.PHONE -> TLRPC.TL_inputPrivacyKeyPhoneNumber()
                PrivacyRuleType.FORWARDS -> TLRPC.TL_inputPrivacyKeyForwards()
                PrivacyRuleType.PHOTO -> TLRPC.TL_inputPrivacyKeyProfilePhoto()
                PrivacyRuleType.P2P -> TLRPC.TL_inputPrivacyKeyPhoneP2P()
                PrivacyRuleType.CALLS -> TLRPC.TL_inputPrivacyKeyPhoneCall()
                PrivacyRuleType.INVITE -> TLRPC.TL_inputPrivacyKeyChatInvite()
                PrivacyRuleType.VOICE_MESSAGES -> TLRPC.TL_inputPrivacyKeyVoiceMessages()
                PrivacyRuleType.BIO -> TLRPC.TL_inputPrivacyKeyAbout()
                PrivacyRuleType.BIRTHDAY -> TLRPC.TL_inputPrivacyKeyBirthday()
                PrivacyRuleType.GIFTS -> TLRPC.TL_inputPrivacyKeyStarGiftsAutoSave()
                else -> TLRPC.TL_inputPrivacyKeyStatusTimestamp()
            }

            when (rule.mode) {
                PrivacyRuleMode.ALLOW_ALL -> req.rules.add(TLRPC.TL_inputPrivacyValueAllowAll())
                PrivacyRuleMode.ALLOW_CONTACTS -> req.rules.add(TLRPC.TL_inputPrivacyValueAllowContacts())
                PrivacyRuleMode.DISALLOW_ALL -> req.rules.add(TLRPC.TL_inputPrivacyValueDisallowAll())
                PrivacyRuleMode.CUSTOM -> req.rules.add(TLRPC.TL_inputPrivacyValueDisallowAll())
            }

            connectionsManager.sendRequest(req) { _, _ -> }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to set privacy rules", e))
        }
    }

    override suspend fun loadPrivacyRules(): Result<Unit> = withContext(mainDispatcher) {
        try {
            contactsController.loadPrivacySettings()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to load privacy rules", e))
        }
    }

    override fun observeBlockedPeers(): Flow<List<Long>> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.blockedUsersDidLoad)
            .map { getBlockedPeers() }
            .onStart { emit(getBlockedPeers()) }
    }

    override suspend fun getBlockedPeers(): List<Long> = withContext(mainDispatcher) {
        val list = mutableListOf<Long>()
        val dict = messagesController.blockePeers
        if (dict != null) {
            for (i in 0 until dict.size()) {
                list.add(dict.keyAt(i))
            }
        }
        list
    }

    override suspend fun getBlockedCount(): Int = withContext(mainDispatcher) {
        messagesController.totalBlockedCount
    }

    override suspend fun blockPeer(peerId: Long): Result<Unit> = withContext(mainDispatcher) {
        try {
            messagesController.blockPeer(peerId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to block peer", e))
        }
    }

    override suspend fun unblockPeer(peerId: Long): Result<Unit> = withContext(mainDispatcher) {
        try {
            messagesController.unblockPeer(peerId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to unblock peer", e))
        }
    }

    override fun getPasscodeSettings(): PasscodeSettingsModel {
        return PrivacyMapper.mapPasscodeSettings()
    }

    override suspend fun setPasscode(passcode: String, type: Int): Result<Unit> = withContext(mainDispatcher) {
        try {
            SharedConfig.passcodeSalt = ByteArray(16)
            Utilities.random.nextBytes(SharedConfig.passcodeSalt)
            val passcodeBytes = passcode.toByteArray(StandardCharsets.UTF_8)
            val bytes = ByteArray(32 + passcodeBytes.size)
            System.arraycopy(SharedConfig.passcodeSalt, 0, bytes, 0, 16)
            System.arraycopy(passcodeBytes, 0, bytes, 16, passcodeBytes.size)
            System.arraycopy(SharedConfig.passcodeSalt, 0, bytes, passcodeBytes.size + 16, 16)
            SharedConfig.passcodeHash = Utilities.bytesToHex(Utilities.computeSHA256(bytes, 0, bytes.size.toLong()))
            SharedConfig.passcodeType = type
            SharedConfig.appLocked = false
            SharedConfig.saveConfig()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to set passcode", e))
        }
    }

    override fun checkPasscode(passcode: String): Boolean {
        return SharedConfig.checkPasscode(passcode)
    }

    override suspend fun clearPasscode(): Result<Unit> = withContext(mainDispatcher) {
        try {
            SharedConfig.passcodeHash = ""
            SharedConfig.appLocked = false
            SharedConfig.saveConfig()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to clear passcode", e))
        }
    }

    override fun setAppLocked(locked: Boolean) {
        SharedConfig.appLocked = locked
        SharedConfig.saveConfig()
    }

    override suspend fun setAutoLockIn(seconds: Int): Result<Unit> = withContext(mainDispatcher) {
        try {
            SharedConfig.autoLockIn = seconds
            SharedConfig.saveConfig()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to update auto lock", e))
        }
    }

    override suspend fun setUseFingerprint(use: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            SharedConfig.useFingerprintLock = use
            SharedConfig.saveConfig()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to update fingerprint", e))
        }
    }

    override suspend fun setAllowScreenCapture(allow: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            SharedConfig.allowScreenCapture = allow
            SharedConfig.saveConfig()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to update screen capture", e))
        }
    }

    override fun observeTwoStepVerification(): Flow<TwoStepVerificationModel> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.didSetOrRemoveTwoStepPassword)
            .map { loadTwoStepVerification().getOrDefault(TwoStepVerificationModel()) }
            .onStart { emit(loadTwoStepVerification().getOrDefault(TwoStepVerificationModel())) }
    }

    override suspend fun loadTwoStepVerification(): Result<TwoStepVerificationModel> = suspendCancellableCoroutine { cont ->
        val req = TL_account.getPassword()
        connectionsManager.sendRequest(req, { response, error ->
            if (error != null) {
                cont.resume(Result.Failure(AppError.Generic(error.text ?: "Failed to load 2FA settings")))
            } else if (response is TL_account.Password) {
                val model = PrivacyMapper.mapTwoStepVerification(response)
                cont.resume(Result.Success(model))
            } else {
                cont.resume(Result.Success(TwoStepVerificationModel()))
            }
        }, ConnectionsManager.RequestFlagFailOnServerErrors or ConnectionsManager.RequestFlagWithoutLogin)
    }
}
