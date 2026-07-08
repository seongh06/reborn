package com.reborn.core.network.util

import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.BaseResponse
import com.reborn.core.network.model.ErrorResponse
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

internal suspend inline fun <reified T> Result<HttpResponse>.asApiResponse(): ApiResponse<T> {
    val response = this.getOrNull()
    val exception = this.exceptionOrNull()

    if (exception != null) {
        return when (exception) {
            is ConnectTimeoutException ->
                ApiResponse.Failure.NetworkError(
                    message = "서버 연결 시간이 초과되었습니다.",
                    throwable = "TIMEOUT"
                )
            else ->
                ApiResponse.Failure.NetworkError(
                    message = exception.message ?: "네트워크 연결 실패",
                    throwable = exception.toString()
                )
        }
    }

    return try {
        if (response == null) throw Exception("Response is null")

        if (!response.status.isSuccess()) {
            val errorResponse: ErrorResponse = response.body()
            return ApiResponse.Failure.HttpError(
                code = response.status.value,
                message = errorResponse.message
            )
        }

        val baseResponse: BaseResponse<T> = response.body()
        if (baseResponse.isSuccess) {
            ApiResponse.Success(baseResponse.data as T)
        }else {
            ApiResponse.Failure.HttpError(
                code = response.status.value,
                message = baseResponse.message
            )
        }
    } catch (e: Exception) {
        ApiResponse.Failure.UnknownApiError(
            message = e.message ?: "Unknown Error",
            throwable = e.stackTraceToString()
        )
    }
}