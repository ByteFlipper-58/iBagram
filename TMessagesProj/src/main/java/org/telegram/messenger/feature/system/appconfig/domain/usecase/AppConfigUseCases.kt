package org.telegram.messenger.feature.system.appconfig.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.appconfig.domain.model.AiComposeConfigModel
import org.telegram.messenger.feature.system.appconfig.domain.model.AppGlobalConfigState
import org.telegram.messenger.feature.system.appconfig.domain.model.AppLimitsConfigModel
import org.telegram.messenger.feature.system.appconfig.domain.model.PollsConfigModel
import org.telegram.messenger.feature.system.appconfig.domain.model.RichMessageConfigModel
import org.telegram.messenger.feature.system.appconfig.domain.model.StarsConfigModel
import org.telegram.messenger.feature.system.appconfig.domain.model.TonConfigModel
import org.telegram.messenger.feature.system.appconfig.domain.repository.AppConfigRepository

class GetAppConfigUseCase(
    private val repository: AppConfigRepository
) {
    operator fun invoke(): AppGlobalConfigState = repository.getConfig()
}

class ObserveAppConfigUseCase(
    private val repository: AppConfigRepository
) {
    operator fun invoke(): Flow<AppGlobalConfigState> = repository.observeConfig()
}

class GetMessageLimitsUseCase(
    private val repository: AppConfigRepository
) {
    operator fun invoke(isPremium: Boolean): Int {
        val limits = repository.getConfig().limits
        return if (isPremium) limits.messageLengthLimitPremium else limits.messageLengthLimitDefault
    }
}

class GetStarsPricingConfigUseCase(
    private val repository: AppConfigRepository
) {
    operator fun invoke(): StarsConfigModel = repository.getConfig().stars
}

class GetTonPricingConfigUseCase(
    private val repository: AppConfigRepository
) {
    operator fun invoke(): TonConfigModel = repository.getConfig().ton
}

class GetRichMessageLimitsUseCase(
    private val repository: AppConfigRepository
) {
    operator fun invoke(): RichMessageConfigModel = repository.getConfig().richMessage
}

class GetPollsConfigUseCase(
    private val repository: AppConfigRepository
) {
    operator fun invoke(): PollsConfigModel = repository.getConfig().polls
}

class GetAiComposeConfigUseCase(
    private val repository: AppConfigRepository
) {
    operator fun invoke(): AiComposeConfigModel = repository.getConfig().aiCompose
}

class GetAppLimitsUseCase(
    private val repository: AppConfigRepository
) {
    operator fun invoke(): AppLimitsConfigModel = repository.getConfig().limits
}

class ReloadAppConfigUseCase(
    private val repository: AppConfigRepository
) {
    suspend operator fun invoke(): Result<AppGlobalConfigState> = repository.reloadConfig()
}

class UpdateAppConfigValueUseCase(
    private val repository: AppConfigRepository
) {
    suspend operator fun invoke(key: String, value: Any): Result<Unit> = repository.updateConfigValue(key, value)
}
