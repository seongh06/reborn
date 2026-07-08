package com.reborn.server.domain.metric

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface MetricLogRepository : JpaRepository<MetricLog, Long> {

    fun findTopByDeviceIdOrderByCreatedAtDesc(deviceId: Long): MetricLog?

    fun findAllByDeviceId(deviceId: Long, pageable: Pageable): Page<MetricLog>

    @Query(
        "SELECT m FROM MetricLog m " +
            "WHERE m.device.id = :deviceId AND m.createdAt BETWEEN :from AND :to " +
            "ORDER BY m.createdAt DESC",
    )
    fun findAllByDeviceIdAndPeriod(
        @Param("deviceId") deviceId: Long,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): List<MetricLog>

    // 기기 삭제(ON DELETE SET NULL) 후 device_id = NULL 상태인 고아 로그 조회
    fun findAllByDeviceIsNull(pageable: Pageable): Page<MetricLog>
}
