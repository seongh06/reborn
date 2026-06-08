package com.reborn.server.domain.data

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface SensorLogsRepository : JpaRepository<SensorLogs, Long> {

    fun findTopByDeviceIdOrderByCreatedAtDesc(deviceId: Long): SensorLogs?

    fun findAllByDeviceId(deviceId: Long, pageable: Pageable): Page<SensorLogs>

    @Query(
        "SELECT s FROM SensorLogs s " +
            "WHERE s.device.id = :deviceId AND s.createdAt BETWEEN :from AND :to " +
            "ORDER BY s.createdAt DESC",
    )
    fun findAllByDeviceIdAndPeriod(
        @Param("deviceId") deviceId: Long,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): List<SensorLogs>

    // 기기 삭제(ON DELETE SET NULL) 후 device_id = NULL 상태인 고아 로그 조회
    fun findAllByDeviceIsNull(pageable: Pageable): Page<SensorLogs>
}
