package org.telegram.messenger.feature.messaging.autodelete.domain.model

data class GlobalAutoDeleteStateModel(
    val ttl: AutoDeleteTtlModel = AutoDeleteTtlModel.OFF,
    val isLoading: Boolean = false
)
