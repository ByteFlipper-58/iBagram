package org.telegram.messenger.feature.messaging.translate.data.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.telegram.messenger.LocaleController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.TranslateController
import org.telegram.ui.RestrictedLanguagesSelectActivity

/**
 * Local data source managing translation configuration, dialog translation states,
 * restricted languages, and language preferences.
 */
open class TranslationLocalDataSource(
    protected val currentAccount: Int,
    protected val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) {

    protected open fun getTranslateController(): TranslateController? {
        return try {
            MessagesController.getInstance(currentAccount)?.translateController
        } catch (_: Throwable) {
            null
        }
    }

    open fun isChatTranslateEnabled(): Boolean {
        return try {
            getTranslateController()?.isChatTranslateEnabled ?: false
        } catch (_: Throwable) {
            false
        }
    }

    open fun setChatTranslateEnabled(enabled: Boolean) {
        try {
            getTranslateController()?.isChatTranslateEnabled = enabled
        } catch (_: Throwable) {
        }
    }

    open fun isContextTranslateEnabled(): Boolean {
        return try {
            getTranslateController()?.isContextTranslateEnabled ?: false
        } catch (_: Throwable) {
            false
        }
    }

    open fun setContextTranslateEnabled(enabled: Boolean) {
        try {
            getTranslateController()?.isContextTranslateEnabled = enabled
        } catch (_: Throwable) {
        }
    }

    open fun getRestrictedLanguages(): Set<String> {
        return try {
            RestrictedLanguagesSelectActivity.getRestrictedLanguages() ?: emptySet()
        } catch (_: Throwable) {
            emptySet()
        }
    }

    open fun updateRestrictedLanguages(languages: Set<String>) {
        try {
            RestrictedLanguagesSelectActivity.updateRestrictedLanguages(HashSet(languages), true)
            getTranslateController()?.checkRestrictedLanguagesUpdate()
        } catch (_: Throwable) {
        }
    }

    open fun getCurrentLanguage(): String {
        return try {
            TranslateController.currentLanguage() ?: "en"
        } catch (_: Throwable) {
            "en"
        }
    }

    open fun isDialogTranslatable(dialogId: Long): Boolean {
        return try {
            getTranslateController()?.isDialogTranslatable(dialogId) ?: false
        } catch (_: Throwable) {
            false
        }
    }

    open fun isTranslatingDialog(dialogId: Long): Boolean {
        return try {
            getTranslateController()?.isTranslatingDialog(dialogId) ?: false
        } catch (_: Throwable) {
            false
        }
    }

    open fun getDialogTranslateTo(dialogId: Long): String? {
        return try {
            getTranslateController()?.getDialogTranslateTo(dialogId)
        } catch (_: Throwable) {
            null
        }
    }

    open fun setDialogTranslateTo(dialogId: Long, lang: String) {
        try {
            getTranslateController()?.setDialogTranslateTo(dialogId, lang)
        } catch (_: Throwable) {
        }
    }

    open fun toggleTranslatingDialog(dialogId: Long, enabled: Boolean) {
        try {
            getTranslateController()?.toggleTranslatingDialog(dialogId, enabled)
        } catch (_: Throwable) {
        }
    }

    open fun getAvailableLocaleInfos(): List<LocaleController.LocaleInfo> {
        return try {
            val localeController = LocaleController.getInstance() ?: return emptyList()
            val official = localeController.languages ?: emptyList()
            val unofficial = localeController.unofficialLanguages ?: emptyList()
            val remote = localeController.remoteLanguages ?: emptyList()
            official + unofficial + remote
        } catch (_: Throwable) {
            emptyList()
        }
    }

    open fun getCurrentLocaleShortName(): String? {
        return try {
            LocaleController.getInstance()?.currentLocaleInfo?.shortName
        } catch (_: Throwable) {
            null
        }
    }

    open suspend fun applyAppLanguage(languageCode: String): Boolean = withContext(mainDispatcher) {
        try {
            val localeController = LocaleController.getInstance() ?: return@withContext false
            val allInfos = (localeController.languages ?: emptyList<LocaleController.LocaleInfo>()) +
                (localeController.unofficialLanguages ?: emptyList()) +
                (localeController.remoteLanguages ?: emptyList())

            val target = allInfos.find { it.pluralLangCode == languageCode || it.shortName == languageCode }
                ?: return@withContext false

            localeController.applyLanguage(target, true, false, currentAccount)
            true
        } catch (_: Throwable) {
            false
        }
    }
}
