package org.telegram.messenger.feature.media.contentpreview.data.mapper

import org.telegram.messenger.feature.media.contentpreview.domain.model.PreviewContentType

/**
 * Маппер типов контента и жестов для ContentPreviewViewer.
 */
object ContentPreviewMapper {

    fun mapContentTypeId(id: Int): PreviewContentType {
        return PreviewContentType.fromId(id)
    }

    fun mapContentTypeToId(type: PreviewContentType): Int {
        return type.id
    }

    fun calculateNormalizedDragProgress(
        startY: Float,
        currentY: Float,
        maxDistance: Float
    ): Float {
        if (maxDistance <= 0f) return 0f
        val deltaY = (startY - currentY).coerceAtLeast(0f)
        return (deltaY / maxDistance).coerceIn(0f, 1f)
    }

    fun shouldTriggerHaptic(previousProgress: Float, currentProgress: Float, threshold: Float = 0.75f): Boolean {
        return previousProgress < threshold && currentProgress >= threshold
    }
}
