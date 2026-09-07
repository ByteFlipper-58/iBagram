package org.telegram.messenger.core.result

/**
 * Functional Result monad for explicit and safe error handling across architectural layers
 * without relying on uncaught exceptions.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Failure(val error: AppError) : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }

    fun getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Failure -> defaultValue
    }

    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }

    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Failure -> this
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onFailure(action: (AppError) -> Unit): Result<T> {
        if (this is Failure) action(error)
        return this
    }

    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun failure(error: AppError): Result<Nothing> = Failure(error)
        fun failure(message: String, cause: Throwable? = null): Result<Nothing> =
            Failure(AppError.Generic(message, cause))
    }
}

sealed class AppError(open val message: String, open val cause: Throwable? = null) {
    data class Network(override val message: String, val code: Int = -1, override val cause: Throwable? = null) : AppError(message, cause)
    data class Database(override val message: String, override val cause: Throwable? = null) : AppError(message, cause)
    data class NotFound(override val message: String) : AppError(message)
    data class InvalidInput(override val message: String) : AppError(message)
    data class Generic(override val message: String, override val cause: Throwable? = null) : AppError(message, cause)
}
