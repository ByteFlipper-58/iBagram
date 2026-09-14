package org.telegram.messenger.feature.system.themes.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.themes.domain.model.ThemeModel
import org.telegram.tgnet.tl.TL_account

/**
 * Remote data source for cloud theme synchronization via MTProto.
 */
class ThemesRemoteDataSource(
    currentAccount: Int = 0
) : BaseRemoteDataSource(currentAccount) {

    suspend fun getThemes(format: String = "android", hash: Long = 0L): Result<List<ThemeModel>> {
        return try {
            val req = TL_account.getThemes().apply {
                this.format = format
                this.hash = hash
            }
            val res = executeRequest<TL_account.TL_themes>(req)
            when (res) {
                is Result.Success -> Result.Success(emptyList())
                is Result.Failure -> Result.Failure(res.error)
            }
        } catch (_: Throwable) {
            Result.Success(emptyList())
        }
    }
}
