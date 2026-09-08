package org.telegram.messenger.feature.factcheck.data.mapper

import org.telegram.messenger.feature.factcheck.domain.model.FactCheckEntityModel
import org.telegram.messenger.feature.factcheck.domain.model.FactCheckModel
import org.telegram.tgnet.TLRPC

object FactCheckMapper {

    fun toDomain(tl: TLRPC.TL_factCheck, dialogId: Long, messageId: Int): FactCheckModel {
        val domainEntities = tl.text?.entities?.mapNotNull { toDomainEntity(it) } ?: emptyList()
        return FactCheckModel(
            hash = tl.hash,
            dialogId = dialogId,
            messageId = messageId,
            text = tl.text?.text.orEmpty(),
            entities = domainEntities,
            country = tl.country,
            needCheck = tl.need_check
        )
    }

    fun toDomainEntity(entity: TLRPC.MessageEntity?): FactCheckEntityModel? {
        if (entity == null) return null
        val type = when (entity) {
            is TLRPC.TL_messageEntityBold -> "bold"
            is TLRPC.TL_messageEntityItalic -> "italic"
            is TLRPC.TL_messageEntityTextUrl -> "text_url"
            is TLRPC.TL_messageEntityUrl -> "url"
            is TLRPC.TL_messageEntityCode -> "code"
            is TLRPC.TL_messageEntityPre -> "pre"
            is TLRPC.TL_messageEntityStrike -> "strike"
            is TLRPC.TL_messageEntityUnderline -> "underline"
            is TLRPC.TL_messageEntitySpoiler -> "spoiler"
            else -> entity.javaClass.simpleName.removePrefix("TL_messageEntity").lowercase()
        }
        val url = (entity as? TLRPC.TL_messageEntityTextUrl)?.url
        return FactCheckEntityModel(
            offset = entity.offset,
            length = entity.length,
            type = type,
            url = url
        )
    }

    fun toTlTextWithEntities(text: String, entities: List<FactCheckEntityModel>?): TLRPC.TL_textWithEntities {
        val result = TLRPC.TL_textWithEntities()
        result.text = text
        result.entities = entities?.mapNotNull { toTlEntity(it) }?.let { ArrayList(it) } ?: ArrayList()
        return result
    }

    fun toTlEntity(entity: FactCheckEntityModel): TLRPC.MessageEntity? {
        val tlEntity = when (entity.type.lowercase()) {
            "bold" -> TLRPC.TL_messageEntityBold()
            "italic" -> TLRPC.TL_messageEntityItalic()
            "text_url" -> TLRPC.TL_messageEntityTextUrl().apply { this.url = entity.url }
            "url" -> if (entity.url != null) {
                TLRPC.TL_messageEntityTextUrl().apply { this.url = entity.url }
            } else {
                TLRPC.TL_messageEntityUrl()
            }
            "code" -> TLRPC.TL_messageEntityCode()
            "pre" -> TLRPC.TL_messageEntityPre()
            "strike" -> TLRPC.TL_messageEntityStrike()
            "underline" -> TLRPC.TL_messageEntityUnderline()
            "spoiler" -> TLRPC.TL_messageEntitySpoiler()
            else -> return null
        }
        tlEntity.offset = entity.offset
        tlEntity.length = entity.length
        return tlEntity
    }
}
