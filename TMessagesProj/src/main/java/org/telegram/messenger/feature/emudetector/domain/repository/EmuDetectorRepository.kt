package org.telegram.messenger.feature.emudetector.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.emudetector.domain.model.EmulatorDetectorConfig
import org.telegram.messenger.feature.emudetector.domain.model.EmulatorDiagnostics

/**
 * Контракт репозитория детектора эмуляторов и виртуальных сред.
 */
interface EmuDetectorRepository {

    /**
     * Выполняет полную диагностику среды.
     * @param forceRefresh если true, игнорирует кэшированный результат и запускает проверку заново.
     */
    suspend fun detectEnvironment(forceRefresh: Boolean = false): EmulatorDiagnostics

    /**
     * Возвращает кэшированный результат диагностики без повторного сканирования.
     */
    fun getCachedDiagnostics(): EmulatorDiagnostics?

    /**
     * Проверяет, запущен ли клиент в среде эмулятора (быстрый синхронный опрос кэша или базовой эвристики).
     */
    fun isEmulator(): Boolean

    /**
     * Возвращает текущую конфигурацию детектора.
     */
    fun getConfig(): EmulatorDetectorConfig

    /**
     * Обновляет конфигурацию детектора.
     */
    fun updateConfig(config: EmulatorDetectorConfig)

    /**
     * Добавляет имя пакета в список отслеживаемых companion-приложений эмуляторов.
     */
    fun addCustomPackage(packageName: String)

    /**
     * Сбрасывает кэш диагностики.
     */
    fun clearCache()

    /**
     * Реактивный поток текущего состояния диагностики.
     */
    fun observeDiagnostics(): StateFlow<EmulatorDiagnostics?>

    /**
     * Реактивный поток флага обнаружения эмулятора.
     */
    fun observeIsEmulator(): StateFlow<Boolean>
}
