package org.telegram.messenger.feature.pip.data.mapper

import org.telegram.messenger.feature.pip.domain.model.PipSourceModel
import org.telegram.messenger.pip.PipSource

object PipMapper {

    fun toDomain(source: PipSource): PipSourceModel {
        return PipSourceModel(
            tag = source.tag ?: "",
            priority = source.priority,
            isAvailable = source.isAvailable,
            isAttachedToPip = source.state2?.isAttachedToPip ?: false,
            needsMediaSession = source.needMediaSession,
            aspectRatioWidth = if (source.params != null) source.params.width else 16,
            aspectRatioHeight = if (source.params != null) source.params.height else 9,
            title = source.tag
        )
    }
}
