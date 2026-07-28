package com.reborn.server.domain.googlesheets.controller

import com.reborn.server.domain.googlesheets.dto.GoogleSheetsDto
import com.reborn.server.domain.googlesheets.service.GoogleSheetsService
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.ApiResponse
import com.reborn.server.global.model.CommonErrorCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Google Sheets 연동 API", description = "장소별 Google Sheets OAuth 연동 (데이터 화면 내보내기용)")
@RestController
@RequestMapping("/api/google-sheets/oauth")
class GoogleSheetsController(
    private val googleSheetsService: GoogleSheetsService,
) {

    @Operation(
        summary = "Google Sheets 계정 연동 시작",
        description = "Google 동의 화면으로 이동할 authorizeUrl을 발급합니다. 해당 장소의 ADMIN 권한이 필요합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "발급 성공 — authorizeUrl 반환"),
        SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
        SwaggerApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
        SwaggerApiResponse(responseCode = "404", description = "존재하지 않는 장소"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/authorize")
    fun authorize(
        @RequestParam placeId: Long,
        authentication: Authentication,
    ): ApiResponse<GoogleSheetsDto.AuthorizeResponse> =
        ApiResponse.success(googleSheetsService.startAuthorize(extractUserId(authentication), placeId))

    @Operation(
        summary = "Google Sheets OAuth 콜백",
        description = "Google이 사용자 동의 후 브라우저를 리다이렉트하는 엔드포인트입니다. 인증 불필요(Google이 직접 호출).",
    )
    @GetMapping("/callback")
    fun callback(
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) state: String?,
    ): ResponseEntity<String> {
        val placeName = runCatching { googleSheetsService.handleCallback(code, state) }
            .getOrElse { e ->
                val message = (e as? BusinessAlertException)?.message ?: "연동 중 오류가 발생했습니다."
                return htmlResponse(HttpStatus.BAD_REQUEST, "Google Sheets 연동 실패", message)
            }

        return htmlResponse(HttpStatus.OK, "Google Sheets 연동 완료", "\"$placeName\" 장소에 Google Sheets 계정이 연동되었습니다. 앱으로 돌아가주세요.")
    }

    private fun htmlResponse(status: HttpStatus, title: String, message: String): ResponseEntity<String> {
        val html = """
            <!DOCTYPE html>
            <html lang="ko"><head><meta charset="UTF-8"><title>$title</title></head>
            <body style="font-family:sans-serif;text-align:center;padding-top:80px;">
                <h2>$title</h2>
                <p>$message</p>
            </body></html>
        """.trimIndent()
        return ResponseEntity.status(status)
            .headers(HttpHeaders().apply { contentType = MediaType.TEXT_HTML })
            .body(html)
    }

    private fun extractUserId(authentication: Authentication): Long =
        authentication.principal as? Long
            ?: throw BusinessAlertException(CommonErrorCode.UNAUTHORIZED, "인증 정보가 유효하지 않습니다.")
}
