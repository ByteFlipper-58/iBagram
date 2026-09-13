package org.telegram.messenger.feature.security.captcha.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.security.captcha.domain.model.CaptchaAction
import org.telegram.messenger.feature.security.captcha.domain.model.CaptchaRequestModel

/**
 * Local data source managing in-memory active reCAPTCHA requests.
 */
class CaptchaLocalDataSource {

    private val _activeRequests = MutableStateFlow<List<CaptchaRequestModel>>(emptyList())

    fun observeActiveRequests(): Flow<List<CaptchaRequestModel>> = _activeRequests.asStateFlow()

    fun getActiveRequests(): List<CaptchaRequestModel> = _activeRequests.value

    fun addOrUpdateRequest(
        currentAccount: Int,
        requestToken: Int,
        action: CaptchaAction,
        keyId: String
    ): CaptchaRequestModel {
        var result: CaptchaRequestModel? = null
        _activeRequests.update { list ->
            val existingIndex = list.indexOfFirst {
                it.currentAccount == currentAccount &&
                    it.action.rawAction.equals(action.rawAction, ignoreCase = true) &&
                    it.keyId == keyId
            }
            if (existingIndex >= 0) {
                list.mapIndexed { idx, req ->
                    if (idx == existingIndex) {
                        val updated = req.withAdditionalToken(requestToken)
                        result = updated
                        updated
                    } else req
                }
            } else {
                val newRequest = CaptchaRequestModel(
                    currentAccount = currentAccount,
                    action = action,
                    keyId = keyId,
                    requestTokens = setOf(requestToken)
                )
                result = newRequest
                list + newRequest
            }
        }
        return result ?: CaptchaRequestModel(currentAccount, action, keyId, setOf(requestToken))
    }

    fun removeRequests(currentAccount: Int, requestTokens: List<Int>) {
        _activeRequests.update { list ->
            list.filterNot {
                it.currentAccount == currentAccount &&
                    it.requestTokens.any { reqToken -> reqToken in requestTokens }
            }
        }
    }

    fun cancelRequest(currentAccount: Int, action: String, keyId: String) {
        _activeRequests.update { list ->
            list.filterNot {
                it.currentAccount == currentAccount &&
                    it.action.rawAction.equals(action, ignoreCase = true) &&
                    it.keyId == keyId
            }
        }
    }

    fun clearAll() {
        _activeRequests.value = emptyList()
    }
}
