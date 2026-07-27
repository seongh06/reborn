package com.reborn.core.network.remote

import com.reborn.core.network.datasource.DeviceProvisioningDataSource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter

// 기기 SoftAP 프로비저닝 포털 전용 - 우리 서버(core:network의 인증된 클라이언트)와는 완전히 무관한
// 별도 호스트(192.168.4.1)라, App.kt의 Coil 이미지 로더와 같은 이유로 인증/베이스URL이 전혀 없는
// 순수 HttpClient를 직접 생성해서 쓴다(#143).
class DeviceProvisioningDataSourceImpl(
    private val httpClient: HttpClient = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000L
            connectTimeoutMillis = 5_000L
        }
    },
) : DeviceProvisioningDataSource {

    // 기기(reborn_dht22.ino/reborn_ai_speaker.ino)의 프로비저닝 폼과 동일한 계약: GET /save?ssid=&password=&device_id=
    // 성공하면 기기가 "저장 완료" 문구가 담긴 HTML을 응답한 뒤 스스로 재부팅한다 - 실패(필드 누락 등)면
    // 같은 입력 폼을 다시 응답하므로 응답 본문에 성공 문구가 있는지로 판별한다.
    override suspend fun configureWifi(ssid: String, password: String, deviceId: String): Result<Unit> = runCatching {
        val response = httpClient.get("http://192.168.4.1/save") {
            parameter("ssid", ssid)
            parameter("password", password)
            parameter("device_id", deviceId)
        }
        val body = response.body<String>()
        require("저장 완료" in body) { "기기가 설정을 저장하지 못했습니다. 입력값을 확인해주세요." }
    }
}
