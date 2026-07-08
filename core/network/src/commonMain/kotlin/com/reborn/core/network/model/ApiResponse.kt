package com.reborn.core.network.model

import kotlinx.serialization.Serializable


@Serializable
sealed interface ApiResponse<out T> {

    @Serializable
    data class Success<T>(val data: T) : ApiResponse<T>

    @Serializable
    sealed interface Failure : ApiResponse<Nothing> {
        @Serializable
        data class HttpError(val code: Int, val message: String, val businessCode: String? = null) : Failure
        @Serializable
        data class NetworkError(val message: String, val throwable: String? = null) : Failure
        @Serializable
        data class UnknownApiError(val message: String, val throwable: String? = null) : Failure
    }
}