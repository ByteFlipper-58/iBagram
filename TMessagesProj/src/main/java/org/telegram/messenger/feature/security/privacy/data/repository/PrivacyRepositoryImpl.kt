package org.telegram.messenger.feature.security.privacy.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.privacy.data.datasource.PrivacyLocalDataSource
import org.telegram.messenger.feature.security.privacy.data.datasource.PrivacyRemoteDataSource
import org.telegram.messenger.feature.security.privacy.data.mapper.PrivacyMapper
import org.telegram.messenger.feature.security.privacy.domain.model.PasscodeSettingsModel
import org.telegram.messenger.feature.security.privacy.domain.model.PrivacyRuleMode
import org.telegram.messenger.feature.security.privacy.domain.model.PrivacyRuleModel
import org.telegram.messenger.feature.security.privacy.domain.model.PrivacyRuleType
import org.telegram.messenger.feature.security.privacy.domain.model.TwoStepVerificationModel
import org.telegram.messenger.feature.security.privacy.domain.repository.PrivacyRepository
import org.telegram.tgnet.TLRPC

/**
 * Modern repository implementation managing privacy rules, blocklist, passcode, and 2FA password.
 */
class PrivacyRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: PrivacyLocalDataSource,
    private val remoteDataSource: PrivacyRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : PrivacyRepository {

    override fun observePrivacyRules(type: PrivacyRuleType): Flow<PrivacyRuleModel?> {
        return localDataSource.observePrivacyRulesEvents()
            .map { getPrivacyRules(type) }
            .onStart { emit(getPrivacyRules(type)) }
    }

    override suspend fun getPrivacyRules(type: PrivacyRuleType): PrivacyRuleModel? = withContext(mainDispatcher) {
        val legacy = localDataSource.getPrivacyRules(type.legacyType)
        PrivacyMapper.mapPrivacyRules(type, legacy)
    }

    override suspend fun setPrivacyRules(type: PrivacyRuleType, rule: PrivacyRuleModel): Result<Unit> = withContext(mainDispatcher) {
        try {
            val legacyRules = PrivacyMapper.mapToLegacyRules(rule)
            localDataSource.setPrivacyRules(legacyRules, type.legacyType)

            val key = when (type) {
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

            val inputRules = mutableListOf<TLRPC.InputPrivacyRule>()
            when (rule.mode) {
                PrivacyRuleMode.ALLOW_ALL -> inputRules.add(TLRPC.TL_inputPrivacyValueAllowAll())
                PrivacyRuleMode.ALLOW_CONTACTS -> inputRules.add(TLRPC.TL_inputPrivacyValueAllowContacts())
                PrivacyRuleMode.DISALLOW_ALL -> inputRules.add(TLRPC.TL_inputPrivacyValueDisallowAll())
                PrivacyRuleMode.CUSTOM -> inputRules.add(TLRPC.TL_inputPrivacyValueDisallowAll())
            }

            remoteDataSource.setPrivacy(key, inputRules)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to set privacy rules", e))
        }
    }

    override suspend fun loadPrivacyRules(): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.loadPrivacySettings()
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to load privacy rules", e))
        }
    }

    override fun observeBlockedPeers(): Flow<List<Long>> {
        return localDataSource.observeBlockedUsersEvents()
            .map { getBlockedPeers() }
            .onStart { emit(getBlockedPeers()) }
    }

    override suspend fun getBlockedPeers(): List<Long> = withContext(mainDispatcher) {
        localDataSource.getBlockedPeers()
    }

    override suspend fun getBlockedCount(): Int = withContext(mainDispatcher) {
        localDataSource.getBlockedCount()
    }

    override suspend fun blockPeer(peerId: Long): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.blockPeer(peerId)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to block peer", e))
        }
    }

    override suspend fun unblockPeer(peerId: Long): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.unblockPeer(peerId)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to unblock peer", e))
        }
    }

    override fun getPasscodeSettings(): PasscodeSettingsModel {
        return localDataSource.getPasscodeSettings()
    }

    override suspend fun setPasscode(passcode: String, type: Int): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.setPasscode(passcode, type)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to set passcode", e))
        }
    }

    override fun checkPasscode(passcode: String): Boolean {
        return localDataSource.checkPasscode(passcode)
    }

    override suspend fun clearPasscode(): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.clearPasscode()
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to clear passcode", e))
        }
    }

    override fun setAppLocked(locked: Boolean) {
        localDataSource.setAppLocked(locked)
    }

    override suspend fun setAutoLockIn(seconds: Int): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.setAutoLockIn(seconds)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to update auto lock", e))
        }
    }

    override suspend fun setUseFingerprint(use: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.setUseFingerprint(use)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to update fingerprint", e))
        }
    }

    override suspend fun setAllowScreenCapture(allow: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.setAllowScreenCapture(allow)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to update screen capture", e))
        }
    }

    override fun observeTwoStepVerification(): Flow<TwoStepVerificationModel> {
        return localDataSource.observeTwoStepEvents()
            .map { loadTwoStepVerification().getOrDefault(TwoStepVerificationModel()) }
            .onStart { emit(loadTwoStepVerification().getOrDefault(TwoStepVerificationModel())) }
    }

    override suspend fun loadTwoStepVerification(): Result<TwoStepVerificationModel> = withContext(mainDispatcher) {
        val result = remoteDataSource.loadTwoStepVerification()
        result.map { PrivacyMapper.mapTwoStepVerification(it) }
    }
}
