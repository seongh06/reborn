package com.reborn.server.global.handler

import com.reborn.server.global.model.CommonErrorCode
import com.reborn.server.global.model.ErrorResponse
import com.reborn.server.global.slack.SlackWebhookClient
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler(
    private val slackWebhookClient: SlackWebhookClient,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessAlertException::class)
    fun handleBusinessAlertException(e: BusinessAlertException): ResponseEntity<ErrorResponse> {
        log.warn("Business exception [{}]: {}", e.errorCode, e.message)
        return ResponseEntity
            .status(e.errorCode.code)
            .body(ErrorResponse.of(e.errorCode.code, e.message))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = e.bindingResult.fieldErrors.firstOrNull()?.defaultMessage
            ?: CommonErrorCode.INVALID_INPUT.message
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of(CommonErrorCode.INVALID_INPUT.code, message))
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotAllowedException(e: HttpRequestMethodNotSupportedException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(ErrorResponse.of(CommonErrorCode.METHOD_NOT_ALLOWED))

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception", e)
        val requestId = MDC.get("requestId") ?: "unknown"
        slackWebhookClient.send(
            "🚨 *[ReBorn] 서버 오류 발생*\n" +
                "• 타입: `${e::class.simpleName}`\n" +
                "• RequestId: `$requestId`\n" +
                "• 상세 내용은 서버 로그를 확인하세요",
        )
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.of(CommonErrorCode.INTERNAL_SERVER_ERROR))
    }
}
