package org.telegram.messenger.feature.business.businesslinks.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.businesslinks.data.datasource.BusinessLinksLocalDataSource
import org.telegram.messenger.feature.business.businesslinks.data.datasource.BusinessLinksRemoteDataSource
import org.telegram.messenger.feature.business.businesslinks.data.mapper.BusinessLinkMapper
import org.telegram.messenger.feature.business.businesslinks.domain.model.BusinessLinkInputModel
import org.telegram.messenger.feature.business.businesslinks.domain.model.BusinessLinkModel
import org.telegram.messenger.feature.business.businesslinks.domain.repository.BusinessLinksRepository
import org.telegram.tgnet.tl.TL_account
import java.util.ArrayList

/**
 * Modern repository implementation managing business chat links.
 */
class BusinessLinksRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: BusinessLinksLocalDataSource,
    private val remoteDataSource: BusinessLinksRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : BusinessLinksRepository {

    override fun observeBusinessLinks(): Flow<List<BusinessLinkModel>> {
        return localDataSource.observeBusinessLinksUpdated()
            .map { getBusinessLinks() }
            .onStart { emit(getBusinessLinks()) }
            .flowOn(mainDispatcher)
    }

    override suspend fun getBusinessLinks(): List<BusinessLinkModel> = withContext(mainDispatcher) {
        val raw = localDataSource.getLinks()
        raw.mapNotNull { BusinessLinkMapper.mapLink(it) }
    }

    override suspend fun loadBusinessLinks(forceReload: Boolean): Result<List<BusinessLinkModel>> = withContext(mainDispatcher) {
        localDataSource.load(forceReload)
        Result.success(getBusinessLinks())
    }

    override suspend fun createLink(input: BusinessLinkInputModel?): Result<BusinessLinkModel> = withContext(mainDispatcher) {
        val inputLink = BusinessLinkMapper.toInputLink(input)
        val result = remoteDataSource.createLink(inputLink)
        result.map { created ->
            localDataSource.addLink(created)
            BusinessLinkMapper.mapLink(created) ?: BusinessLinkModel(
                link = created.link ?: "",
                slug = BusinessLinkMapper.extractSlug(created.link ?: ""),
                title = created.title,
                message = created.message ?: "",
                views = created.views,
                hasEntities = !created.entities.isNullOrEmpty()
            )
        }
    }

    override suspend fun editLink(
        slug: String,
        title: String?,
        message: String
    ): Result<BusinessLinkModel> = withContext(mainDispatcher) {
        val existing = localDataSource.findLink(slug)
        val inputLink = TL_account.TL_inputBusinessChatLink().apply {
            this.message = message
            this.entities = existing?.entities ?: ArrayList()
            if (!title.isNullOrBlank()) {
                this.title = title
                this.flags = this.flags or 2
            }
            if (!this.entities.isNullOrEmpty()) {
                this.flags = this.flags or 1
            }
        }
        val result = remoteDataSource.editLink(slug, inputLink)
        result.map { edited ->
            localDataSource.updateLink(edited)
            BusinessLinkMapper.mapLink(edited) ?: BusinessLinkModel(
                link = edited.link ?: "",
                slug = BusinessLinkMapper.extractSlug(edited.link ?: ""),
                title = edited.title,
                message = edited.message ?: "",
                views = edited.views,
                hasEntities = !edited.entities.isNullOrEmpty()
            )
        }
    }

    override suspend fun deleteLink(slug: String): Result<Unit> = withContext(mainDispatcher) {
        val result = remoteDataSource.deleteLink(slug)
        result.map {
            localDataSource.removeLink(slug)
        }
    }

    override fun findLink(slug: String): BusinessLinkModel? {
        val found = localDataSource.findLink(slug)
        return BusinessLinkMapper.mapLink(found)
    }

    override fun canAddNew(): Boolean {
        return localDataSource.canAddNew()
    }

    override fun getLinksLimit(): Int {
        return localDataSource.getLinksLimit()
    }
}
