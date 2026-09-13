package org.telegram.messenger.feature.network.push.data.datasource

import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.PushListenerController
import org.telegram.messenger.SharedConfig
import org.telegram.messenger.UserConfig
import org.telegram.messenger.feature.network.push.data.mapper.PushMapper
import org.telegram.messenger.feature.network.push.domain.model.PushStatusModel

/**
 * Local data source managing push tokens, provider state, and registration flags across accounts.
 */
open class PushLocalDataSource(
    private val currentAccount: Int
) {
    // In-memory test fallbacks
    private var testToken: String? = null
    private var testPushType: Int? = null
    private var testPushStatus: String? = null
    private var testHasServices: Boolean? = null
    private var testProviderTitle: String? = null
    private val testRegisteredAccounts = mutableMapOf<Int, Boolean>()

    open fun getPushProvider(): PushListenerController.IPushListenerServiceProvider? {
        return try {
            if (ApplicationLoader.applicationContext != null) {
                ApplicationLoader.getPushProvider()
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    open fun getPushToken(): String? {
        return testToken ?: try {
            SharedConfig.pushString
        } catch (_: Throwable) {
            null
        }
    }

    open fun setPushToken(token: String?) {
        testToken = token
    }

    open fun getPushType(): Int {
        return testPushType ?: try {
            SharedConfig.pushType
        } catch (_: Throwable) {
            PushListenerController.PUSH_TYPE_FIREBASE
        }
    }

    open fun setPushType(type: Int) {
        testPushType = type
    }

    open fun getPushStatusString(): String? {
        return testPushStatus ?: try {
            SharedConfig.pushStringStatus
        } catch (_: Throwable) {
            null
        }
    }

    open fun setPushStatusString(status: String?) {
        testPushStatus = status
    }

    open fun hasServices(): Boolean {
        testHasServices?.let { return it }
        return try {
            getPushProvider()?.hasServices() ?: false
        } catch (_: Throwable) {
            false
        }
    }

    open fun setHasServices(has: Boolean) {
        testHasServices = has
    }

    open fun getProviderTitle(): String {
        testProviderTitle?.let { return it }
        return try {
            getPushProvider()?.logTitle ?: "Google Play Services"
        } catch (_: Throwable) {
            "Google Play Services"
        }
    }

    open fun setProviderTitle(title: String) {
        testProviderTitle = title
    }

    open fun isRegisteredForAccount(acc: Int): Boolean {
        testRegisteredAccounts[acc]?.let { return it }
        return try {
            UserConfig.getInstance(acc).registeredForPush
        } catch (_: Throwable) {
            false
        }
    }

    open fun setRegisteredForAccount(acc: Int, registered: Boolean) {
        testRegisteredAccounts[acc] = registered
    }

    open fun getRegisteredAccountsCount(): Int {
        var count = 0
        for (i in 0 until 4) {
            if (isRegisteredForAccount(i)) {
                count++
            }
        }
        return count
    }

    open fun computeStatus(): PushStatusModel {
        return PushMapper.mapToPushStatus(
            token = getPushToken(),
            legacyPushType = getPushType(),
            status = getPushStatusString(),
            hasServices = hasServices(),
            isRegisteredForAccount = isRegisteredForAccount(currentAccount),
            registeredAccountsCount = getRegisteredAccountsCount(),
            providerTitle = getProviderTitle()
        )
    }
}
