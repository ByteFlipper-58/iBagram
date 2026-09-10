package org.telegram.messenger.feature.security.flagsecure.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.security.flagsecure.domain.model.SecurityReasonType
import org.telegram.messenger.feature.security.flagsecure.domain.model.WindowSecurityState

/**
 * Контракт репозитория арбитража безопасности окон (FLAG_SECURE).
 */
interface FlagSecureRepository {

    /**
     * Добавляет причину включения защиты окна с условием активности.
     */
    fun attachReason(
        windowId: String,
        reason: SecurityReasonType,
        condition: (() -> Boolean)? = null
    ): WindowSecurityState

    /**
     * Снимает указанную причину защиты окна.
     */
    fun detachReason(
        windowId: String,
        reason: SecurityReasonType
    ): WindowSecurityState

    /**
     * Вызывает пересчет условий всех прикрепленных причин для окна.
     */
    fun invalidateWindow(windowId: String): WindowSecurityState

    /**
     * Проверяет, защищено ли окно в данный момент (быстрый синхронный запрос).
     */
    fun isWindowSecured(windowId: String): Boolean

    /**
     * Возвращает текущий снимок состояния безопасности окна.
     */
    fun getWindowState(windowId: String): WindowSecurityState

    /**
     * Возвращает карту состояний всех отслеживаемых окон.
     */
    fun getAllWindowStates(): Map<String, WindowSecurityState>

    /**
     * Сбрасывает состояние безопасности окна и удаляет все причины.
     */
    fun resetWindow(windowId: String)

    /**
     * Реактивный поток состояния конкретного окна.
     */
    fun observeWindowState(windowId: String): StateFlow<WindowSecurityState>

    /**
     * Реактивный поток карты всех активных окон.
     */
    fun observeAllWindowStates(): StateFlow<Map<String, WindowSecurityState>>
}
