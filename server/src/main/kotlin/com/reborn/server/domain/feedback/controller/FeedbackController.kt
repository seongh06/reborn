package com.reborn.server.domain.feedback.controller

import com.reborn.server.domain.feedback.dto.FeedbackDto
import com.reborn.server.domain.feedback.service.FeedbackService
import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.ApiResponse
import com.reborn.server.global.model.CommonErrorCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "피드백 API", description = "QR 피드백 제출/조회/상태변경")
@RestController
@RequestMapping("/api/feedback")
class FeedbackController(
    private val feedbackService: FeedbackService,
) {

    @Operation(
        summary = "피드백 보내기",
        description = "QR 웹페이지에서 방문자가 피드백을 제출합니다. 인증이 필요 없으며, sessionToken으로 중복 제출을 방지합니다.",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "제출 성공 — feedbackId, status(PENDING), createdAt 반환"),
        SwaggerApiResponse(responseCode = "400", description = "필수 필드 누락 또는 잘못된 qrCode"),
        SwaggerApiResponse(responseCode = "404", description = "존재하지 않는 장소 또는 기기"),
        SwaggerApiResponse(responseCode = "429", description = "동일 세션 중복 제출"),
    )
    @PostMapping
    fun submit(
        @Valid @RequestBody request: FeedbackDto.SubmitRequest,
        httpRequest: HttpServletRequest,
    ): ApiResponse<FeedbackDto.SubmitResponse> =
        ApiResponse.success(feedbackService.submit(request, httpRequest.getHeader("User-Agent")))

    @Operation(
        summary = "피드백 조회",
        description = "특정 장소에 접수된 피드백 목록을 조회합니다. deviceId/status로 필터링할 수 있습니다. (ADMIN 권한 필요)",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "조회 성공 — totalCount, feedbacks 반환"),
        SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
        SwaggerApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
        SwaggerApiResponse(responseCode = "404", description = "존재하지 않는 장소 또는 기기"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    fun getList(
        @RequestParam placeId: Long,
        @RequestParam(required = false) deviceId: String?,
        @RequestParam(required = false) status: String?,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable,
        authentication: Authentication,
    ): ApiResponse<FeedbackDto.ListResponse> =
        ApiResponse.success(
            feedbackService.getList(extractUserId(authentication), placeId, deviceId, status, pageable),
        )

    @Operation(
        summary = "피드백 개수 조회",
        description = "특정 장소의 피드백 개수를 상태별로 집계합니다. (ADMIN 권한 필요)",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "조회 성공 — total, pending, approved, rejected 반환"),
        SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
        SwaggerApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
        SwaggerApiResponse(responseCode = "404", description = "존재하지 않는 장소"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/count")
    fun getCount(
        @RequestParam placeId: Long,
        authentication: Authentication,
    ): ApiResponse<FeedbackDto.CountResponse> =
        ApiResponse.success(feedbackService.getCount(extractUserId(authentication), placeId))

    @Operation(
        summary = "피드백 상태 변경",
        description = "접수된 피드백을 승인(APPROVED) 또는 거절(REJECTED)로 처리합니다. (ADMIN 권한 필요)",
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "상태 변경 성공 — feedbackId, status 반환"),
        SwaggerApiResponse(responseCode = "400", description = "잘못된 status 값 또는 이미 처리된 피드백"),
        SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
        SwaggerApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
        SwaggerApiResponse(responseCode = "404", description = "존재하지 않는 피드백"),
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{feedbackId}")
    fun updateStatus(
        @PathVariable feedbackId: Long,
        @Valid @RequestBody request: FeedbackDto.StatusUpdateRequest,
        authentication: Authentication,
    ): ApiResponse<FeedbackDto.StatusUpdateResponse> =
        ApiResponse.success(feedbackService.updateStatus(extractUserId(authentication), feedbackId, request))

    private fun extractUserId(authentication: Authentication): Long =
        authentication.principal as? Long
            ?: throw BusinessAlertException(CommonErrorCode.UNAUTHORIZED, "인증 정보가 유효하지 않습니다.")
}
