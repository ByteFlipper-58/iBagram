package org.telegram.messenger.feature.security.passkeys.data.datasource

import android.os.Build
import org.telegram.messenger.BuildVars
import org.telegram.messenger.MessagesController

/**
 * Local data source for Passkey capabilities and device settings.
 */
open class PasskeysLocalDataSource(
    private val currentAccount: Int
) {
    /**
     * Checks if Passkeys are supported by current device Android version and configuration.
     */
    open fun isSupported(): Boolean {
        return Build.VERSION.SDK_INT >= 28 && BuildVars.SUPPORTS_PASSKEYS
    }

    /**
     * Returns the maximum allowed passkeys per account from server configuration.
     */
    open fun getMaxPasskeys(): Int {
        return try {
            MessagesController.getInstance(currentAccount).config.passkeysAccountPasskeysMax.get()
        } catch (_: Throwable) {
            5
        }
    }
}
