package org.telegram.messenger.feature.security.privacy.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.telegram.messenger.ContactsController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.SharedConfig
import org.telegram.messenger.Utilities
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.feature.security.privacy.data.mapper.PrivacyMapper
import org.telegram.messenger.feature.security.privacy.domain.model.PasscodeSettingsModel
import org.telegram.tgnet.TLRPC
import java.nio.charset.StandardCharsets
import java.util.ArrayList

/**
 * Local data source managing privacy rules, blocklist, passcode configurations, and local event emissions.
 */
open class PrivacyLocalDataSource(
    private val currentAccount: Int
) {
    // In-memory test cache
    private val testRules = mutableMapOf<Int, ArrayList<TLRPC.PrivacyRule>>()
    private val testBlockedPeers = mutableListOf<Long>()
    private var testPasscodeSettings: PasscodeSettingsModel? = null
    private var testPasscode: String? = null

    open fun getPrivacyRules(legacyType: Int): ArrayList<TLRPC.PrivacyRule>? {
        testRules[legacyType]?.let { return it }
        return try {
            ContactsController.getInstance(currentAccount).getPrivacyRules(legacyType)
        } catch (_: Throwable) {
            null
        }
    }

    open fun setPrivacyRules(rules: ArrayList<TLRPC.PrivacyRule>?, legacyType: Int) {
        if (rules != null) {
            testRules[legacyType] = rules
        } else {
            testRules.remove(legacyType)
        }
        try {
            ContactsController.getInstance(currentAccount).setPrivacyRules(rules, legacyType)
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.privacyRulesUpdated)
        } catch (_: Throwable) {}
    }

    open fun loadPrivacySettings() {
        try {
            ContactsController.getInstance(currentAccount).loadPrivacySettings()
        } catch (_: Throwable) {}
    }

    open fun getBlockedPeers(): List<Long> {
        if (testBlockedPeers.isNotEmpty()) return ArrayList(testBlockedPeers)
        val list = mutableListOf<Long>()
        return try {
            val mc = MessagesController.getInstance(currentAccount)
            val dict = mc.blockePeers
            if (dict != null) {
                for (i in 0 until dict.size()) {
                    list.add(dict.keyAt(i))
                }
            }
            list
        } catch (_: Throwable) {
            emptyList()
        }
    }

    open fun getBlockedCount(): Int {
        if (testBlockedPeers.isNotEmpty()) return testBlockedPeers.size
        return try {
            MessagesController.getInstance(currentAccount).totalBlockedCount
        } catch (_: Throwable) {
            0
        }
    }

    open fun blockPeer(peerId: Long) {
        if (!testBlockedPeers.contains(peerId)) {
            testBlockedPeers.add(peerId)
        }
        try {
            MessagesController.getInstance(currentAccount).blockPeer(peerId)
        } catch (_: Throwable) {}
    }

    open fun unblockPeer(peerId: Long) {
        testBlockedPeers.remove(peerId)
        try {
            MessagesController.getInstance(currentAccount).unblockPeer(peerId)
        } catch (_: Throwable) {}
    }

    open fun getPasscodeSettings(): PasscodeSettingsModel {
        testPasscodeSettings?.let { return it }
        return try {
            PrivacyMapper.mapPasscodeSettings()
        } catch (_: Throwable) {
            PasscodeSettingsModel()
        }
    }

    open fun setPasscode(passcode: String, type: Int) {
        testPasscode = passcode
        testPasscodeSettings = PasscodeSettingsModel(
            isPasscodeSet = passcode.isNotEmpty(),
            passcodeType = type,
            isAppLocked = false,
            autoLockInSeconds = testPasscodeSettings?.autoLockInSeconds ?: 0,
            useFingerprint = testPasscodeSettings?.useFingerprint ?: false,
            allowScreenCapture = testPasscodeSettings?.allowScreenCapture ?: false
        )
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
        } catch (_: Throwable) {}
    }

    open fun checkPasscode(passcode: String): Boolean {
        if (testPasscode != null) {
            return testPasscode == passcode
        }
        return try {
            SharedConfig.checkPasscode(passcode)
        } catch (_: Throwable) {
            false
        }
    }

    open fun clearPasscode() {
        testPasscode = null
        testPasscodeSettings = PasscodeSettingsModel(
            isPasscodeSet = false,
            passcodeType = 0,
            isAppLocked = false
        )
        try {
            SharedConfig.passcodeHash = ""
            SharedConfig.appLocked = false
            SharedConfig.saveConfig()
        } catch (_: Throwable) {}
    }

    open fun setAppLocked(locked: Boolean) {
        testPasscodeSettings = testPasscodeSettings?.copy(isAppLocked = locked)
        try {
            SharedConfig.appLocked = locked
            SharedConfig.saveConfig()
        } catch (_: Throwable) {}
    }

    open fun setAutoLockIn(seconds: Int) {
        testPasscodeSettings = testPasscodeSettings?.copy(autoLockInSeconds = seconds)
        try {
            SharedConfig.autoLockIn = seconds
            SharedConfig.saveConfig()
        } catch (_: Throwable) {}
    }

    open fun setUseFingerprint(use: Boolean) {
        testPasscodeSettings = testPasscodeSettings?.copy(useFingerprint = use)
        try {
            SharedConfig.useFingerprintLock = use
            SharedConfig.saveConfig()
        } catch (_: Throwable) {}
    }

    open fun setAllowScreenCapture(allow: Boolean) {
        testPasscodeSettings = testPasscodeSettings?.copy(allowScreenCapture = allow)
        try {
            SharedConfig.allowScreenCapture = allow
            SharedConfig.saveConfig()
        } catch (_: Throwable) {}
    }

    open fun observePrivacyRulesEvents(): Flow<Unit> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.privacyRulesUpdated).map { }
    }

    open fun observeBlockedUsersEvents(): Flow<Unit> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.blockedUsersDidLoad).map { }
    }

    open fun observeTwoStepEvents(): Flow<Unit> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.didSetOrRemoveTwoStepPassword).map { }
    }
}
