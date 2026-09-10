package org.telegram.messenger.feature.security.privacy.domain.model

data class PasscodeSettingsModel(
    val isPasscodeSet: Boolean = false,
    val passcodeType: Int = 0, // 0 = 4 digits PIN, 1 = Password
    val isAppLocked: Boolean = false,
    val autoLockInSeconds: Int = 0,
    val useFingerprint: Boolean = false,
    val allowScreenCapture: Boolean = false
)
