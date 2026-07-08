package com.reborn.core.data.mapper

import com.reborn.core.model.DomainException
import com.reborn.core.network.model.ApiResponse


inline fun <T, R> ApiResponse<T>.toResult(
    @Suppress("UNCHECKED_CAST") transform: (T) -> R = { it as R },
): Result<R> = when (this) {
    is ApiResponse.Success -> {
        runCatching { transform(data) }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(DomainException.UnknownException(cause = it)) }
        )
    }

    is ApiResponse.Failure.HttpError -> {
        Result.failure(mapErrorCodeToDomainException(code.toLong(), if (message != null) Exception(message) else null))
    }

    is ApiResponse.Failure.NetworkError -> {
        val cause = if (throwable != null) Exception(throwable) else null
        Result.failure(DomainException.NetworkException(cause = cause))
    }

    is ApiResponse.Failure.UnknownApiError -> {
        val cause = if (throwable != null) Exception(throwable) else null
        Result.failure(DomainException.UnknownException(cause = cause))
    }
}

fun <T> ApiResponse<T>.toResult(): Result<T> = toResult { it }

fun mapErrorCodeToDomainException(errorCode: Long, cause: Throwable? = null): DomainException {
    return when (errorCode) {
        400L -> DomainException.InvalidInputException(cause = cause)
        401L -> DomainException.InvalidJwtTokenException(cause = cause)
        404L -> DomainException.UserNotFoundException(cause = cause)
        500L -> DomainException.ServerErrorException(cause = cause)

        1000L -> DomainException.InvalidOAuthTokenException(cause = cause)
        1001L -> DomainException.UnsupportedSocialProviderException(cause = cause)
        1002L -> DomainException.KakaoFailedException(cause = cause)

        2000L -> DomainException.AlreadyRegisteredUserException(cause = cause)
        2001L -> DomainException.InvalidJwtTokenException(cause = cause)
        3000L -> DomainException.UserNotFoundException(cause = cause)

        9100L -> DomainException.ImageUploadFailedException(cause = cause)
        9101L -> DomainException.ImageDeleteFailedException(cause = cause)
        9102L -> DomainException.UnsupportedFileTypeException(cause = cause)
        9103L -> DomainException.UnsupportedFileExtensionException(cause = cause)

        9000L -> DomainException.ServerErrorException(cause = cause)
        9001L -> DomainException.InvalidInputException(cause = cause)

        else -> DomainException.UnknownException(cause = cause)
    }
}