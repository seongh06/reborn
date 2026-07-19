package com.reborn.server.domain.smartthings.controller

import com.reborn.server.domain.smartthings.dto.SmartThingsDto
import com.reborn.server.domain.smartthings.service.SmartThingsService
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

@Tag(name = "SmartThings 연동 API", description = "장소별 SmartThings OAuth 연동 (서버가 직접 SmartThings를 호출하는 구조, #130)")
@RestController
@RequestMapping("/api/smartthings/oauth")
class SmartThingsController(
    private val smartThingsService: SmartThingsService,
) {

    @Operation(
        summary = "SmartThings 계정 연동 시작",
        description = "SmartThings 동의 화면으로 이동할 authorizeUrl을 발급합니다. 관리자 앱이 이 URL을 " +
            "외부 브라우저/Custom Tab으로 열면 됩니다. 해당 장소의 ADMIN 권한이 필요합니다.",
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
    ): ApiResponse<SmartThingsDto.AuthorizeResponse> =
        ApiResponse.success(smartThingsService.startAuthorize(extractUserId(authentication), placeId))

    @Operation(
        summary = "SmartThings OAuth 콜백",
        description = "SmartThings가 사용자 동의 후 브라우저를 리다이렉트하는 엔드포인트입니다. " +
            "인가 코드를 토큰으로 교환해 장소에 저장하고, 결과를 보여주는 HTML을 반환합니다. 인증 불필요(SmartThings가 직접 호출).",
    )
    @GetMapping("/callback")
    fun callback(
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) state: String?,
    ): ResponseEntity<String> {
        val placeName = runCatching { smartThingsService.handleCallback(code, state) }
            .getOrElse { e ->
                val message = (e as? BusinessAlertException)?.message ?: "연동 중 오류가 발생했습니다."
                return htmlResponse(HttpStatus.BAD_REQUEST, "SmartThings 연동 실패", message)
            }

        return htmlResponse(HttpStatus.OK, "SmartThings 연동 완료", "\"$placeName\" 장소에 SmartThings 계정이 연동되었습니다. 앱으로 돌아가주세요.")
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
