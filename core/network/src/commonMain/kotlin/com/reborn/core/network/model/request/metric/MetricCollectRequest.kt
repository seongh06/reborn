package com.reborn.core.network.model.request.metric

import kotlinx.serialization.Serializable

// 공기계 전용(#294) - 서버 MetricDto.CollectRequest와 필드명을 맞춤. 온습도(아두이노 몫)는 이
// 클라이언트에서 보낼 일이 없어 제외.
@Serializable
data class MetricCollectRequest(
    val illuminance: Int? = null,
    val peopleCount: Int? = null,
)
