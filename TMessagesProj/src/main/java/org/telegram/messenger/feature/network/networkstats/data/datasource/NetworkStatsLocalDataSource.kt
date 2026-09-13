package org.telegram.messenger.feature.network.networkstats.data.datasource

import org.telegram.messenger.StatsController

/**
 * Local data source for reading and writing network usage statistics.
 * Bridges to StatsController with headless in-memory fallback.
 */
open class NetworkStatsLocalDataSource(
    private val currentAccount: Int
) {

    private val inMemorySentBytes = Array(3) { LongArray(8) }
    private val inMemoryReceivedBytes = Array(3) { LongArray(8) }
    private val inMemorySentItems = Array(3) { IntArray(8) }
    private val inMemoryReceivedItems = Array(3) { IntArray(8) }
    private val inMemoryCallsTotalTime = IntArray(3)
    private val inMemoryResetStatsDate = LongArray(3) { System.currentTimeMillis() }

    open fun getStatsController(): StatsController? {
        return try {
            StatsController.getInstance(currentAccount)
        } catch (_: Throwable) {
            null
        }
    }

    open fun getSentBytesCount(networkType: Int, dataType: Int): Long {
        val controller = getStatsController()
        if (controller != null) {
            return controller.getSentBytesCount(networkType, dataType)
        }
        if (networkType in 0..2 && dataType in 0..7) {
            if (dataType == StatsController.TYPE_MESSAGES) {
                return inMemorySentBytes[networkType][StatsController.TYPE_TOTAL] -
                    inMemorySentBytes[networkType][StatsController.TYPE_FILES] -
                    inMemorySentBytes[networkType][StatsController.TYPE_AUDIOS] -
                    inMemorySentBytes[networkType][StatsController.TYPE_VIDEOS] -
                    inMemorySentBytes[networkType][StatsController.TYPE_PHOTOS] -
                    inMemorySentBytes[networkType][StatsController.TYPE_MUSIC]
            }
            return inMemorySentBytes[networkType][dataType]
        }
        return 0L
    }

    open fun getReceivedBytesCount(networkType: Int, dataType: Int): Long {
        val controller = getStatsController()
        if (controller != null) {
            return controller.getReceivedBytesCount(networkType, dataType)
        }
        if (networkType in 0..2 && dataType in 0..7) {
            if (dataType == StatsController.TYPE_MESSAGES) {
                return inMemoryReceivedBytes[networkType][StatsController.TYPE_TOTAL] -
                    inMemoryReceivedBytes[networkType][StatsController.TYPE_FILES] -
                    inMemoryReceivedBytes[networkType][StatsController.TYPE_AUDIOS] -
                    inMemoryReceivedBytes[networkType][StatsController.TYPE_VIDEOS] -
                    inMemoryReceivedBytes[networkType][StatsController.TYPE_PHOTOS] -
                    inMemoryReceivedBytes[networkType][StatsController.TYPE_MUSIC]
            }
            return inMemoryReceivedBytes[networkType][dataType]
        }
        return 0L
    }

    open fun getSentItemsCount(networkType: Int, dataType: Int): Int {
        val controller = getStatsController()
        if (controller != null) {
            return controller.getSentItemsCount(networkType, dataType)
        }
        if (networkType in 0..2 && dataType in 0..7) {
            return inMemorySentItems[networkType][dataType]
        }
        return 0
    }

    open fun getReceivedItemsCount(networkType: Int, dataType: Int): Int {
        val controller = getStatsController()
        if (controller != null) {
            return controller.getRecivedItemsCount(networkType, dataType)
        }
        if (networkType in 0..2 && dataType in 0..7) {
            return inMemoryReceivedItems[networkType][dataType]
        }
        return 0
    }

    open fun getRecivedItemsCount(networkType: Int, dataType: Int): Int {
        return getReceivedItemsCount(networkType, dataType)
    }

    open fun getCallsTotalTime(networkType: Int): Int {
        val controller = getStatsController()
        if (controller != null) {
            return controller.getCallsTotalTime(networkType)
        }
        if (networkType in 0..2) {
            return inMemoryCallsTotalTime[networkType]
        }
        return 0
    }

    open fun getResetStatsDate(networkType: Int): Long {
        val controller = getStatsController()
        if (controller != null) {
            return controller.getResetStatsDate(networkType)
        }
        if (networkType in 0..2) {
            return inMemoryResetStatsDate[networkType]
        }
        return System.currentTimeMillis()
    }

    open fun incrementSentBytesCount(networkType: Int, dataType: Int, bytes: Long) {
        val controller = getStatsController()
        if (controller != null) {
            controller.incrementSentBytesCount(networkType, dataType, bytes)
        } else if (networkType in 0..2 && dataType in 0..7) {
            inMemorySentBytes[networkType][dataType] += bytes
            if (dataType != StatsController.TYPE_TOTAL) {
                inMemorySentBytes[networkType][StatsController.TYPE_TOTAL] += bytes
            }
        }
    }

    open fun incrementReceivedBytesCount(networkType: Int, dataType: Int, bytes: Long) {
        val controller = getStatsController()
        if (controller != null) {
            controller.incrementReceivedBytesCount(networkType, dataType, bytes)
        } else if (networkType in 0..2 && dataType in 0..7) {
            inMemoryReceivedBytes[networkType][dataType] += bytes
            if (dataType != StatsController.TYPE_TOTAL) {
                inMemoryReceivedBytes[networkType][StatsController.TYPE_TOTAL] += bytes
            }
        }
    }

    open fun incrementSentItemsCount(networkType: Int, dataType: Int, count: Int) {
        val controller = getStatsController()
        if (controller != null) {
            controller.incrementSentItemsCount(networkType, dataType, count)
        } else if (networkType in 0..2 && dataType in 0..7) {
            inMemorySentItems[networkType][dataType] += count
            if (dataType != StatsController.TYPE_TOTAL) {
                inMemorySentItems[networkType][StatsController.TYPE_TOTAL] += count
            }
        }
    }

    open fun incrementReceivedItemsCount(networkType: Int, dataType: Int, count: Int) {
        val controller = getStatsController()
        if (controller != null) {
            controller.incrementReceivedItemsCount(networkType, dataType, count)
        } else if (networkType in 0..2 && dataType in 0..7) {
            inMemoryReceivedItems[networkType][dataType] += count
            if (dataType != StatsController.TYPE_TOTAL) {
                inMemoryReceivedItems[networkType][StatsController.TYPE_TOTAL] += count
            }
        }
    }

    open fun incrementTotalCallsTime(networkType: Int, seconds: Int) {
        val controller = getStatsController()
        if (controller != null) {
            controller.incrementTotalCallsTime(networkType, seconds)
        } else if (networkType in 0..2) {
            inMemoryCallsTotalTime[networkType] += seconds
        }
    }

    open fun resetStats(networkType: Int) {
        val controller = getStatsController()
        if (controller != null) {
            controller.resetStats(networkType)
        } else if (networkType in 0..2) {
            inMemoryResetStatsDate[networkType] = System.currentTimeMillis()
            for (i in 0..7) {
                inMemorySentBytes[networkType][i] = 0L
                inMemoryReceivedBytes[networkType][i] = 0L
                inMemorySentItems[networkType][i] = 0
                inMemoryReceivedItems[networkType][i] = 0
            }
            inMemoryCallsTotalTime[networkType] = 0
        }
    }
}
