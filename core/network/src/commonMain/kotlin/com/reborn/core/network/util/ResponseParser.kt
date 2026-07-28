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
            // exception.message는 OkHttp/Ktor 원시 예외 메시지(영어, 기술 용어)라 사용자에게 그대로
            // 보여주면 안 됨 - 화면엔 항상 친화적인 한국어 문구만, 상세 원인은 throwable(현재는
            // 로깅 용도로만 보관, UI에서 소비하지 않음)에 남긴다.
            else ->
                ApiResponse.Failure.NetworkError(
                    message = "네트워크 연결에 실패했습니다. 인터넷 연결을 확인해주세요.",
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
        // e.message는 응답 파싱 실패 시 kotlinx.serialization 등의 원시 예외 메시지라 사용자에게
        // 그대로 보여주면 안 됨(위 NetworkError와 동일한 이유) - 항상 친화적인 한국어 문구만 노출.
        ApiResponse.Failure.UnknownApiError(
            message = "알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
            throwable = e.stackTraceToString()
        )
    }
}