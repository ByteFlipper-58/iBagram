package org.telegram.messenger.feature.businesslinks.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.businesslinks.domain.model.BusinessLinkInputModel
import org.telegram.messenger.feature.businesslinks.domain.model.BusinessLinkModel

interface BusinessLinksRepository {
    fun observeBusinessLinks(): Flow<List<BusinessLinkModel>>
    suspend fun getBusinessLinks(): List<BusinessLinkModel>
    suspend fun loadBusinessLinks(forceReload: Boolean = false): Result<List<BusinessLinkModel>>
    suspend fun createLink(input: BusinessLinkInputModel? = null): Result<BusinessLinkModel>
    suspend fun editLink(slug: String, title: String?, message: String): Result<BusinessLinkModel>
    suspend fun deleteLink(slug: String): Result<Unit>
    fun findLink(slug: String): BusinessLinkModel?
    fun canAddNew(): Boolean
    fun getLinksLimit(): Int
}
