package org.telegram.messenger.feature.system.ringtones.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.ringtones.domain.model.RingtoneErrorCode
import org.telegram.messenger.feature.system.ringtones.domain.model.RingtoneLimitsModel
import org.telegram.messenger.feature.system.ringtones.domain.model.RingtoneModel
import org.telegram.messenger.feature.system.ringtones.domain.model.RingtoneState
import org.telegram.messenger.feature.system.ringtones.domain.model.RingtoneValidationResult
import org.telegram.messenger.feature.system.ringtones.domain.repository.RingtoneRepository

class ValidateRingtoneEligibilityUseCase {
    operator fun invoke(
        durationSec: Int,
        sizeBytes: Long,
        mimeType: String?,
        fileExtension: String?,
        limits: RingtoneLimitsModel = RingtoneLimitsModel()
    ): RingtoneValidationResult {
        if (durationSec > limits.maxDurationSeconds) {
            return RingtoneValidationResult(
                isValid = false,
                errorCode = RingtoneErrorCode.TOO_LONG,
                message = "Ringtone duration $durationSec exceeds max ${limits.maxDurationSeconds}s"
            )
        }

        if (sizeBytes > limits.maxSizeBytes) {
            return RingtoneValidationResult(
                isValid = false,
                errorCode = RingtoneErrorCode.TOO_BIG,
                message = "Ringtone size $sizeBytes exceeds max ${limits.maxSizeBytes} bytes"
            )
        }

        val ext = fileExtension?.lowercase()?.trimStart('.') ?: ""
        val mime = mimeType?.lowercase() ?: ""

        val isExtSupported = ext.isNotEmpty() && limits.supportedExtensions.contains(ext)
        val isMimeSupported = mime.isNotEmpty() && limits.supportedMimeTypes.contains(mime)

        if (!isExtSupported && !isMimeSupported) {
            return RingtoneValidationResult(
                isValid = false,
                errorCode = RingtoneErrorCode.UNSUPPORTED_FORMAT,
                message = "Format $ext ($mime) is not supported for custom ringtones"
            )
        }

        return RingtoneValidationResult(isValid = true, errorCode = RingtoneErrorCode.NONE)
    }
}

class ObserveRingtonesUseCase(private val repository: RingtoneRepository) {
    operator fun invoke(): Flow<List<RingtoneModel>> = repository.observeRingtones()
}

class ObserveRingtoneStateUseCase(private val repository: RingtoneRepository) {
    operator fun invoke(): Flow<RingtoneState> = repository.observeState()
}

class GetRingtonesUseCase(private val repository: RingtoneRepository) {
    operator fun invoke(): List<RingtoneModel> = repository.getRingtones()
}

class GetRingtoneByIdUseCase(private val repository: RingtoneRepository) {
    operator fun invoke(id: Long): RingtoneModel? = repository.getRingtoneById(id)
}

class GetRingtoneSoundPathUseCase(private val repository: RingtoneRepository) {
    operator fun invoke(id: Long): String? = repository.getRingtoneSoundPath(id)
}

class AddRingtoneUseCase(
    private val repository: RingtoneRepository,
    private val validateEligibility: ValidateRingtoneEligibilityUseCase
) {
    operator fun invoke(ringtone: RingtoneModel): RingtoneValidationResult {
        val validation = validateEligibility(
            durationSec = ringtone.durationSec,
            sizeBytes = ringtone.sizeBytes,
            mimeType = ringtone.mimeType,
            fileExtension = ringtone.localUri?.substringAfterLast('.', "")
        )
        if (validation.isValid) {
            repository.addRingtone(ringtone)
        }
        return validation
    }
}

class RemoveRingtoneUseCase(private val repository: RingtoneRepository) {
    operator fun invoke(id: Long): Boolean = repository.removeRingtone(id)
}

class SaveRingtoneFromDocumentUseCase(
    private val repository: RingtoneRepository,
    private val validateEligibility: ValidateRingtoneEligibilityUseCase
) {
    operator fun invoke(
        documentId: Long,
        title: String,
        durationSec: Int,
        sizeBytes: Long,
        mimeType: String
    ): RingtoneValidationResult {
        val validation = validateEligibility(
            durationSec = durationSec,
            sizeBytes = sizeBytes,
            mimeType = mimeType,
            fileExtension = null
        )
        if (validation.isValid) {
            repository.saveRingtoneFromDocument(documentId, title, durationSec, sizeBytes, mimeType)
        }
        return validation
    }
}

class UploadRingtoneUseCase(
    private val repository: RingtoneRepository,
    private val validateEligibility: ValidateRingtoneEligibilityUseCase
) {
    operator fun invoke(
        filePath: String,
        fileName: String,
        durationSec: Int,
        sizeBytes: Long
    ): Result<RingtoneModel> {
        val ext = fileName.substringAfterLast('.', "")
        val validation = validateEligibility(
            durationSec = durationSec,
            sizeBytes = sizeBytes,
            mimeType = null,
            fileExtension = ext
        )
        if (!validation.isValid) {
            return Result.failure(IllegalArgumentException(validation.message ?: "Invalid ringtone"))
        }
        return repository.uploadRingtone(filePath, fileName, durationSec, sizeBytes)
    }
}

class CancelRingtoneUploadUseCase(private val repository: RingtoneRepository) {
    operator fun invoke(filePath: String) {
        repository.cancelUpload(filePath)
    }
}

class RefreshRingtonesUseCase(private val repository: RingtoneRepository) {
    operator fun invoke(force: Boolean = false) {
        repository.refreshRingtones(force)
    }
}

class SelectRingtoneUseCase(private val repository: RingtoneRepository) {
    operator fun invoke(id: Long?) {
        repository.selectRingtone(id)
    }
}
