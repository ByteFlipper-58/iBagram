package org.telegram.messenger.feature.businesslinks.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.businesslinks.data.mapper.BusinessLinkMapper
import org.telegram.messenger.feature.businesslinks.domain.model.BusinessLinkInputModel
import org.telegram.messenger.feature.businesslinks.domain.model.BusinessLinkModel
import org.telegram.messenger.feature.businesslinks.domain.repository.BusinessLinksRepository
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account
import org.telegram.ui.Business.BusinessLinksController
import java.util.ArrayList
import kotlin.coroutines.resume

class LegacyBusinessLinksRepository(
    private val currentAccount: Int,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : BusinessLinksRepository {

    override fun observeBusinessLinks(): Flow<List<BusinessLinkModel>> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.businessLinksUpdated)
            .map { getBusinessLinks() }
            .onStart { emit(getBusinessLinks()) }
            .flowOn(mainDispatcher)
    }

    override suspend fun getBusinessLinks(): List<BusinessLinkModel> = withContext(mainDispatcher) {
        val controller = BusinessLinksController.getInstance(currentAccount)
        controller.links?.mapNotNull { BusinessLinkMapper.mapLink(it) } ?: emptyList()
    }

    override suspend fun loadBusinessLinks(forceReload: Boolean): Result<List<BusinessLinkModel>> = withContext(mainDispatcher) {
        try {
            val controller = BusinessLinksController.getInstance(currentAccount)
            controller.load(forceReload)
            Result.Success(getBusinessLinks())
        } catch (e: Throwable) {
            Result.failure(e.message ?: "Failed to load business links", e)
        }
    }

    override suspend fun createLink(input: BusinessLinkInputModel?): Result<BusinessLinkModel> = withContext(mainDispatcher) {
        suspendCancellableCoroutine { cont ->
            val req = TL_account.createBusinessChatLink()
            req.link = BusinessLinkMapper.toInputLink(input)

            val reqId = ConnectionsManager.getInstance(currentAccount).sendRequest(req) { res, err ->
                AndroidUtilities.runOnUIThread {
                    if (res is TL_account.TL_businessChatLink) {
                        val controller = BusinessLinksController.getInstance(currentAccount)
                        controller.links.add(res)
                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.businessLinksUpdated)
                        val model = BusinessLinkMapper.mapLink(res)
                        if (model != null) {
                            cont.resume(Result.Success(model))
                        } else {
                            cont.resume(Result.failure("Failed to map created link"))
                        }
                    } else {
                        cont.resume(Result.failure(err?.text ?: "Failed to create business link"))
                    }
                }
            }

            cont.invokeOnCancellation {
                ConnectionsManager.getInstance(currentAccount).cancelRequest(reqId, true)
            }
        }
    }

    override suspend fun editLink(
        slug: String,
        title: String?,
        message: String
    ): Result<BusinessLinkModel> = withContext(mainDispatcher) {
        suspendCancellableCoroutine { cont ->
            val controller = BusinessLinksController.getInstance(currentAccount)
            val existing = controller.findLink(slug)
            if (existing == null) {
                cont.resume(Result.failure("Business link $slug not found"))
                return@suspendCancellableCoroutine
            }

            val req = TL_account.editBusinessChatLink()
            req.slug = existing.link

            val inputLink = TL_account.TL_inputBusinessChatLink().apply {
                this.message = message
                this.entities = existing.entities ?: ArrayList()
                if (!title.isNullOrBlank()) {
                    this.title = title
                    this.flags = this.flags or 2
                }
                if (!entities.isNullOrEmpty()) {
                    this.flags = this.flags or 1
                }
            }
            req.link = inputLink

            val reqId = ConnectionsManager.getInstance(currentAccount).sendRequest(req) { res, err ->
                AndroidUtilities.runOnUIThread {
                    if (res is TL_account.TL_businessChatLink) {
                        val index = controller.links.indexOf(existing)
                        if (index != -1) {
                            controller.links[index] = res
                            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.businessLinksUpdated)
                        }
                        val model = BusinessLinkMapper.mapLink(res)
                        if (model != null) {
                            cont.resume(Result.Success(model))
                        } else {
                            cont.resume(Result.failure("Failed to map edited link"))
                        }
                    } else {
                        cont.resume(Result.failure(err?.text ?: "Failed to edit business link"))
                    }
                }
            }

            cont.invokeOnCancellation {
                ConnectionsManager.getInstance(currentAccount).cancelRequest(reqId, true)
            }
        }
    }

    override suspend fun deleteLink(slug: String): Result<Unit> = withContext(mainDispatcher) {
        suspendCancellableCoroutine { cont ->
            val controller = BusinessLinksController.getInstance(currentAccount)
            val existing = controller.findLink(slug)
            if (existing == null) {
                cont.resume(Result.failure("Business link $slug not found"))
                return@suspendCancellableCoroutine
            }

            val req = TL_account.deleteBusinessChatLink()
            req.slug = slug

            val reqId = ConnectionsManager.getInstance(currentAccount).sendRequest(req) { res, err ->
                AndroidUtilities.runOnUIThread {
                    if (res is TLRPC.TL_boolTrue) {
                        controller.links.remove(existing)
                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.businessLinksUpdated)
                        cont.resume(Result.Success(Unit))
                    } else {
                        cont.resume(Result.failure(err?.text ?: "Failed to delete business link"))
                    }
                }
            }

            cont.invokeOnCancellation {
                ConnectionsManager.getInstance(currentAccount).cancelRequest(reqId, true)
            }
        }
    }

    override fun findLink(slug: String): BusinessLinkModel? {
        val controller = BusinessLinksController.getInstance(currentAccount)
        return BusinessLinkMapper.mapLink(controller.findLink(slug))
    }

    override fun canAddNew(): Boolean {
        val controller = BusinessLinksController.getInstance(currentAccount)
        return controller.canAddNew()
    }

    override fun getLinksLimit(): Int {
        return MessagesController.getInstance(currentAccount).businessChatLinksLimit
    }
}
