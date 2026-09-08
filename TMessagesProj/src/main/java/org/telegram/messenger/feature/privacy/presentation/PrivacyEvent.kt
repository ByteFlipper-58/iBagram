package org.telegram.messenger.feature.privacy.presentation

import org.telegram.messenger.feature.privacy.domain.model.PrivacyRuleModel
import org.telegram.messenger.feature.privacy.domain.model.PrivacyRuleType

sealed class PrivacyEvent {
    data class SetRule(val type: PrivacyRuleType, val rule: PrivacyRuleModel) : PrivacyEvent()
    object ReloadRules : PrivacyEvent()

    data class BlockPeer(val peerId: Long) : PrivacyEvent()
    data class UnblockPeer(val peerId: Long) : PrivacyEvent()

    data class SetPasscode(val passcode: String, val type: Int) : PrivacyEvent()
    object ClearPasscode : PrivacyEvent()
    data class ToggleFingerprint(val enabled: Boolean) : PrivacyEvent()
    data class ToggleScreenCapture(val allow: Boolean) : PrivacyEvent()
    data class SetAutoLock(val seconds: Int) : PrivacyEvent()

    object ReloadTwoStepVerification : PrivacyEvent()
    object DismissError : PrivacyEvent()
}
