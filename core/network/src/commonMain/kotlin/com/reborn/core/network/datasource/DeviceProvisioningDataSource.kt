package com.reborn.core.network.datasource

interface DeviceProvisioningDataSource {
    // 기기의 SoftAP 프로비저닝 포털(http://192.168.4.1)에 WiFi 자격증명을 전달한다(#143).
    // 우리 서버가 아니라 로컬 AP 안에서만 유효한 기기 자체 웹서버라 인증 없음 - 성공하면 기기가
    // 저장 완료 페이지를 응답한 뒤 곧바로 재부팅한다.
    suspend fun configureWifi(ssid: String, password: String, deviceId: String): Result<Unit>
}
