package org.telegram.messenger.feature.security.privacy.data.mapper

import org.telegram.messenger.SharedConfig
import org.telegram.messenger.feature.security.privacy.domain.model.PasscodeSettingsModel
import org.telegram.messenger.feature.security.privacy.domain.model.PrivacyRuleMode
import org.telegram.messenger.feature.security.privacy.domain.model.PrivacyRuleModel
import org.telegram.messenger.feature.security.privacy.domain.model.PrivacyRuleType
import org.telegram.messenger.feature.security.privacy.domain.model.TwoStepVerificationModel
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account
import java.util.ArrayList

object PrivacyMapper {

    fun mapPrivacyRules(type: PrivacyRuleType, rules: ArrayList<TLRPC.PrivacyRule>?): PrivacyRuleModel? {
        if (rules == null) return null

        var mode = PrivacyRuleMode.DISALLOW_ALL
        val allowedUsers = mutableListOf<Long>()
        val disallowedUsers = mutableListOf<Long>()
        val allowedChats = mutableListOf<Long>()
        val disallowedChats = mutableListOf<Long>()

        for (rule in rules) {
            when (rule) {
                is TLRPC.TL_privacyValueAllowAll -> mode = PrivacyRuleMode.ALLOW_ALL
                is TLRPC.TL_privacyValueAllowContacts -> mode = PrivacyRuleMode.ALLOW_CONTACTS
                is TLRPC.TL_privacyValueDisallowAll -> mode = PrivacyRuleMode.DISALLOW_ALL
                is TLRPC.TL_privacyValueAllowUsers -> allowedUsers.addAll(rule.users)
                is TLRPC.TL_privacyValueDisallowUsers -> disallowedUsers.addAll(rule.users)
                is TLRPC.TL_privacyValueAllowChatParticipants -> allowedChats.addAll(rule.chats)
                is TLRPC.TL_privacyValueDisallowChatParticipants -> disallowedChats.addAll(rule.chats)
            }
        }

        if (allowedUsers.isNotEmpty() || disallowedUsers.isNotEmpty() || allowedChats.isNotEmpty() || disallowedChats.isNotEmpty()) {
            if (mode != PrivacyRuleMode.ALLOW_ALL && mode != PrivacyRuleMode.ALLOW_CONTACTS && mode != PrivacyRuleMode.DISALLOW_ALL) {
                mode = PrivacyRuleMode.CUSTOM
            }
        }

        return PrivacyRuleModel(
            type = type,
            mode = mode,
            allowedUserIds = allowedUsers,
            disallowedUserIds = disallowedUsers,
            allowedChatIds = allowedChats,
            disallowedChatIds = disallowedChats
        )
    }

    fun mapToLegacyRules(rule: PrivacyRuleModel): ArrayList<TLRPC.PrivacyRule> {
        val list = ArrayList<TLRPC.PrivacyRule>()
        when (rule.mode) {
            PrivacyRuleMode.ALLOW_ALL -> list.add(TLRPC.TL_privacyValueAllowAll())
            PrivacyRuleMode.ALLOW_CONTACTS -> list.add(TLRPC.TL_privacyValueAllowContacts())
            PrivacyRuleMode.DISALLOW_ALL -> list.add(TLRPC.TL_privacyValueDisallowAll())
            PrivacyRuleMode.CUSTOM -> list.add(TLRPC.TL_privacyValueDisallowAll())
        }
        if (rule.allowedUserIds.isNotEmpty()) {
            val u = TLRPC.TL_privacyValueAllowUsers()
            u.users.addAll(rule.allowedUserIds)
            list.add(u)
        }
        if (rule.disallowedUserIds.isNotEmpty()) {
            val u = TLRPC.TL_privacyValueDisallowUsers()
            u.users.addAll(rule.disallowedUserIds)
            list.add(u)
        }
        return list
    }

    fun mapPasscodeSettings(): PasscodeSettingsModel {
        val hash = SharedConfig.passcodeHash ?: ""
        return PasscodeSettingsModel(
            isPasscodeSet = hash.isNotEmpty(),
            passcodeType = SharedConfig.passcodeType,
            isAppLocked = SharedConfig.appLocked,
            autoLockInSeconds = SharedConfig.autoLockIn,
            useFingerprint = SharedConfig.useFingerprintLock,
            allowScreenCapture = SharedConfig.allowScreenCapture
        )
    }

    fun mapTwoStepVerification(password: TL_account.Password?): TwoStepVerificationModel {
        if (password == null) {
            return TwoStepVerificationModel(hasPassword = false, hasRecoveryEmail = false, emailPattern = null)
        }
        return TwoStepVerificationModel(
            hasPassword = password.has_password,
            hasRecoveryEmail = password.has_recovery,
            emailPattern = password.login_email_pattern
        )
    }
}
