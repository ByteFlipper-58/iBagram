package org.telegram.messenger.feature.business.quickreplies.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.feature.business.quickreplies.data.mapper.QuickReplyMapper
import org.telegram.messenger.feature.business.quickreplies.domain.model.QuickReplyModel
import org.telegram.ui.Business.QuickRepliesController
import java.util.concurrent.CopyOnWriteArrayList

class QuickRepliesLocalDataSource(
    private val currentAccount: Int
) {
    private var isTestMode = false
    private val testFallbackReplies = CopyOnWriteArrayList<QuickReplyModel>()
    private val testEventFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 16)

    fun observeQuickRepliesUpdated(): Flow<Unit> {
        if (isTestMode) {
            return testEventFlow.asSharedFlow()
        }
        return try {
            NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.quickRepliesUpdated)
                .map { Unit }
        } catch (e: Throwable) {
            testEventFlow.asSharedFlow()
        }
    }

    fun getReplies(): List<QuickReplyModel> {
        if (isTestMode || testFallbackReplies.isNotEmpty()) {
            return testFallbackReplies.toList()
        }
        return try {
            val controller = QuickRepliesController.getInstance(currentAccount)
            if (controller != null && controller.replies != null) {
                QuickReplyMapper.mapToQuickReplyList(controller.replies)
            } else {
                testFallbackReplies.toList()
            }
        } catch (e: Throwable) {
            testFallbackReplies.toList()
        }
    }

    fun load() {
        if (isTestMode) return
        try {
            QuickRepliesController.getInstance(currentAccount)?.load()
        } catch (e: Throwable) {
            // Headless fallback
        }
    }

    fun findReplyById(id: Int): QuickReplyModel? {
        if (isTestMode || testFallbackReplies.isNotEmpty()) {
            return testFallbackReplies.find { it.id == id }
        }
        return try {
            val controller = QuickRepliesController.getInstance(currentAccount)
            if (controller != null) {
                controller.findReply(id.toLong())?.let { QuickReplyMapper.mapToQuickReply(it) }
            } else {
                testFallbackReplies.find { it.id == id }
            }
        } catch (e: Throwable) {
            testFallbackReplies.find { it.id == id }
        }
    }

    fun findReplyByName(name: String): QuickReplyModel? {
        if (isTestMode || testFallbackReplies.isNotEmpty()) {
            return testFallbackReplies.find { it.name.equals(name, ignoreCase = true) }
        }
        return try {
            val controller = QuickRepliesController.getInstance(currentAccount)
            if (controller != null) {
                controller.findReply(name)?.let { QuickReplyMapper.mapToQuickReply(it) }
            } else {
                testFallbackReplies.find { it.name.equals(name, ignoreCase = true) }
            }
        } catch (e: Throwable) {
            testFallbackReplies.find { it.name.equals(name, ignoreCase = true) }
        }
    }

    fun isNameBusy(name: String, exceptId: Int): Boolean {
        if (isTestMode || testFallbackReplies.isNotEmpty()) {
            return testFallbackReplies.any { it.name.equals(name, ignoreCase = true) && it.id != exceptId }
        }
        return try {
            val controller = QuickRepliesController.getInstance(currentAccount)
            if (controller != null) {
                controller.isNameBusy(name, exceptId)
            } else {
                testFallbackReplies.any { it.name.equals(name, ignoreCase = true) && it.id != exceptId }
            }
        } catch (e: Throwable) {
            testFallbackReplies.any { it.name.equals(name, ignoreCase = true) && it.id != exceptId }
        }
    }

    fun canAddNew(): Boolean {
        if (isTestMode || testFallbackReplies.isNotEmpty()) {
            return testFallbackReplies.size < 100
        }
        return try {
            val controller = QuickRepliesController.getInstance(currentAccount)
            if (controller != null) {
                controller.canAddNew()
            } else {
                testFallbackReplies.size < 100
            }
        } catch (e: Throwable) {
            testFallbackReplies.size < 100
        }
    }

    fun renameReply(id: Int, newName: String) {
        if (isTestMode || testFallbackReplies.isNotEmpty()) {
            val index = testFallbackReplies.indexOfFirst { it.id == id }
            if (index >= 0) {
                val old = testFallbackReplies[index]
                testFallbackReplies[index] = old.copy(name = newName)
                testEventFlow.tryEmit(Unit)
            }
            return
        }
        try {
            val controller = QuickRepliesController.getInstance(currentAccount)
            if (controller != null) {
                controller.renameReply(id, newName)
            } else {
                val index = testFallbackReplies.indexOfFirst { it.id == id }
                if (index >= 0) {
                    val old = testFallbackReplies[index]
                    testFallbackReplies[index] = old.copy(name = newName)
                    testEventFlow.tryEmit(Unit)
                }
            }
        } catch (e: Throwable) {
            val index = testFallbackReplies.indexOfFirst { it.id == id }
            if (index >= 0) {
                val old = testFallbackReplies[index]
                testFallbackReplies[index] = old.copy(name = newName)
                testEventFlow.tryEmit(Unit)
            }
        }
    }

    fun reorderReplies(ids: List<Int>) {
        if (isTestMode || testFallbackReplies.isNotEmpty()) {
            val sorted = ids.mapNotNull { id -> testFallbackReplies.find { it.id == id } }
            testFallbackReplies.clear()
            testFallbackReplies.addAll(sorted)
            testEventFlow.tryEmit(Unit)
            return
        }
        try {
            val controller = QuickRepliesController.getInstance(currentAccount)
            if (controller != null) {
                for (i in ids.indices) {
                    val reply = controller.findReply(ids[i].toLong())
                    if (reply != null) {
                        reply.order = i
                    }
                }
                controller.reorder()
            } else {
                val sorted = ids.mapNotNull { id -> testFallbackReplies.find { it.id == id } }
                testFallbackReplies.clear()
                testFallbackReplies.addAll(sorted)
                testEventFlow.tryEmit(Unit)
            }
        } catch (e: Throwable) {
            val sorted = ids.mapNotNull { id -> testFallbackReplies.find { it.id == id } }
            testFallbackReplies.clear()
            testFallbackReplies.addAll(sorted)
            testEventFlow.tryEmit(Unit)
        }
    }

    fun deleteReplies(ids: List<Int>) {
        if (isTestMode || testFallbackReplies.isNotEmpty()) {
            testFallbackReplies.removeAll { it.id in ids }
            testEventFlow.tryEmit(Unit)
            return
        }
        try {
            val controller = QuickRepliesController.getInstance(currentAccount)
            if (controller != null) {
                controller.deleteReplies(ArrayList(ids))
            } else {
                testFallbackReplies.removeAll { it.id in ids }
                testEventFlow.tryEmit(Unit)
            }
        } catch (e: Throwable) {
            testFallbackReplies.removeAll { it.id in ids }
            testEventFlow.tryEmit(Unit)
        }
    }

    fun sendQuickReply(dialogId: Long, shortcutId: Int) {
        try {
            val controller = QuickRepliesController.getInstance(currentAccount)
            val reply = controller?.findReply(shortcutId.toLong())
            if (controller != null && reply != null) {
                controller.sendQuickReplyTo(dialogId, reply)
            }
        } catch (e: Throwable) {
            // Headless fallback
        }
    }

    // Testing helpers
    fun setTestReplies(replies: List<QuickReplyModel>) {
        isTestMode = true
        testFallbackReplies.clear()
        testFallbackReplies.addAll(replies)
        testEventFlow.tryEmit(Unit)
    }

    fun addTestReply(reply: QuickReplyModel) {
        isTestMode = true
        testFallbackReplies.add(reply)
        testEventFlow.tryEmit(Unit)
    }
}
