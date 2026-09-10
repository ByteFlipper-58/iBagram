package org.telegram.messenger.feature.security.privacy.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.privacy.domain.model.PasscodeSettingsModel
import org.telegram.messenger.feature.security.privacy.domain.model.PrivacyRuleModel
import org.telegram.messenger.feature.security.privacy.domain.model.PrivacyRuleType
import org.telegram.messenger.feature.security.privacy.domain.model.TwoStepVerificationModel

interface PrivacyRepository {
    fun observePrivacyRules(type: PrivacyRuleType): Flow<PrivacyRuleModel?>
    suspend fun getPrivacyRules(type: PrivacyRuleType): PrivacyRuleModel?
    suspend fun setPrivacyRules(type: PrivacyRuleType, rule: PrivacyRuleModel): Result<Unit>
    suspend fun loadPrivacyRules(): Result<Unit>

    fun observeBlockedPeers(): Flow<List<Long>>
    suspend fun getBlockedPeers(): List<Long>
    suspend fun getBlockedCount(): Int
    suspend fun blockPeer(peerId: Long): Result<Unit>
    suspend fun unblockPeer(peerId: Long): Result<Unit>

    fun getPasscodeSettings(): PasscodeSettingsModel
    suspend fun setPasscode(passcode: String, type: Int): Result<Unit>
    fun checkPasscode(passcode: String): Boolean
    suspend fun clearPasscode(): Result<Unit>
    fun setAppLocked(locked: Boolean)
    suspend fun setAutoLockIn(seconds: Int): Result<Unit>
    suspend fun setUseFingerprint(use: Boolean): Result<Unit>
    suspend fun setAllowScreenCapture(allow: Boolean): Result<Unit>

    fun observeTwoStepVerification(): Flow<TwoStepVerificationModel>
    suspend fun loadTwoStepVerification(): Result<TwoStepVerificationModel>
}
