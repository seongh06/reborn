package com.reborn.server.domain.analytics.service

import com.reborn.server.domain.analytics.ApiRequestLog
import com.reborn.server.domain.analytics.repository.ApiRequestLogRepository
import com.reborn.server.global.async.AsyncConfig
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// LoggingInterceptor가 매 요청마다 호출 - 요청 처리 자체를 막지 않도록 비동기로 기록한다(#222).
@Service
class ApiRequestLogService(
    private val apiRequestLogRepository: ApiRequestLogRepository,
) {

    @Async(AsyncConfig.ASYNC_EXECUTOR)
    @Transactional
    fun record(method: String, path: String, status: Int, userId: Long?, elapsedMs: Long) {
        apiRequestLogRepository.save(
            ApiRequestLog(method = method, path = path, status = status, userId = userId, elapsedMs = elapsedMs),
        )
    }
}
