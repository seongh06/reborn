package com.reborn.server.domain.analytics.repository

import com.reborn.server.domain.analytics.AutoControlExecutionLog
import org.springframework.data.jpa.repository.JpaRepository

interface AutoControlExecutionLogRepository : JpaRepository<AutoControlExecutionLog, Long>
