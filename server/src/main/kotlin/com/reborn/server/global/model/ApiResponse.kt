package com.reborn.server.global.model

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T? = null,
) {
    companion object {
        fun <T> success(data: T? = null, message: String = "요청이 성공했습니다."): ApiResponse<T> =
            ApiResponse(code = 200, message = message, data = data)

        fun fail(errorCode: CommonErrorCode): ApiResponse<Nothing> =
            ApiResponse(code = errorCode.code, message = errorCode.message)
    }
}
