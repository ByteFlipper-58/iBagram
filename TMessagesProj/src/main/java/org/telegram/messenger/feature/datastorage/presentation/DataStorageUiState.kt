package org.telegram.messenger.feature.datastorage.presentation

import org.telegram.messenger.feature.datastorage.domain.model.AutoDownloadPresetModel
import org.telegram.messenger.feature.datastorage.domain.model.KeepMediaSettingsModel
import org.telegram.messenger.feature.datastorage.domain.model.NetworkUsageModel
import org.telegram.messenger.feature.datastorage.domain.model.StorageUsageModel

data class DataStorageUiState(
    val storageUsage: StorageUsageModel = StorageUsageModel(),
    val mobileUsage: NetworkUsageModel? = null,
    val wifiUsage: NetworkUsageModel? = null,
    val roamingUsage: NetworkUsageModel? = null,
    val mobilePreset: AutoDownloadPresetModel? = null,
    val wifiPreset: AutoDownloadPresetModel? = null,
    val roamingPreset: AutoDownloadPresetModel? = null,
    val keepMediaSettings: KeepMediaSettingsModel? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
