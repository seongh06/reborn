package com.reborn.server.domain.analytics.repository

import com.reborn.server.domain.analytics.ApiRequestLog
import org.springframework.data.jpa.repository.JpaRepository

interface ApiRequestLogRepository : JpaRepository<ApiRequestLog, Long>
