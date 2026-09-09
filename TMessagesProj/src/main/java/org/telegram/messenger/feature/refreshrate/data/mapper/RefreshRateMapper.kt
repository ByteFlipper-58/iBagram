package org.telegram.messenger.feature.refreshrate.data.mapper

import android.os.Build
import android.view.Display
import org.telegram.messenger.feature.refreshrate.domain.model.DisplayRefreshModeModel

object RefreshRateMapper {

    fun mapMode(mode: Any?): DisplayRefreshModeModel? {
        if (mode == null) return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mode is Display.Mode) {
            return DisplayRefreshModeModel(
                modeId = mode.modeId,
                width = mode.physicalWidth,
                height = mode.physicalHeight,
                refreshRate = mode.refreshRate
            )
        }
        return null
    }

    fun mapModes(modes: Array<*>?): List<DisplayRefreshModeModel> {
        if (modes == null) return emptyList()
        val result = mutableListOf<DisplayRefreshModeModel>()
        for (m in modes) {
            val mapped = mapMode(m)
            if (mapped != null) {
                result.add(mapped)
            }
        }
        return result
    }
}
