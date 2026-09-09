package org.telegram.messenger.feature.timezones.data.mapper

import org.telegram.messenger.feature.timezones.domain.model.TimezoneModel
import org.telegram.tgnet.TLRPC

object TimezoneMapper {

    fun toDomain(tl: TLRPC.TL_timezone?): TimezoneModel? {
        if (tl == null) return null
        return TimezoneModel(
            id = tl.id ?: "",
            name = tl.name ?: "",
            utcOffsetSeconds = tl.utc_offset
        )
    }

    fun toDomainList(list: List<TLRPC.TL_timezone>?): List<TimezoneModel> {
        if (list == null) return emptyList()
        return list.mapNotNull { toDomain(it) }
    }

    fun toTl(model: TimezoneModel): TLRPC.TL_timezone {
        val tl = TLRPC.TL_timezone()
        tl.id = model.id
        tl.name = model.name
        tl.utc_offset = model.utcOffsetSeconds
        return tl
    }
}
