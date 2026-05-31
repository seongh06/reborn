package com.reborn.server.global.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonPropertyOrder


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("success", "message", "data")
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
) {
    companion object {
        // 성공 응답 (데이터가 있는 경우)
        fun <T> success(data: T? = null, message: String = "API 호출 성공"): ApiResponse<T> =
            ApiResponse(
                success = true,
                message = message,
                data = data
            )

        // 성공 응답 (데이터 없이 메시지만 전달하는 경우)
        fun success(message: String): ApiResponse<Nothing> =
            ApiResponse(
                success = true,
                message = message,
                data = null
            )

        // 실패 응답 (에러 코드 객체를 사용하는 경우)
        fun fail(errorCode: CommonErrorCode): ApiResponse<Nothing> =
            ApiResponse(
                success = false,
                message = errorCode.message,
                data = null
            )

        // 실패 응답 (메시지를 직접 입력하는 경우 - Validation 등)
        fun fail(message: String): ApiResponse<Nothing> =
            ApiResponse(
                success = false,
                message = message,
                data = null
            )
    }
}