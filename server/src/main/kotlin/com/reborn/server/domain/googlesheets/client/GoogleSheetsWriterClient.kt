package com.reborn.server.domain.googlesheets.client

import com.reborn.server.global.handler.BusinessAlertException
import com.reborn.server.global.model.CommonErrorCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.Duration

// Google Sheets API v4 호출 클라이언트(데이터 화면 내보내기). accessToken은 호출부(GoogleSheetsService)가
// 관리하는 유효한 토큰을 그대로 전달받는다 - 이 클라이언트는 리프레시를 모른다.
@Component
class GoogleSheetsWriterClient(
    restTemplateBuilder: RestTemplateBuilder,
    @param:Value("\${google-sheets.api-base-url:https://sheets.googleapis.com/v4/spreadsheets}") private val apiBaseUrl: String,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate: RestTemplate = restTemplateBuilder
        .connectTimeout(Duration.ofSeconds(10))
        .readTimeout(Duration.ofSeconds(30))
        .build()

    fun createSpreadsheet(accessToken: String, title: String): String {
        val body = mapOf("properties" to mapOf("title" to title))

        val response = runCatching {
            restTemplate.exchange(apiBaseUrl, HttpMethod.POST, HttpEntity(body, jsonHeaders(accessToken)), Map::class.java).body
        }.onFailure { e -> log.warn("Google Sheets 생성 실패: {}", e.message) }
            .getOrNull()
            ?: throw BusinessAlertException(CommonErrorCode.INTERNAL_SERVER_ERROR, "Google Sheets 생성에 실패했습니다.")

        return response["spreadsheetId"] as? String
            ?: throw BusinessAlertException(CommonErrorCode.INTERNAL_SERVER_ERROR, "Google Sheets 생성 응답이 올바르지 않습니다.")
    }

    fun writeValues(accessToken: String, spreadsheetId: String, range: String, values: List<List<Any?>>) {
        val body = mapOf("values" to values)
        val url = "$apiBaseUrl/$spreadsheetId/values/$range?valueInputOption=RAW"

        runCatching {
            restTemplate.exchange(url, HttpMethod.PUT, HttpEntity(body, jsonHeaders(accessToken)), Map::class.java)
        }.onFailure { e -> log.warn("Google Sheets 값 쓰기 실패: {}", e.message) }
            .getOrElse { throw BusinessAlertException(CommonErrorCode.INTERNAL_SERVER_ERROR, "Google Sheets에 값을 쓰는 데 실패했습니다.") }
    }

    private fun jsonHeaders(accessToken: String): HttpHeaders = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
        setBearerAuth(accessToken)
    }
}
