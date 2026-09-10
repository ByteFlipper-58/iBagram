package org.telegram.messenger.feature.messaging.stickers.data.mapper

import org.telegram.messenger.MessageObject
import org.telegram.messenger.feature.messaging.stickers.domain.model.StickerModel
import org.telegram.messenger.feature.messaging.stickers.domain.model.StickerSetModel
import org.telegram.messenger.feature.messaging.stickers.domain.model.StickerType
import org.telegram.tgnet.TLRPC

/**
 * Pure mapper converting legacy Telegram TLRPC sticker and sticker set structures
 * to clean domain entities [StickerModel] and [StickerSetModel].
 */
object StickerMapper {

    fun toDomain(document: TLRPC.Document?): StickerModel {
        if (document == null) {
            return StickerModel(id = 0L)
        }

        var emoji = ""
        var setId = 0L
        var width = 0
        var height = 0
        var isCustomEmoji = false

        if (document.attributes != null) {
            for (attr in document.attributes) {
                if (attr is TLRPC.TL_documentAttributeSticker) {
                    emoji = attr.alt ?: ""
                    if (attr.stickerset is TLRPC.TL_inputStickerSetID) {
                        setId = (attr.stickerset as TLRPC.TL_inputStickerSetID).id
                    }
                } else if (attr is TLRPC.TL_documentAttributeImageSize) {
                    width = attr.w
                    height = attr.h
                } else if (attr is TLRPC.TL_documentAttributeCustomEmoji) {
                    isCustomEmoji = true
                    if (attr.stickerset is TLRPC.TL_inputStickerSetID) {
                        setId = (attr.stickerset as TLRPC.TL_inputStickerSetID).id
                    }
                }
            }
        }

        val isAnimated = document.mime_type == "application/x-tgsticker" || MessageObject.isAnimatedStickerDocument(document)
        val isVideo = document.mime_type == "video/webm" || MessageObject.isVideoStickerDocument(document)

        val type = when {
            isCustomEmoji -> StickerType.EMOJI
            isVideo -> StickerType.VIDEO
            isAnimated -> StickerType.ANIMATED
            else -> StickerType.IMAGE
        }

        return StickerModel(
            id = document.id,
            accessHash = document.access_hash,
            setId = setId,
            mimeType = document.mime_type ?: "",
            emoji = emoji,
            type = type,
            width = width,
            height = height,
            size = document.size
        )
    }

    fun toDomainList(documents: List<TLRPC.Document>?): List<StickerModel> {
        return documents?.map { toDomain(it) } ?: emptyList()
    }

    fun toDomain(stickerSet: TLRPC.TL_messages_stickerSet?): StickerSetModel {
        if (stickerSet == null || stickerSet.set == null) {
            return StickerSetModel(id = 0L, title = "", shortName = "")
        }

        val set = stickerSet.set
        val stickers = toDomainList(stickerSet.documents)
        val isAnimated = stickers.any { it.type == StickerType.ANIMATED }
        val isVideo = stickers.any { it.type == StickerType.VIDEO }

        return StickerSetModel(
            id = set.id,
            accessHash = set.access_hash,
            title = set.title ?: "",
            shortName = set.short_name ?: "",
            count = set.count,
            isInstalled = set.installed,
            isArchived = set.archived,
            isOfficial = set.official,
            isAnimated = isAnimated,
            isVideo = isVideo,
            isEmoji = set.emojis,
            stickers = stickers
        )
    }

    fun toSetDomainList(sets: List<TLRPC.TL_messages_stickerSet>?): List<StickerSetModel> {
        return sets?.map { toDomain(it) } ?: emptyList()
    }
}
