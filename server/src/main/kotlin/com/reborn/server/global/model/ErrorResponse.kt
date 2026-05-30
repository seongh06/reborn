package com.reborn.server.global.model

data class ErrorResponse(
    val code: Int,
    val message: String,
) {
    companion object {
        fun of(errorCode: CommonErrorCode): ErrorResponse =
            ErrorResponse(code = errorCode.code, message = errorCode.message)

        fun of(code: Int, message: String): ErrorResponse =
            ErrorResponse(code = code, message = message)
    }
}
