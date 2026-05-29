package com.reborn.server.global.model

enum class CommonErrorCode(val code: Int, val message: String) {
    // 공통
    INVALID_INPUT(400, "잘못된 요청입니다."),
    UNAUTHORIZED(401, "인증이 필요합니다."),
    FORBIDDEN(403, "접근 권한이 없습니다."),
    NOT_FOUND(404, "리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(405, "허용되지 않는 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다."),
}
