package com.reborn.server.global.token

import com.fasterxml.jackson.databind.ObjectMapper
import com.reborn.server.global.model.CommonErrorCode
import com.reborn.server.global.model.ErrorResponse
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import java.nio.charset.StandardCharsets

internal fun HttpServletResponse.writeErrorJson(objectMapper: ObjectMapper, errorCode: CommonErrorCode) {
    status = errorCode.code
    contentType = MediaType.APPLICATION_JSON_VALUE
    characterEncoding = StandardCharsets.UTF_8.name()
    writer.write(objectMapper.writeValueAsString(ErrorResponse.of(errorCode)))
}
